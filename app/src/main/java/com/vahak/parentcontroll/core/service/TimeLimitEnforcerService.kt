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

    private lateinit var timeLockOverlay: TimeLockOverlay
    private var monitoringJob: Job? = null
    private var currentChildId: String? = null
    private var lastKnownPackage: String = ""

    // 1. HOIST THE TRACKING VARIABLES
    // Moving these here so they survive even when the monitoring loop is cancelled
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
        timeLockOverlay = TimeLockOverlay(this)
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
                Log.w(TAG, "Action Stop received")
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

            // 2. USE THE HOISTED VARIABLES
            currentDateTracker = LocalDate.now()
            usedSecondsTodayTracker = usageDao.getDailyUsage(childId, currentDateTracker)?.usedSeconds ?: 0
            appUsageMapTracker.clear()

            Log.d(TAG, "💾 Loaded initial time from DB: $usedSecondsTodayTracker seconds")

            val existingAppUsages = usageDao.observeAppUsageForDay(childId, currentDateTracker).first()
            existingAppUsages.forEach { record ->
                appUsageMapTracker[record.packageName] = record.usedSeconds
            }

            var loopCounter = 0

            settingsDao.getGlobalSettings(childId).collectLatest { settings ->
                if (settings == null || !settings.isTimeLimitActive) {
                    Log.w(TAG, "🛑 Settings changed: Time limit is INACTIVE. Shutting down service.")
                    timeLockOverlay.hide()
                    stopSelf()
                    return@collectLatest
                }

                val limitInSeconds = settings.dailyTimeLimitMins * 60

                while (isActive) {
                    val now = LocalDate.now()
                    if (now != currentDateTracker) {
                        Log.i(TAG, "🌙 Midnight Rollover! Saving data and resetting clock.")
                        saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)
                        currentDateTracker = now
                        usedSecondsTodayTracker = 0
                        appUsageMapTracker.clear()
                    }

                    val isScreenOn = powerManager.isInteractive
                    val currentApp = getForegroundPackage()
                    val isOurLauncher = currentApp == packageName

                    if (currentApp == "com.android.settings") {
                        timeLockOverlay.show()
                    } else if (isScreenOn) {
                        if (currentApp.isNotEmpty() && !isOurLauncher) {
                            usedSecondsTodayTracker += 1
                            appUsageMapTracker[currentApp] = (appUsageMapTracker[currentApp] ?: 0) + 1
                        }

                        if (usedSecondsTodayTracker >= limitInSeconds) {
                            if (!isOurLauncher) {
                                timeLockOverlay.show()
                            } else {
                                timeLockOverlay.hide()
                            }
                        } else {
                            timeLockOverlay.hide()
                        }
                    } else {
                        timeLockOverlay.hide()
                    }

                    loopCounter++
                    if (loopCounter >= 60) {
                        Log.i(TAG, "💾 Periodic Save: Total=$usedSecondsTodayTracker")
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
            Log.v(TAG, "Database successfully updated for $date")
        } catch (e: Exception) {
            Log.e(TAG, "Database update failed: ${e.message}")
        }
    }

    // 3. CREATE THE FINAL SAVE FUNCTION
    private fun performFinalSave() {
        val childId = currentChildId ?: return
        if (usedSecondsTodayTracker == 0 && appUsageMapTracker.isEmpty()) return

        Log.i(TAG, "💾 Triggering Final Save before shutdown! Catching those lost seconds.")

        // When a service is destroyed, its lifecycleScope is cancelled immediately.
        // We MUST use NonCancellable inside a fresh IO coroutine to ensure Room has enough time to write the data.
        CoroutineScope(Dispatchers.IO).launch {
            withContext(NonCancellable) {
                saveDataToRoom(childId, currentDateTracker, usedSecondsTodayTracker, appUsageMapTracker)
            }
        }
    }

    private fun stopMonitoring() {
        Log.i(TAG, "Stop Monitoring called")
        monitoringJob?.cancel()
        timeLockOverlay.hide()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service Destroyed")
        // 4. CALL THE FINAL SAVE RIGHT BEFORE DEATH
        performFinalSave()
        stopMonitoring()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("محافظت خانواده فعال است")
            .setContentText("در حال نظارت بر استفاده از دستگاه...")
            .setSmallIcon(android.R.drawable.ic_secure).setOngoing(true).build()
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(CHANNEL_ID, "نظارت خانواده", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}