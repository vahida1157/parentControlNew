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

    // We keep 3 instances of the overlay so we can manage their states cleanly
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
        Log.d(TAG, "Service Created")

        // Initialize Overlays (You can pass custom strings here later if you update TimeLockOverlay)
        timeLockOverlay = RestrictionOverlay(this, this, OverlayType.TIME_LIMIT)
        appLockOverlay = RestrictionOverlay(this, this, OverlayType.APP_BLOCK)
        bedtimeLockOverlay = RestrictionOverlay(this, this, OverlayType.BEDTIME)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val childId = intent.getStringExtra(EXTRA_CHILD_ID)
                if (childId != null) {
                    currentChildId = childId
                    startForeground(NOTIFICATION_ID, createNotification())
                    startMonitoring(childId)
                } else {
                    stopSelf()
                }
            }

            ACTION_STOP -> {
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

    // --- NEW: Time Math for Bedtimes crossing Midnight ---
    private fun isBedtimeActiveNow(start: LocalTime, end: LocalTime): Boolean {
        val now = LocalTime.now()
        return if (start.isBefore(end)) {
            // E.g., Nap time: 13:00 to 15:00
            !now.isBefore(start) && now.isBefore(end)
        } else {
            // E.g., Night sleep: 22:00 to 07:00 (Crosses midnight)
            !now.isBefore(start) || now.isBefore(end)
        }
    }

    private fun startMonitoring(childId: String) {
        monitoringJob?.cancel()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        monitoringJob = lifecycleScope.launch {
            Log.i(TAG, "🚀 Monitoring Started for Child: $childId")

            currentDateTracker = LocalDate.now()
            usedSecondsTodayTracker =
                usageDao.getDailyUsage(childId, currentDateTracker)?.usedSeconds ?: 0

            appUsageMapTracker.clear()
            val existingAppUsages =
                usageDao.observeAppUsageForDay(childId, currentDateTracker).first()
            existingAppUsages.forEach { appUsageMapTracker[it.packageName] = it.usedSeconds }

            var loopCounter = 0

            combine(
                settingsDao.getGlobalSettings(childId), appRuleDao.observeAllowedApps(childId)
            ) { settings, allowedApps ->
                Pair(settings, allowedApps.map { it.packageName }.toSet())
            }.collectLatest { (settings, allowedPackages) ->

                if (settings == null) return@collectLatest // Await settings

                // PRO FIX: We do NOT shut down the service if limits are off.
                // We must keep monitoring to generate the Usage Reports!

                val isTimeLimitEnabled = settings.isTimeLimitActive
                val limitInSeconds = settings.dailyTimeLimitMins * 60

                val isBedtimeEnabled = settings.isBedtimeActive
                val bedtimeStart = settings.bedtimeStart
                val bedtimeEnd = settings.bedtimeEnd

                val homeIntent =
                    Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val launcherPackages = packageManager.queryIntentActivities(homeIntent, 0)
                    .map { it.activityInfo.packageName }.toSet()

                val criticalSystemPackages =
                    setOf("com.android.systemui", "android") + launcherPackages

                while (isActive) {
                    val now = LocalDate.now()

                    // Midnight Rollover Tracker Reset
                    if (now != currentDateTracker) {
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

                    // Calculate real-time bedtime status
                    val isBedtimeNow =
                        isBedtimeEnabled && isBedtimeActiveNow(bedtimeStart, bedtimeEnd)

                    if (currentApp == "com.android.settings") {
                        // Priority 0: Always block system settings
                        hideAllOverlaysExcept(appLockOverlay)
                        appLockOverlay.show()
                    } else if (isScreenOn && currentApp.isNotEmpty() && !isOurLauncher) {

                        val isCriticalSystem = criticalSystemPackages.contains(currentApp)
                        val isAppAllowed = allowedPackages.contains(currentApp)

                        // --- THE HIERARCHY OF RESTRICTIONS ---

                        // Priority 1: Bedtime (Blocks everything)
                        if (isBedtimeNow && !isCriticalSystem) {
                            hideAllOverlaysExcept(bedtimeLockOverlay)
                            bedtimeLockOverlay.show()
                        }
                        // Priority 2: App Level Restrictions
                        else if (!isAppAllowed && !isCriticalSystem) {
                            hideAllOverlaysExcept(appLockOverlay)
                            appLockOverlay.show()
                        }
                        // Priority 3: Daily Time Limits & Usage Tracking
                        else {
                            if (!isCriticalSystem) {
                                usedSecondsTodayTracker += 1
                                appUsageMapTracker[currentApp] =
                                    (appUsageMapTracker[currentApp] ?: 0) + 1
                            }

                            if (isTimeLimitEnabled && usedSecondsTodayTracker >= limitInSeconds) {
                                hideAllOverlaysExcept(timeLockOverlay) // PRO FIX 2: Only hide the others!
                                timeLockOverlay.show()
                            } else {
                                hideAllOverlays() // Safe to use the phone, drop all shields!
                            }
                        }
                    } else {
                        // Safe Zone (Our Launcher or Screen Off)
                        hideAllOverlays()
                    }

                    // DB Save every 60 seconds
                    loopCounter++
                    if (loopCounter >= 60) {
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
            usageDao.insertOrUpdateDailyUsage(DailyUsageEntity(childId, date, totalSeconds))
            val appRecords = appMap.map { (pkg, seconds) ->
                AppUsageRecordEntity(childId, date, pkg, seconds)
            }
            if (appRecords.isNotEmpty()) usageDao.insertOrUpdateAppUsages(appRecords)
        } catch (e: Exception) {
            Log.e(TAG, "Database update failed: ${e.message}")
        }
    }

    private fun performFinalSave() {
        val childId = currentChildId ?: return
        if (usedSecondsTodayTracker == 0 && appUsageMapTracker.isEmpty()) return
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                saveDataToRoom(
                    childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker
                )
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        hideAllOverlays()
    }

    override fun onDestroy() {
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