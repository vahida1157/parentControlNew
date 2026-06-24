package com.vahak.mehrban

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.vahak.mehrban.core.data.local.dao.CrashLogDao
import com.vahak.mehrban.core.util.GlobalCrashHandler
import com.vahak.mehrban.worker.TelemetrySyncWorker
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.HiltAndroidApp
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class ModernFamilyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface CrashHandlerEntryPoint {
        fun getCrashLogDao(): CrashLogDao
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // 1. Set up the Crash Handler to save locally
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        val crashLogDao = EntryPoints.get(this, CrashHandlerEntryPoint::class.java).getCrashLogDao()
        Thread.setDefaultUncaughtExceptionHandler(GlobalCrashHandler(crashLogDao, defaultHandler))

        // 2. 🚀 THE FIX: Immediately attempt to sync any leftover crashes from previous sessions
        triggerCrashSyncOnBoot()
    }

    private fun triggerCrashSyncOnBoot() {
        // Require internet, but don't care if it's Wi-Fi or Cellular
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // 🚀 THE FIX: Use a OneTimeWorkRequest instead of Periodic
        val syncRequest = OneTimeWorkRequestBuilder<TelemetrySyncWorker>()
            .setConstraints(constraints)
            .build()

        // Use REPLACE so if the app is caught in a rapid crash loop,
        // we don't spam WorkManager with hundreds of identical requests.
        WorkManager.getInstance(this).enqueueUniqueWork(
            "crash_log_sync_on_boot",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}