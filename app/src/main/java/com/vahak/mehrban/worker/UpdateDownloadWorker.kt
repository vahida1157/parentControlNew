package com.vahak.mehrban.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vahak.mehrban.R
import com.vahak.mehrban.data.remote.DownloadState
import com.vahak.mehrban.domain.repository.UpdateDownloadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale

@HiltWorker
class UpdateDownloadWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val downloadManager: UpdateDownloadManager
) : CoroutineWorker(context, params) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString("url") ?: return Result.failure()
        val fileName = inputData.getString("fileName") ?: "update.apk"

        createNotificationChannel()

        var result: Result = Result.failure()

        downloadManager.downloadApk(fileName, downloadUrl).collectLatest { state ->
            when (state) {
                is DownloadState.Downloading -> {
                    // 1. Update the Notification
                    setForeground(createForegroundInfo(state.progress))

                    // 2. Send progress back to the UI (Compose)
                    setProgress(workDataOf("progress" to state.progress))
                }

                is DownloadState.Success -> {
                    // Send the final file path to the UI so it can trigger the installation
                    result = Result.success(workDataOf("filePath" to state.file.absolutePath))
                }

                is DownloadState.Error -> {
                    // 🚀 FIX: Pass the pure Enum name across the WorkManager boundary
                    result = Result.failure(workDataOf("error" to state.error.name))
                }

            }
        }
        return result
    }

    private fun getLocalizedContext(): Context {
        // Read the language we passed in from AppUpdateManager (default to Persian)
        val langCode = inputData.getString("lang") ?: "fa"

        val locale = Locale.Builder().setLanguage(langCode).build()
        val configuration = android.content.res.Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return context.createConfigurationContext(configuration)
    }

    private fun createForegroundInfo(progress: Int): ForegroundInfo {
        // 🚀 Get the context that speaks the correct language
        val localizedContext = getLocalizedContext()

        val notification = NotificationCompat.Builder(localizedContext, "update_channel")
            .setContentTitle(localizedContext.getString(R.string.app_name))
            .setContentText(localizedContext.getString(R.string.update_downloading, progress))
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(1001, notification)
        }
    }

    private fun createNotificationChannel() {
        val localizedContext = getLocalizedContext()

        val channel = NotificationChannel(
            "update_channel",
            localizedContext.getString(R.string.settings_update_check),
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }
}