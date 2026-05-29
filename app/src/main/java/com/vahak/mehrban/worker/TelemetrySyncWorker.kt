package com.vahak.mehrban.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vahak.mehrban.core.data.local.dao.CrashLogDao
import com.vahak.mehrban.data.remote.ApplicationCrashApi // You'll need to create this Retrofit interface
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TelemetrySyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val crashLogDao: CrashLogDao,
    private val applicationCrashApi: ApplicationCrashApi
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val unsyncedCrashes = crashLogDao.getAllCrashes()
        if (unsyncedCrashes.isEmpty()) return Result.success()

        return try {
            // Send the list of crashes to the Spring Boot backend
            val response = applicationCrashApi.syncCrashLogs(unsyncedCrashes)

            if (response.isSuccessful) {
                // If the server received them safely, delete them from the phone
                crashLogDao.deleteCrashes(unsyncedCrashes.map { it.id })
                Result.success()
            } else {
                Result.retry() // Server error, try again later
            }
        } catch (e: Exception) {
            Result.retry() // Offline, try again when Wi-Fi returns
        }
    }
}