package com.vahak.parentcontroll.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.vahak.parentcontroll.core.data.local.dao.AppRuleDao
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.dao.UsageDao
import com.vahak.parentcontroll.core.data.local.entity.AppUsageRecordEntity
import com.vahak.parentcontroll.core.data.local.entity.DailyUsageEntity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@AndroidEntryPoint
class RestrictionEnforcerService : LifecycleService() {

    @Inject
    lateinit var settingsDao: SettingsDao

    @Inject
    lateinit var usageDao: UsageDao

    @Inject
    lateinit var appRuleDao: AppRuleDao

    private lateinit var timeLockOverlay: RestrictionOverlay
    private lateinit var appLockOverlay: RestrictionOverlay
    private lateinit var bedtimeLockOverlay: RestrictionOverlay

    private var monitoringJob: Job? = null
    private var currentChildId: String? = null
    private var lastKnownPackage: String = ""

    // --- HOISTED TRACKING VARIABLES ---
    private var currentDateTracker: LocalDate = LocalDate.now()
    private var usedSecondsTodayTracker: Int = 0
    private val appUsageMapTracker = mutableMapOf<String, Int>()

    companion object {
        private const val TAG = "EnforcerService"
        private const val CHANNEL_ID = "RestrictionEnforcerChannel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_CHILD_ID = "EXTRA_CHILD_ID"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "⚙️ Service Created. Initializing Overlays...")

        timeLockOverlay = RestrictionOverlay(this, this, OverlayType.TIME_LIMIT)
        appLockOverlay = RestrictionOverlay(this, this, OverlayType.APP_BLOCK)
        bedtimeLockOverlay = RestrictionOverlay(this, this, OverlayType.BEDTIME)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "🚀 onStartCommand triggered with Action = ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                val childId = intent.getStringExtra(EXTRA_CHILD_ID)
                if (childId != null) {
                    currentChildId = childId
                    startForeground(NOTIFICATION_ID, createNotification())
                    startMonitoring(childId)
                } else {
                    Log.e(TAG, "❌ Start failed: Child ID is null. Shutting down.")
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                Log.w(TAG, "🛑 Action Stop received. Halting monitoring.")
                stopMonitoring()
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun getForegroundPackage(): String {
        val usageStatsManager = getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 1000 * 60, time)
        val event = android.app.usage.UsageEvents.Event()

        var newlyFoundPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED) {
                newlyFoundPackage = event.packageName
            }
        }
        if (newlyFoundPackage != null) lastKnownPackage = newlyFoundPackage
        if (lastKnownPackage.isEmpty()) lastKnownPackage = packageName

