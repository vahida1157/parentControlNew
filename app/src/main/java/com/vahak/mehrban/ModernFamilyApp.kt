package com.vahak.mehrban

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vahak.mehrban.core.data.local.dao.CrashLogDao
import com.vahak.mehrban.core.util.GlobalCrashHandler
import com.vahak.mehrban.worker.TelemetrySyncWorker
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ModernFamilyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    // 🚀 We use an EntryPoint to safely get the DAO without breaking Hilt's lifecycle
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CrashHandlerEntryPoint {
        fun getCrashLogDao(): CrashLogDao
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Install Crash Handler (from previous step)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashLogDao = EntryPoints.get(this, CrashHandlerEntryPoint::class.java).getCrashLogDao()
        Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(crashLogDao, defaultHandler))

        // 2. 🚀 ENQUEUE THE CRASH SYNC WORKER
        setupCrashSyncWorker()
    }
    private fun setupCrashSyncWorker() {
        // Require any working internet connection (Wi-Fi or Cellular)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Run this check every 6 hours
        val syncRequest = PeriodicWorkRequestBuilder<TelemetrySyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        // Use KEEP so we don't accidentally restart the timer if the app opens multiple times
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "crash_log_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}