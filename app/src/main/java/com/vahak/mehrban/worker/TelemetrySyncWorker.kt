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

        // 🚀 Map the Room Entities to clean Network DTOs
        val networkPayload = unsyncedCrashes.map { entity ->
            ApplicationCrashApi.CrashLogDto(
                id = entity.id,
                timestamp = entity.timestamp,
                appVersion = entity.appVersion,
                androidVersion = entity.androidVersion,
                deviceModel = entity.deviceModel,
                exceptionType = entity.exceptionType,
                stackTrace = entity.stackTrace
            )
        }

        return try {
            // Change Retrofit API to accept List<CrashLogDto>
            val response = applicationCrashApi.syncCrashLogs(networkPayload)

            if (response.isSuccessful) {
                crashLogDao.deleteCrashes(unsyncedCrashes.map { it.id })
                Result.success()
            } else {
                Result.retry()
            }
        } catch (_: Exception) {
            Result.retry()
        }
    }
}