package com.vahak.mehrban.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.dao.AppRuleDao
import com.vahak.mehrban.core.data.local.dao.ChildSettingsDao
import com.vahak.mehrban.core.data.local.dao.UsageDao
import com.vahak.mehrban.core.data.local.entity.AppUsageRecordEntity
import com.vahak.mehrban.core.data.local.entity.DailyUsageEntity
import com.vahak.mehrban.core.util.BrowserUsageTracker
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
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

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

    private var currentDateTracker: LocalDate = LocalDate.now()
    private var usedSecondsTodayTracker = 0
    private var appUsageMapTracker = mutableMapOf<String, Int>()
    private var externalDailySecondsTracker = 0
    private var externalAppUsageMapTracker = mutableMapOf<String, Int>()

    companion object {
        private const val TAG = "EnforcerService"
        private const val CHANNEL_ID = "RestrictionEnforcerChannel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_CHILD_ID = "EXTRA_CHILD_ID"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"

        const val BROWSER_PACKAGE_KEY = "com.vahak.mehrban.browser"
    }

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("⚙️ Service Created. Initializing Overlays...")

        timeLockOverlay = RestrictionOverlay(this, this, OverlayType.TIME_LIMIT)
        appLockOverlay = RestrictionOverlay(this, this, OverlayType.APP_BLOCK)
        sleepTimeLockOverlay = RestrictionOverlay(this, this, OverlayType.SLEEP_TIME)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.tag(TAG).d("🚀 onStartCommand triggered with Action = ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                val childId = intent.getStringExtra(EXTRA_CHILD_ID)
                if (childId != null) {
                    currentChildId = childId
                    startForeground(NOTIFICATION_ID, createNotification())
                    startMonitoring(childId)
                } else {
                    Timber.tag(TAG).e("❌ Start failed: Child ID is null. Shutting down.")
                    stopSelf()
                }
            }

            ACTION_STOP -> {
                Timber.tag(TAG).w("🛑 Action Stop received. Halting monitoring.")
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
            Timber.tag(TAG).i("👀 startMonitoring() initiated for Child ID: $childId")

            currentDateTracker = LocalDate.now()
            externalDailySecondsTracker = 0
            externalAppUsageMapTracker.clear()
            appUsageMapTracker.clear()

            val dailyRecord = usageDao.getDailyUsage(childId, currentDateTracker)
            usedSecondsTodayTracker = dailyRecord?.usedSeconds ?: 0

            val cachedGlobalDaily = dailyRecord?.globalUsedSeconds ?: 0
            externalDailySecondsTracker = maxOf(0, cachedGlobalDaily - usedSecondsTodayTracker)

            val existingAppUsages =
                usageDao.observeAppUsageForDay(childId, currentDateTracker).first()
            existingAppUsages.forEach { appRecord ->
                appUsageMapTracker[appRecord.packageName] = appRecord.usedSeconds
                externalAppUsageMapTracker[appRecord.packageName] =
                    maxOf(0, appRecord.globalUsedSeconds - appRecord.usedSeconds)
            }

            Timber.tag(TAG)
                .d("🔄 Local State Restored. Local: $usedSecondsTodayTracker | External: $externalDailySecondsTracker")

            var loopCounter = 0

            combine(
                childSettingsDao.getGlobalSettings(childId), appRuleDao.observeAllowedApps(childId)
            ) { settings, allowedApps ->
                Pair(settings, allowedApps.map { it.packageName }.toSet())
            }.collectLatest { (settings, allowedPackages) ->

                if (settings == null) {
                    Timber.tag(TAG).w("⚠️ Settings are null. Waiting for DB initialization...")
                    return@collectLatest
                }

                val isTimeLimitEnabled = settings.isTimeLimitActive
                val baseLimitInSeconds = settings.dailyTimeLimitMins * 60
                val appliedBonus = if (settings.isExerciseRewardEnabled) settings.earnedBonusSecondsToday else 0
                val totalLimitInSeconds = baseLimitInSeconds + appliedBonus
                val isSleepTimeEnabled = settings.isSleepTimeActive
                val sleepTimeStart = settings.sleepTimeStart
                val sleepTimeEnd = settings.sleepTimeEnd

                Timber.tag(TAG)
                    .i("📊 Settings Loaded | TimeLimit Active: $isTimeLimitEnabled ($totalLimitInSeconds sec) | SleepTime Active: $isSleepTimeEnabled ($sleepTimeStart - $sleepTimeEnd) | Allowed Apps Count: ${allowedPackages.size}")

                val homeIntent =
                    Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
                val launcherPackages = packageManager.queryIntentActivities(homeIntent, 0)
                    .map { it.activityInfo.packageName }.toSet()
                val criticalSystemPackages =
                    setOf("com.android.systemui", "android") + launcherPackages

                Timber.tag(TAG).d("🛡️ System Whitelist active for: $criticalSystemPackages")

                while (isActive) {
                    val now = LocalDate.now()

                    if (now != currentDateTracker) {
                        Timber.tag(TAG).i("🌙 Midnight Rollover detected! Resetting daily trackers.")
                        saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)
                        currentDateTracker = now
                        usedSecondsTodayTracker = 0
                        appUsageMapTracker.clear()
                        externalDailySecondsTracker = 0
                        externalAppUsageMapTracker.clear()
                    }

                    val isScreenOn = powerManager.isInteractive
                    val currentApp = getForegroundPackage()

                    val isBrowserForeground = BrowserUsageTracker.isBrowserForeground
                    val isOurLauncher = currentApp == packageName && !isBrowserForeground
                    val isCriticalSystem = criticalSystemPackages.contains(currentApp) && !isBrowserForeground
                    val isSleepTimeNow = isSleepTimeEnabled && isSleepTimeActiveNow(sleepTimeStart, sleepTimeEnd)

                    // 🚀 THE FIX: 1. COUNT TIME FIRST (Before any blocking logic)
                    if (isScreenOn && currentApp.isNotEmpty() && !isOurLauncher && !isCriticalSystem) {
                        usedSecondsTodayTracker += 1
                        val trackPkg = if (isBrowserForeground) BROWSER_PACKAGE_KEY else currentApp
                        appUsageMapTracker[trackPkg] = (appUsageMapTracker[trackPkg] ?: 0) + 1
                    }

                    val effectiveDailyTotal = usedSecondsTodayTracker + externalDailySecondsTracker

                    if (BuildConfig.DEBUG) {
                        Timber.tag(TAG).d("⏱️ TICK | Screen: ${if (isScreenOn) "ON" else "OFF"} | App: $currentApp (Browser: $isBrowserForeground) | Effective Time: $effectiveDailyTotal/$totalLimitInSeconds sec | SleepTimeNow: $isSleepTimeNow")
                    }

                    // 2. APPLY RESTRICTIONS & OVERLAYS
                    if (isScreenOn && currentApp.isNotEmpty() && !isOurLauncher) {

                        val isAppAllowed = allowedPackages.contains(currentApp)

                        if (isSleepTimeNow && !isCriticalSystem) {
                            Timber.tag(TAG).w("💤 BLOCKING PRIORITY 1: Sleep time")
                            hideAllOverlaysExcept(sleepTimeLockOverlay)
                            sleepTimeLockOverlay.show()
                        } else if (!isAppAllowed && !isCriticalSystem && !isBrowserForeground) {
                            Timber.tag(TAG).w("🚫 BLOCKING PRIORITY 2: Not Allowed App")
                            hideAllOverlaysExcept(appLockOverlay)
                            appLockOverlay.show()
                        } else if (isTimeLimitEnabled && effectiveDailyTotal >= totalLimitInSeconds && !isCriticalSystem) {
                            Timber.tag(TAG).w("⏳ BLOCKING PRIORITY 3: Global Time Limit")
                            hideAllOverlaysExcept(timeLockOverlay)
                            timeLockOverlay.show()
                        } else {
                            hideAllOverlays()
                        }
                    } else {
                        hideAllOverlays()
                    }

                    // --- 60-SECOND BACKGROUND SYNC ---
                    loopCounter++
                    if (loopCounter >= 60) {
                        Timber.tag(TAG).d("⏲️ 60-second mark reached. Triggering DB save & Sync.")
                        saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val globalResponse = usageRepository.syncUnsyncedData(activeChildId = childId, forcePing = false)
                                if (globalResponse != null) {
                                    val globalDaily = globalResponse.globalDailySeconds[childId] ?: usedSecondsTodayTracker
                                    val globalApps = globalResponse.globalAppSeconds[childId] ?: emptyMap()

                                    externalDailySecondsTracker = maxOf(0, globalDaily - usedSecondsTodayTracker)
                                    globalApps.forEach { (pkg, globalAppTime) ->
                                        val localAppTime = appUsageMapTracker[pkg] ?: 0
                                        externalAppUsageMapTracker[pkg] = maxOf(0, globalAppTime - localAppTime)
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.tag(TAG).d("📡 Offline or Sync Failed. Continuing with local metrics. Error: ${e.message}")
                            }
                        }
                        loopCounter = 0
                    }
                    delay(1000L.milliseconds)
                }
            }
        }
    }

    private fun hideAllOverlays() {
        if (timeLockOverlay.isShowing() || appLockOverlay.isShowing() || sleepTimeLockOverlay.isShowing()) {
            if (BuildConfig.DEBUG) {
                Timber.tag(TAG).v("🧹 Hiding all overlays.")
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
            Timber.tag(TAG)
                .i("💾 Saving to Room DB... Total: $totalSeconds sec | Apps: ${appMap.size}")
            usageDao.insertOrUpdateDailyUsage(
                DailyUsageEntity(
                    childId,
                    date,
                    totalSeconds,
                    isSynced = false
                )
            )
            val appRecords = appMap.map { (pkg, seconds) ->
                AppUsageRecordEntity(childId, date, pkg, seconds, isSynced = false)
            }
            if (appRecords.isNotEmpty()) usageDao.insertOrUpdateAppUsages(appRecords)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "❌ Database update failed")
        }
    }

    private fun performFinalSave() {
        val childId = currentChildId ?: return
        if (usedSecondsTodayTracker == 0 && appUsageMapTracker.isEmpty()) return

        Timber.tag(TAG).i("💾 [FINAL SAVE] Service shutting down. Saving final state...")
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                saveDataToRoom(
                    childId,
                    currentDateTracker,
                    usedSecondsTodayTracker,
                    appUsageMapTracker
                )
                try {
                    usageRepository.syncUnsyncedData(activeChildId = childId, forcePing = false)
                } catch (e: Exception) {
                    Timber.tag(TAG).e(e, "Final sync failed on exit. Data preserved in Room.")
                }
            }
        }
    }

    private fun stopMonitoring() {
        Timber.tag(TAG).i("🛑 stopMonitoring() called. Canceling job and hiding overlays.")
        monitoringJob?.cancel()
        hideAllOverlays()
    }

    override fun onDestroy() {
        Timber.tag(TAG).d("💥 Service Destroyed")
        performFinalSave()
        stopMonitoring()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_family_protection_title))
            .setContentText(getString(R.string.notification_family_protection_text))
            .setSmallIcon(android.R.drawable.ic_secure)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}