        return lastKnownPackage
    }

    private fun isBedtimeActiveNow(start: LocalTime, end: LocalTime): Boolean {
        val now = LocalTime.now()
        return if (start.isBefore(end)) {
            !now.isBefore(start) && now.isBefore(end)
        } else {
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    private fun startMonitoring(childId: String) {
        monitoringJob?.cancel()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        monitoringJob = lifecycleScope.launch {
            Log.i(TAG, "👀 startMonitoring() initiated for Child ID: $childId")

            currentDateTracker = LocalDate.now()
            usedSecondsTodayTracker =
                usageDao.getDailyUsage(childId, currentDateTracker)?.usedSeconds ?: 0

            appUsageMapTracker.clear()
            val existingAppUsages =
                usageDao.observeAppUsageForDay(childId, currentDateTracker).first()
            existingAppUsages.forEach { appUsageMapTracker[it.packageName] = it.usedSeconds }

            Log.d(
                TAG,
                "🔄 Local State Restored. Starting at: $usedSecondsTodayTracker seconds for today."
            )

            var loopCounter = 0

            combine(
                settingsDao.getGlobalSettings(childId), appRuleDao.observeAllowedApps(childId)
            ) { settings, allowedApps ->
                Pair(settings, allowedApps.map { it.packageName }.toSet())
            }.collectLatest { (settings, allowedPackages) ->

                if (settings == null) {
                    Log.w(TAG, "⚠️ Settings are null. Waiting for DB initialization...")
                    return@collectLatest
                }

                val isTimeLimitEnabled = settings.isTimeLimitActive
                val limitInSeconds = settings.dailyTimeLimitMins * 60
                val isBedtimeEnabled = settings.isBedtimeActive
                val bedtimeStart = settings.bedtimeStart
                val bedtimeEnd = settings.bedtimeEnd

                Log.i(
                    TAG,
                    "📊 Settings Loaded | TimeLimit Active: $isTimeLimitEnabled ($limitInSeconds sec) | Bedtime Active: $isBedtimeEnabled ($bedtimeStart - $bedtimeEnd) | Allowed Apps Count: ${allowedPackages.size}"
                )

                val homeIntent =
                    Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val launcherPackages = packageManager.queryIntentActivities(homeIntent, 0)
                    .map { it.activityInfo.packageName }.toSet()

                val criticalSystemPackages =
                    setOf("com.android.systemui", "android") + launcherPackages
                Log.d(TAG, "🛡️ System Whitelist active for: $criticalSystemPackages")

                while (isActive) {
                    val now = LocalDate.now()

                    // Midnight Rollover
                    if (now != currentDateTracker) {
                        Log.i(TAG, "🌙 Midnight Rollover detected! Resetting daily trackers.")
                        saveDataToRoom(
                            childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker
                        )
                        currentDateTracker = now
                        usedSecondsTodayTracker = 0
                        appUsageMapTracker.clear()
                    }

                    val isScreenOn = powerManager.isInteractive
                    val currentApp = getForegroundPackage()
                    val isOurLauncher = currentApp == packageName
                    val isBedtimeNow =
                        isBedtimeEnabled && isBedtimeActiveNow(bedtimeStart, bedtimeEnd)

                    // --- THE HEARTBEAT LOG ---
                    Log.d(
                        TAG,
                        "⏱️ TICK | Screen: ${if (isScreenOn) "ON" else "OFF"} | App: $currentApp | Time: $usedSecondsTodayTracker/$limitInSeconds sec | BedtimeNow: $isBedtimeNow"
                    )

                    if (currentApp == "com.android.settings") {
                        Log.w(
                            TAG, "🚨 BLOCKING PRIORITY 0: Child attempted to open Android Settings."
                        )
                        hideAllOverlaysExcept(appLockOverlay)
                        appLockOverlay.show()
                    } else if (isScreenOn && currentApp.isNotEmpty() && !isOurLauncher) {

                        val isCriticalSystem = criticalSystemPackages.contains(currentApp)
                        val isAppAllowed = allowedPackages.contains(currentApp)

                        if (isBedtimeNow && !isCriticalSystem) {
                            Log.w(
                                TAG,
                                "💤 BLOCKING PRIORITY 1: Bedtime is active. Blocking $currentApp"
                            )
                            hideAllOverlaysExcept(bedtimeLockOverlay)
                            bedtimeLockOverlay.show()
                        } else if (!isAppAllowed && !isCriticalSystem) {
                            Log.w(
                                TAG,
                                "🚫 BLOCKING PRIORITY 2: App is not on the allowed list -> $currentApp"
                            )
                            hideAllOverlaysExcept(appLockOverlay)
                            appLockOverlay.show()
                        } else {
                            if (!isCriticalSystem) {
                                usedSecondsTodayTracker += 1
                                appUsageMapTracker[currentApp] =
                                    (appUsageMapTracker[currentApp] ?: 0) + 1
                                Log.v(TAG, "✅ App Allowed: $currentApp | Tracking incremented.")
                            }

                            if (isTimeLimitEnabled && usedSecondsTodayTracker >= limitInSeconds) {
                                Log.w(
                                    TAG,
                                    "⏳ BLOCKING PRIORITY 3: Daily Time Limit reached ($usedSecondsTodayTracker sec). Blocking $currentApp"
                                )
                                hideAllOverlaysExcept(timeLockOverlay)
                                timeLockOverlay.show()
                            } else {
                                hideAllOverlays()
                            }
                        }
                    } else {
                        // Safe Zone
                        hideAllOverlays()
                    }

                    // DB Save every 60 seconds
                    loopCounter++
                    if (loopCounter >= 60) {
                        Log.d(TAG, "⏲️ 60-second mark reached. Triggering DB save.")
                        saveDataToRoom(
                            childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker
                        )
                        loopCounter = 0
                    }
                    delay(1000L)
                }
            }
        }
    }

    private fun hideAllOverlays() {
        if (timeLockOverlay.isShowing() || appLockOverlay.isShowing() || bedtimeLockOverlay.isShowing()) {
            Log.v(TAG, "🧹 Hiding all overlays.")
        }
        timeLockOverlay.hide()
        appLockOverlay.hide()
        bedtimeLockOverlay.hide()
    }

    private fun hideAllOverlaysExcept(activeOverlay: RestrictionOverlay) {
        if (activeOverlay != timeLockOverlay) timeLockOverlay.hide()
        if (activeOverlay != appLockOverlay) appLockOverlay.hide()
        if (activeOverlay != bedtimeLockOverlay) bedtimeLockOverlay.hide()
    }

    private suspend fun saveDataToRoom(
        childId: String, date: LocalDate, totalSeconds: Int, appMap: Map<String, Int>
    ) {
        try {
            Log.i(TAG, "💾 Saving to Room DB... Total: $totalSeconds sec | Apps: ${appMap.size}")
            usageDao.insertOrUpdateDailyUsage(DailyUsageEntity(childId, date, totalSeconds))
            val appRecords = appMap.map { (pkg, seconds) ->
                AppUsageRecordEntity(childId, date, pkg, seconds)
            }
            if (appRecords.isNotEmpty()) usageDao.insertOrUpdateAppUsages(appRecords)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Database update failed: ${e.message}")
        }
    }

    private fun performFinalSave() {
        val childId = currentChildId ?: return
        if (usedSecondsTodayTracker == 0 && appUsageMapTracker.isEmpty()) return
        Log.i(TAG, "💾 [FINAL SAVE] Service shutting down. Saving final state...")
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                saveDataToRoom(
                    childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker
                )
            }
        }
    }

    private fun stopMonitoring() {
        Log.i(TAG, "🛑 stopMonitoring() called. Canceling job and hiding overlays.")
        monitoringJob?.cancel()
        hideAllOverlays()
    }

    override fun onDestroy() {
        Log.d(TAG, "💥 Service Destroyed")
        performFinalSave()
        stopMonitoring()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("محافظت خانواده فعال است")
            .setContentText("گوشی در حالت امن کودک قرار دارد.")
            .setSmallIcon(android.R.drawable.ic_secure).setOngoing(true).build()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, "نظارت خانواده", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}