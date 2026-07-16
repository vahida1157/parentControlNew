package com.vahak.mehrban.worker

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val INACTIVITY_WORK_NAME = "parent_inactivity_timer"

    fun resetInactivityTimer(context: Context) {
        Timber.d("NotificationScheduler: App sent to background. Scheduling timer for 10 seconds (testing).")

        val inputData = Data.Builder().putInt("days_inactive", 1).build()

        val workRequest = OneTimeWorkRequestBuilder<InactivityWorker>()
            .setInitialDelay(1, TimeUnit.DAYS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            INACTIVITY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelTimer(context: Context) {
        Timber.d("NotificationScheduler: App opened! Canceling inactivity timer.")
        WorkManager.getInstance(context).cancelUniqueWork(INACTIVITY_WORK_NAME)
    }
}