package com.vahak.parentcontroll.core.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.domain.manager.UsageTracker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TimeLimitEnforcerService : LifecycleService() {

    @Inject
    lateinit var settingsDao: SettingsDao

    private lateinit var usageTracker: UsageTracker
    private lateinit var timeLockOverlay: TimeLockOverlay

    private var monitoringJob: Job? = null
    private var currentChildId: String? = null

    companion object {
        private const val CHANNEL_ID = "TimeLimitEnforcerChannel"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_CHILD_ID = "EXTRA_CHILD_ID"
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

    override fun onCreate() {
        super.onCreate()
        usageTracker = UsageTracker(this)
        timeLockOverlay = TimeLockOverlay(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId) // Required for LifecycleService

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

        // START_STICKY tells Android: "If you must kill this for memory, restart it ASAP"
        return START_STICKY
    }

    private fun startMonitoring(childId: String) {
        monitoringJob?.cancel()

        // LifecycleScope automatically cleans up when the service is destroyed
        monitoringJob = lifecycleScope.launch {
            // 1. Observe the database dynamically
            settingsDao.getGlobalSettings(childId).collectLatest { settings ->
                if (settings == null || !settings.isTimeLimitActive) {
                    timeLockOverlay.hide()
                    return@collectLatest
                }

                val limitInMinutes = settings.dailyTimeLimitMins

                // 2. The Monitoring Loop
                while (isActive) {
                    val usedMinutes = usageTracker.getTodayUsageInMinutes()

                    if (usedMinutes >= limitInMinutes) {
                        timeLockOverlay.show()
                    } else {
                        timeLockOverlay.hide()
                    }

                    // Check every 30 seconds
                    delay(30_000L)
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitoringJob?.cancel()
        timeLockOverlay.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
    }

    // --- Mandatory Foreground Notification ---
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("محافظت خانواده فعال است") // "Family Protection Active"
            .setContentText("در حال نظارت بر استفاده از دستگاه...")
            .setSmallIcon(android.R.drawable.ic_secure) // Replace with your app icon later
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "نظارت خانواده", // "Family Monitoring"
            NotificationManager.IMPORTANCE_LOW // Low priority so it doesn't make a sound
        )
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }
}