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
import javax.inject.Inject

@AndroidEntryPoint
class TimeLimitEnforcerService : LifecycleService() {

    @Inject
    lateinit var settingsDao: SettingsDao

    @Inject
    lateinit var usageDao: UsageDao

    @Inject
    lateinit var appRuleDao: AppRuleDao // 1. INJECT APP RULES DAO

    private lateinit var timeLockOverlay: TimeLockOverlay
    private lateinit var appLockOverlay: TimeLockOverlay
    private var monitoringJob: Job? = null
    private var currentChildId: String? = null
    private var lastKnownPackage: String = ""

    // --- HOISTED TRACKING VARIABLES ---
    private var currentDateTracker: LocalDate = LocalDate.now()
    private var usedSecondsTodayTracker: Int = 0
    private val appUsageMapTracker = mutableMapOf<String, Int>()

    companion object {
        private const val TAG = "EnforcerService"
        private const val CHANNEL_ID = "TimeLimitEnforcerChannel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_CHILD_ID = "EXTRA_CHILD_ID"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")

        timeLockOverlay = TimeLockOverlay(this, this)
        appLockOverlay = TimeLockOverlay(this, this)
//        timeLockOverlay = TimeLockOverlay(this, "زمان استفاده شما به پایان رسید!")
//        appLockOverlay = TimeLockOverlay(this, "استفاده از این نرم افزار مجاز نیست!")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand: Action = ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                val childId = intent.getStringExtra(EXTRA_CHILD_ID)
                if (childId != null) {
                    currentChildId = childId
                    startForeground(NOTIFICATION_ID, createNotification())
                    startMonitoring(childId)
                } else {
                    Log.e(TAG, "Start failed: Child ID is null")
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                Log.w(TAG, "Action Stop received from UI")
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

        if (newlyFoundPackage != null) {
            lastKnownPackage = newlyFoundPackage
        }

        if (lastKnownPackage.isEmpty()) {
            lastKnownPackage = packageName
        }

        return lastKnownPackage
    }

    private fun startMonitoring(childId: String) {
        monitoringJob?.cancel()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager

        monitoringJob = lifecycleScope.launch {
            Log.i(TAG, "🚀 Monitoring Started for Child: $childId")

            // Initialize/Sync trackers with database
            currentDateTracker = LocalDate.now()
            usedSecondsTodayTracker =
                usageDao.getDailyUsage(childId, currentDateTracker)?.usedSeconds ?: 0

            appUsageMapTracker.clear()
            val existingAppUsages =
                usageDao.observeAppUsageForDay(childId, currentDateTracker).first()
            existingAppUsages.forEach { record ->
                appUsageMapTracker[record.packageName] = record.usedSeconds
            }

            Log.d(TAG, "💾 Data Restored: $usedSecondsTodayTracker seconds used today.")

            var loopCounter = 0

            // 2. COMBINE SETTINGS AND ALLOWED APPS FLOWS
            combine(
                settingsDao.getGlobalSettings(childId),
                appRuleDao.observeAllowedApps(childId)
            ) { settings, allowedApps ->
                Pair(settings, allowedApps.map { it.packageName }.toSet())
            }.collectLatest { (settings, allowedPackages) ->

                if (settings == null || !settings.isTimeLimitActive) {
                    Log.w(TAG, "🛑 Settings changed: Time limit is INACTIVE. Shutting down service.")
                    timeLockOverlay.hide()
                    appLockOverlay.hide()
                    stopSelf()
                    return@collectLatest
                }

                val limitInSeconds = settings.dailyTimeLimitMins * 60
                Log.i(TAG, "🎯 Active Limit: $limitInSeconds sec | Allowed Apps: ${allowedPackages.size}")

                // --- DYNAMIC SYSTEM WHITELIST ---
                // Fetch all Home/Launcher packages dynamically so Recents work on Xiaomi, Samsung, Pixel, etc.
                val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val launcherPackages = packageManager.queryIntentActivities(homeIntent, 0)
                    .map { it.activityInfo.packageName }
                    .toSet()

                // Combine SystemUI, Android Core, and all device Launchers
                val criticalSystemPackages = setOf("com.android.systemui", "android") + launcherPackages

                Log.i(TAG, "Critical System Packages allowed: $criticalSystemPackages")

                while (isActive) {
                    val now = LocalDate.now()

                    // Midnight Rollover Check
                    if (now != currentDateTracker) {
                        Log.i(TAG, "🌙 Midnight Rollover! Resetting trackers.")
                        saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)
                        currentDateTracker = now
                        usedSecondsTodayTracker = 0
                        appUsageMapTracker.clear()
                    }

                    val isScreenOn = powerManager.isInteractive
                    val currentApp = getForegroundPackage()
                    val isOurLauncher = currentApp == packageName

                    // --- VERBOSE HEARTBEAT LOG ---
                    Log.d(TAG, "⏱️ TICK | Screen: ${if (isScreenOn) "ON" else "OFF"} | App: $currentApp | Total: $usedSecondsTodayTracker/$limitInSeconds")

                    if (currentApp == "com.android.settings") {
                        Log.w(TAG, "🚨 Security: Blocking access to Android Settings")
                        timeLockOverlay.hide()
                        appLockOverlay.show()
                    } else if (isScreenOn) {

                        if (currentApp.isNotEmpty() && !isOurLauncher) {

                            val isCriticalSystem = criticalSystemPackages.contains(currentApp)
                            val isAppAllowed = allowedPackages.contains(currentApp)

                            // 3. LAYER TWO DEFENSE: APP RESTRICTION CHECK
                            if (!isAppAllowed && !isCriticalSystem) {
                                timeLockOverlay.hide() // Ensure time lock isn't showing
                                appLockOverlay.show()  // Show restricted app lock
                            } else {
                                appLockOverlay.hide()  // App is allowed, hide restrict overlay

                                // Increment time if it's a real app (not system UI dropdown)
                                if (!isCriticalSystem) {
                                    usedSecondsTodayTracker += 1
                                    appUsageMapTracker[currentApp] = (appUsageMapTracker[currentApp] ?: 0) + 1
                                }

                                // 4. LAYER ONE DEFENSE: TIME LIMIT CHECK
                                if (usedSecondsTodayTracker >= limitInSeconds) {
                                    timeLockOverlay.show()
                                } else {
                                    timeLockOverlay.hide()
                                }
                            }
                        } else {
                            // Safe Zone (Our Launcher)
                            appLockOverlay.hide()
                            timeLockOverlay.hide()
                        }
                    } else {
                        // Screen is off
                        appLockOverlay.hide()
                        timeLockOverlay.hide()
                    }

                    // Periodic Database Save (Every 60 seconds)
                    loopCounter++
                    if (loopCounter >= 60) {
                        saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)
                        loopCounter = 0
                    }

                    delay(1000L)
                }
            }
        }
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

        Log.i(TAG, "💾 [FINAL SAVE] Shutdown detected. Saving final $usedSecondsTodayTracker seconds...")
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)
            }
        }
    }

    private fun stopMonitoring() {
        Log.i(TAG, "stopMonitoring() called")
        monitoringJob?.cancel()
        timeLockOverlay.hide()
        appLockOverlay.hide()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service Destroyed")
        performFinalSave()
        stopMonitoring()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("محافظت خانواده فعال است")
            .setContentText("در حال نظارت بر استفاده از دستگاه...")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "نظارت خانواده", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}