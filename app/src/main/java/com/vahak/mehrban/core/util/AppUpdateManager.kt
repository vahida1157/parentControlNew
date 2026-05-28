package com.vahak.mehrban.core.util

import android.content.Context
import androidx.work.*
import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.UpdateState
import com.vahak.mehrban.AppDownloadState
import com.vahak.mehrban.core.data.local.UpdateCacheManager
import com.vahak.mehrban.data.remote.AppUpdateApi
import com.vahak.mehrban.worker.UpdateDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateApi: AppUpdateApi,
    private val updateCacheManager: UpdateCacheManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val workManager = WorkManager.getInstance(context)

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Checking)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _isUpdateIgnored = MutableStateFlow(false)
    val isUpdateIgnored: StateFlow<Boolean> = _isUpdateIgnored.asStateFlow()

    private val _appDownloadState = MutableStateFlow<AppDownloadState>(AppDownloadState.Idle)
    val appDownloadState: StateFlow<AppDownloadState> = _appDownloadState.asStateFlow()

    private val _downloadedFilePath = MutableStateFlow<String?>(null)
    val downloadedFilePath: StateFlow<String?> = _downloadedFilePath.asStateFlow()

    init {
        observeDownloadWork()
        loadCachedUpdateState()
        checkForUpdates(forceNetworkCall = false)
    }

    private fun loadCachedUpdateState() {
        val cachedInfo = updateCacheManager.getCachedUpdateInfo()
        if (cachedInfo != null && cachedInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
            _updateState.value = UpdateState.UpdateAvailable(cachedInfo, cachedInfo.isForced)
            if (!cachedInfo.isForced && updateCacheManager.getIgnoredVersion() == cachedInfo.latestVersionCode) {
                _isUpdateIgnored.value = true
            }
        } else {
            _updateState.value = UpdateState.UpToDate
        }
    }

    fun checkForUpdates(
        forceNetworkCall: Boolean = true,
        onResult: ((updateFound: Boolean) -> Unit)? = null
    ) {
        val lastCheck = updateCacheManager.getLastCheckTime()
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L

        if (!forceNetworkCall && (now - lastCheck) < twentyFourHours) return

        scope.launch {
            if (forceNetworkCall) _updateState.value = UpdateState.Checking

            try {
                // 🚀 PASS CURRENT VERSION TO BACKEND
                val response = appUpdateApi.getAppVersion(BuildConfig.VERSION_CODE)

                if (response.isSuccessful && response.body() != null) {
                    val serverInfo = response.body()!!
                    updateCacheManager.saveUpdateInfo(serverInfo, now)

                    if (serverInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
                        _updateState.value = UpdateState.UpdateAvailable(serverInfo, serverInfo.isForced)
                        if (updateCacheManager.getIgnoredVersion() != serverInfo.latestVersionCode) {
                            _isUpdateIgnored.value = false
                        }
                        onResult?.invoke(true)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                        onResult?.invoke(false)
                    }
                } else {
                    if (forceNetworkCall) loadCachedUpdateState()
                    onResult?.invoke(false)
                }
            } catch (_: Exception) {
                if (forceNetworkCall) loadCachedUpdateState()
                onResult?.invoke(false)
            }
        }
    }

    // 🚀 THE PRE-FLIGHT CHECK
    fun startDownload(cachedUrl: String, cachedVersionName: String) {
        _appDownloadState.value = AppDownloadState.Connecting

        scope.launch {
            try {
                // 1. Verify with server one last time before downloading
                val response = appUpdateApi.getAppVersion(BuildConfig.VERSION_CODE)

                val finalUrl: String
                val finalVersionName: String

                if (response.isSuccessful && response.body() != null) {
                    val serverInfo = response.body()!!
                    // 1a. If server says up to date suddenly (rollback), abort.
                    if (serverInfo.latestVersionCode <= BuildConfig.VERSION_CODE) {
                        _appDownloadState.value = AppDownloadState.Error("بروزرسانی لغو شد. شما در حال استفاده از آخرین نسخه تایید شده هستید.")
                        _updateState.value = UpdateState.UpToDate
                        return@launch
                    }
                    // 1b. Use the absolute freshest data
                    finalUrl = serverInfo.downloadUrl
                    finalVersionName = serverInfo.latestVersionName
                } else {
                    // 1c. If server fails, fallback to the cache we passed in
                    finalUrl = cachedUrl
                    finalVersionName = cachedVersionName
                }

                // 2. Fire the WorkManager with the verified URL
                val fileName = "mehrban-update-v$finalVersionName.apk"
                val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                    .setInputData(workDataOf("url" to finalUrl, "fileName" to fileName))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                workManager.enqueueUniqueWork(
                    "app_update_download",
                    ExistingWorkPolicy.REPLACE,
                    downloadRequest
                )

            } catch (_: Exception) {
                // If offline during pre-flight, fallback to cached data and try anyway
                val fileName = "mehrban-update-v$cachedVersionName.apk"
                val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
                    .setInputData(workDataOf("url" to cachedUrl, "fileName" to fileName))
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build()

                workManager.enqueueUniqueWork("app_update_download", ExistingWorkPolicy.REPLACE, downloadRequest)
            }
        }
    }

    fun dismissOptionalUpdate() {
        _isUpdateIgnored.value = true
        val currentState = _updateState.value
        if (currentState is UpdateState.UpdateAvailable) {
            updateCacheManager.setIgnoredVersion(currentState.info.latestVersionCode)
        }
    }

    fun unignoreUpdate() {
        _isUpdateIgnored.value = false
    }

    fun clearDownloadedFilePath() {
        _downloadedFilePath.value = null
        _appDownloadState.value = AppDownloadState.Idle
    }

    private fun observeDownloadWork() {
        scope.launch {
            workManager.getWorkInfosForUniqueWorkFlow("app_update_download").collect { workInfos ->
                val workInfo = workInfos.firstOrNull() ?: return@collect

                // 🚀 THE FIX: Check if the app is already running the updated version
                val cachedInfo = updateCacheManager.getCachedUpdateInfo()
                val isAlreadyUpdated = cachedInfo != null && BuildConfig.VERSION_CODE >= cachedInfo.latestVersionCode

                // If we are already updated, ignore stale "SUCCEEDED" or "FAILED" states from the old version
                if (isAlreadyUpdated && workInfo.state.isFinished) {
                    // Optional but recommended: Clear the old finished work from the database
                    workManager.pruneWork()
                    return@collect
                }

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> _appDownloadState.value = AppDownloadState.Connecting
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("progress", -1)
                        if (progress >= 0) _appDownloadState.value = AppDownloadState.Downloading(progress)
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        val path = workInfo.outputData.getString("filePath") ?: ""
                        _appDownloadState.value = AppDownloadState.Success(path)
                        _downloadedFilePath.value = path
                    }
                    WorkInfo.State.FAILED -> {
                        val errorMsg = workInfo.outputData.getString("error") ?: "خطا در دانلود فایل"
                        _appDownloadState.value = AppDownloadState.Error(errorMsg)
                    }
                    else -> {}
                }
            }
        }
    }
}