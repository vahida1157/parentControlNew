package com.vahak.mehrban.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.core.data.local.dao.AppRuleDao
import com.vahak.mehrban.core.data.local.dao.ChildSettingsDao
import com.vahak.mehrban.core.data.local.dao.UsageDao
import com.vahak.mehrban.core.data.local.entity.AppUsageRecordEntity
import com.vahak.mehrban.core.data.local.entity.DailyUsageEntity
import com.vahak.mehrban.domain.repository.UsageRepository
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
    lateinit var childSettingsDao: ChildSettingsDao

    @Inject
    lateinit var usageDao: UsageDao

    @Inject
    lateinit var appRuleDao: AppRuleDao

    @Inject
    lateinit var usageRepository: UsageRepository

    private lateinit var timeLockOverlay: RestrictionOverlay
    private lateinit var appLockOverlay: RestrictionOverlay
    private lateinit var sleepTimeLockOverlay: RestrictionOverlay

    private var monitoringJob: Job? = null
    private var currentChildId: String? = null
    private var lastKnownPackage: String = ""

    // --- HOISTED TRACKING VARIABLES ---
    private var currentDateTracker: LocalDate = LocalDate.now()

    // --- LOCAL TRACKERS (Saved to Room, increments instantly offline) ---
    private var usedSecondsTodayTracker = 0
    private var appUsageMapTracker = mutableMapOf<String, Int>()

    // --- EXTERNAL TRACKERS (From Server, represents time spent on OTHER devices) ---
    private var externalDailySecondsTracker = 0
    private var externalAppUsageMapTracker = mutableMapOf<String, Int>()

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
        sleepTimeLockOverlay = RestrictionOverlay(this, this, OverlayType.SLEEP_TIME)

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
        val event = UsageEvents.Event()

        var newlyFoundPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                newlyFoundPackage = event.packageName
            }
        }
        if (newlyFoundPackage != null) lastKnownPackage = newlyFoundPackage
        if (lastKnownPackage.isEmpty()) lastKnownPackage = packageName

        return lastKnownPackage
    }

    private fun isSleepTimeActiveNow(start: LocalTime, end: LocalTime): Boolean {
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

            // 1. Reset all trackers
            currentDateTracker = LocalDate.now()
            externalDailySecondsTracker = 0
            externalAppUsageMapTracker.clear()
            appUsageMapTracker.clear()

            // 2. Load Local State AND Global Offline Cache
            val dailyRecord = usageDao.getDailyUsage(childId, currentDateTracker)
            usedSecondsTodayTracker = dailyRecord?.usedSeconds ?: 0

            // 🚀 THE OFFLINE BOOT FIX: Remember external time even without internet
            val cachedGlobalDaily = dailyRecord?.globalUsedSeconds ?: 0
            externalDailySecondsTracker = maxOf(0, cachedGlobalDaily - usedSecondsTodayTracker)

            val existingAppUsages = usageDao.observeAppUsageForDay(childId, currentDateTracker).first()
            existingAppUsages.forEach { appRecord ->
                appUsageMapTracker[appRecord.packageName] = appRecord.usedSeconds
                // Load global cache for specific apps
                externalAppUsageMapTracker[appRecord.packageName] = maxOf(0, appRecord.globalUsedSeconds - appRecord.usedSeconds)
            }

            Log.d(TAG, "🔄 Local State Restored. Local: $usedSecondsTodayTracker | External: $externalDailySecondsTracker")

            var loopCounter = 0

            combine(
                childSettingsDao.getGlobalSettings(childId), appRuleDao.observeAllowedApps(childId)
            ) { settings, allowedApps ->
                Pair(settings, allowedApps.map { it.packageName }.toSet())
            }.collectLatest { (settings, allowedPackages) ->

                if (settings == null) {
                    Log.w(TAG, "⚠️ Settings are null. Waiting for DB initialization...")
                    return@collectLatest
                }

                val isTimeLimitEnabled = settings.isTimeLimitActive
                val limitInSeconds = settings.dailyTimeLimitMins * 60
                val isSleepTimeEnabled = settings.isSleepTimeActive
                val sleepTimeStart = settings.sleepTimeStart
                val sleepTimeEnd = settings.sleepTimeEnd

                Log.i(
                    TAG,
                    "📊 Settings Loaded | TimeLimit Active: $isTimeLimitEnabled ($limitInSeconds sec) | SleepTime Active: $isSleepTimeEnabled ($sleepTimeStart - $sleepTimeEnd) | Allowed Apps Count: ${allowedPackages.size}"
                )

                val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val launcherPackages = packageManager.queryIntentActivities(homeIntent, 0)
                    .map { it.activityInfo.packageName }.toSet()
                val criticalSystemPackages = setOf("com.android.systemui", "android") + launcherPackages

                Log.d(TAG, "🛡️ System Whitelist active for: $criticalSystemPackages")

                while (isActive) {
                    val now = LocalDate.now()

                    // Midnight Rollover
                    if (now != currentDateTracker) {
                        Log.i(TAG, "🌙 Midnight Rollover detected! Resetting daily trackers.")
                        saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)
                        currentDateTracker = now
                        usedSecondsTodayTracker = 0
                        appUsageMapTracker.clear()
                        externalDailySecondsTracker = 0
                        externalAppUsageMapTracker.clear()
                    }

                    val isScreenOn = powerManager.isInteractive
                    val currentApp = getForegroundPackage()
                    val isOurLauncher = currentApp == packageName
                    val isSleepTimeNow = isSleepTimeEnabled && isSleepTimeActiveNow(sleepTimeStart, sleepTimeEnd)

                    // 🚀 MULTI-DEVICE OFFLINE-FIRST LOGIC: Calculate Effective Time
                    val effectiveDailyTotal = usedSecondsTodayTracker + externalDailySecondsTracker

                    // --- THE HEARTBEAT LOG ---
                    if (BuildConfig.DEBUG) {
                        Log.d(
                            TAG,
                            "⏱️ TICK | Screen: ${if (isScreenOn) "ON" else "OFF"} | App: $currentApp | Effective Time: $effectiveDailyTotal/$limitInSeconds sec | SleepTimeNow: $isSleepTimeNow"
                        )
                    }

                    if (currentApp == "com.android.settings") {
                        Log.w(TAG, "🚨 BLOCKING PRIORITY 0: Child attempted to open Android Settings.")
                    } else if (isScreenOn && currentApp.isNotEmpty() && !isOurLauncher) {

                        val isCriticalSystem = criticalSystemPackages.contains(currentApp)
                        val isAppAllowed = allowedPackages.contains(currentApp)

                        if (isSleepTimeNow && !isCriticalSystem) {
                            Log.w(TAG, "💤 BLOCKING PRIORITY 1: Sleep time is active. Blocking $currentApp")
                            hideAllOverlaysExcept(sleepTimeLockOverlay)
                            sleepTimeLockOverlay.show()
                        } else if (!isAppAllowed && !isCriticalSystem) {
                            Log.w(TAG, "🚫 BLOCKING PRIORITY 2: App is not on the allowed list -> $currentApp")
                            hideAllOverlaysExcept(appLockOverlay)
                            appLockOverlay.show()
                        } else {
                            // Valid usage -> Increment local counters
                            if (!isCriticalSystem) {
                                usedSecondsTodayTracker += 1
                                appUsageMapTracker[currentApp] = (appUsageMapTracker[currentApp] ?: 0) + 1

                                if (BuildConfig.DEBUG) {
                                    Log.v(TAG, "✅ App Allowed: $currentApp | Tracking incremented.")
                                }
                            }

                            // 🚀 Check limits against EFFECTIVE (Local + External) time
                            if (isTimeLimitEnabled && effectiveDailyTotal >= limitInSeconds) {
                                Log.w(TAG, "⏳ BLOCKING PRIORITY 3: Global limit reached ($effectiveDailyTotal sec). Blocking $currentApp")
                                hideAllOverlaysExcept(timeLockOverlay)
                                timeLockOverlay.show()
                            } else {
                                hideAllOverlays()
                            }
                        }
                    } else {
                        hideAllOverlays() // Screen off or inside our Launcher
                    }

                    // --- 60-SECOND BACKGROUND SYNC ---
                    loopCounter++
                    if (loopCounter >= 60) {
                        Log.d(TAG, "⏲️ 60-second mark reached. Triggering DB save & Sync.")
                        saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)

                        // Launch sync in background so we NEVER block the 1-second enforcement loop
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val globalResponse = usageRepository.syncUnsyncedData(
                                    activeChildId = childId, forcePing = false
                                )
                                if (globalResponse != null) {
                                    val globalDaily = globalResponse.globalDailySeconds[childId] ?: usedSecondsTodayTracker
                                    val globalApps = globalResponse.globalAppSeconds[childId] ?: emptyMap()

                                    // Calculate External Delta
                                    externalDailySecondsTracker = maxOf(0, globalDaily - usedSecondsTodayTracker)

                                    globalApps.forEach { (pkg, globalAppTime) ->
                                        val localAppTime = appUsageMapTracker[pkg] ?: 0
                                        externalAppUsageMapTracker[pkg] = maxOf(0, globalAppTime - localAppTime)
                                    }
                                }
                            } catch (_: Exception) {
                                Log.d(TAG, "📡 Offline or Sync Failed. Continuing with local metrics.")
                            }
                        }
                        loopCounter = 0
                    }
                    delay(1000L)
                }
            }
        }
    }

    private fun hideAllOverlays() {
        if (timeLockOverlay.isShowing() || appLockOverlay.isShowing() || sleepTimeLockOverlay.isShowing()) {
            if (BuildConfig.DEBUG) {
                Log.v(TAG, "🧹 Hiding all overlays.")
            }
            timeLockOverlay.hide()
            appLockOverlay.hide()
            sleepTimeLockOverlay.hide()
        }
    }

    private fun hideAllOverlaysExcept(activeOverlay: RestrictionOverlay) {
        if (activeOverlay != timeLockOverlay) timeLockOverlay.hide()
        if (activeOverlay != appLockOverlay) appLockOverlay.hide()
        if (activeOverlay != sleepTimeLockOverlay) sleepTimeLockOverlay.hide()
    }

    private suspend fun saveDataToRoom(
        childId: String, date: LocalDate, totalSeconds: Int, appMap: Map<String, Int>
    ) {
        try {
            Log.i(TAG, "💾 Saving to Room DB... Total: $totalSeconds sec | Apps: ${appMap.size}")
            usageDao.insertOrUpdateDailyUsage(
                DailyUsageEntity(
                    childId, date, totalSeconds, isSynced = false
                )
            )
            val appRecords = appMap.map { (pkg, seconds) ->
                AppUsageRecordEntity(childId, date, pkg, seconds, isSynced = false)
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
                try {
                    usageRepository.syncUnsyncedData(activeChildId = childId, forcePing = false)
                } catch (e: Exception) {
                    Log.e(TAG, "Final sync failed on exit. Data preserved in Room.")
                }
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