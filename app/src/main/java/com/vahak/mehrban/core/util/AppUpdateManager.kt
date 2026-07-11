package com.vahak.mehrban.core.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.farsitel.bazaar.updater.BazaarUpdater
import com.farsitel.bazaar.updater.UpdateResult
import com.vahak.mehrban.AppDownloadState
import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.UpdateState
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.UpdateCacheManager
import com.vahak.mehrban.data.remote.AppUpdateApi
import com.vahak.mehrban.data.remote.AppVersionDto
import com.vahak.mehrban.data.remote.DownloadError
import com.vahak.mehrban.worker.UpdateDownloadWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class AppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateApi: AppUpdateApi,
    private val updateCacheManager: UpdateCacheManager,
    private val sessionManager: SessionManager,
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
        @Suppress(
            "SimplifyBooleanWithConstants", "KotlinConstantConditions"
        ) if (BuildConfig.INSTALL_SOURCE != "myket") {
            loadCachedUpdateState()
        }
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
        forceNetworkCall: Boolean = true, onResult: ((updateFound: Boolean) -> Unit)? = null
    ) {
        val lastCheck = updateCacheManager.getLastCheckTime()
        val now = System.currentTimeMillis()
        val twentyFourHours = 24 * 60 * 60 * 1000L

        if (!forceNetworkCall && (now - lastCheck) < twentyFourHours) return

        scope.launch {
            if (forceNetworkCall) _updateState.value = UpdateState.Checking

            @Suppress("KotlinConstantConditions") when (BuildConfig.INSTALL_SOURCE) {
                "website" -> checkWebsiteUpdate(now, onResult)
                "bazaar" -> checkBazaarUpdate(now, onResult)
                "myket" -> {
                    // 🚀 THE ULTIMATE MYKET INTENT TRICK
                    // Directly fire Myket's system check. It handles the popup automatically
                    // if an update exists, or stays silent if updated.
                    triggerMyketInAppUpdate()
                    _updateState.value = UpdateState.UpToDate
                    onResult?.invoke(false)
                }

                else -> {
                    _updateState.value = UpdateState.UpToDate
                    onResult?.invoke(false)
                }
            }
        }
    }

    private suspend fun checkWebsiteUpdate(now: Long, onResult: ((Boolean) -> Unit)?) {
        try {
            val response = appUpdateApi.getAppVersion(BuildConfig.VERSION_CODE)
            if (response.isSuccessful && response.body() != null) {
                val serverInfo = response.body()!!
                updateCacheManager.saveUpdateInfo(serverInfo, now)

                if (serverInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
                    _updateState.value =
                        UpdateState.UpdateAvailable(serverInfo, serverInfo.isForced)
                    if (updateCacheManager.getIgnoredVersion() != serverInfo.latestVersionCode) {
                        _isUpdateIgnored.value = false
                    }
                    onResult?.invoke(true)
                } else {
                    _updateState.value = UpdateState.UpToDate
                    onResult?.invoke(false)
                }
            } else {
                loadCachedUpdateState()
                onResult?.invoke(false)
            }
        } catch (_: Exception) {
            loadCachedUpdateState()
            onResult?.invoke(false)
        }
    }

    private suspend fun checkBazaarUpdate(now: Long, onResult: ((Boolean) -> Unit)?) {
        val updateResult = suspendCancellableCoroutine { continuation ->
            BazaarUpdater.getLastUpdateState(context) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
        }

        if (updateResult is UpdateResult.NeedUpdate) {
            val targetVersion = updateResult.getTargetVersionCode().toInt()
            val storeInfo = AppVersionDto(
                latestVersionCode = targetVersion,
                latestVersionName = "",
                releaseNotes = "نسخه جدید در کافه‌بازار موجود است.",
                downloadUrl = "",
                isForced = false
            )
            updateCacheManager.saveUpdateInfo(storeInfo, now)
            _updateState.value = UpdateState.UpdateAvailable(storeInfo, isForced = false)
            _isUpdateIgnored.value = updateCacheManager.getIgnoredVersion() == targetVersion
            onResult?.invoke(true)
        } else {
            _updateState.value = UpdateState.UpToDate
            onResult?.invoke(false)
        }
    }

    private fun triggerMyketInAppUpdate() {
        val targetPackage = context.packageName.removeSuffix(".debug")
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = "myket://check-update?id=$targetPackage".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback to website if Myket isn't installed
            val webIntent = Intent(Intent.ACTION_VIEW, "https://mehr-banan.ir/download".toUri())
            webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(webIntent)
        }
    }

    fun startDownload(cachedUrl: String, cachedVersionName: String) {
        @Suppress(
            "SimplifyBooleanWithConstants", "KotlinConstantConditions"
        ) if (BuildConfig.INSTALL_SOURCE == "bazaar") {
            try {
                BazaarUpdater.updateApplication(context)
                _updateState.value = UpdateState.UpToDate
            } catch (_: Exception) {
            }
            return
        }

        @Suppress("SimplifyBooleanWithConstants") if (BuildConfig.INSTALL_SOURCE == "myket") {
            triggerMyketInAppUpdate()
            return
        }

        _appDownloadState.value = AppDownloadState.Connecting
        scope.launch {
            try {
                val response = appUpdateApi.getAppVersion(BuildConfig.VERSION_CODE)
                val finalUrl: String
                val finalVersionName: String

                if (response.isSuccessful && response.body() != null) {
                    val serverInfo = response.body()!!
                    if (serverInfo.latestVersionCode <= BuildConfig.VERSION_CODE) {
                        _appDownloadState.value =
                            AppDownloadState.Error(DownloadError.ALREADY_LATEST)
                        _updateState.value = UpdateState.UpToDate
                        return@launch
                    }
                    finalUrl = serverInfo.downloadUrl
                    finalVersionName = serverInfo.latestVersionName
                } else {
                    finalUrl = cachedUrl
                    finalVersionName = cachedVersionName
                }

                val currentLang = sessionManager.appLanguageFlow.first()
                val fileName = "mehrban-update-v$finalVersionName.apk"
                val downloadRequest =
                    OneTimeWorkRequestBuilder<UpdateDownloadWorker>().setInputData(
                        workDataOf("url" to finalUrl, "fileName" to fileName, "lang" to currentLang)
                    ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

                workManager.enqueueUniqueWork(
                    "app_update_download", ExistingWorkPolicy.REPLACE, downloadRequest
                )
            } catch (_: Exception) {
                val fileName = "mehrban-update-v$cachedVersionName.apk"
                val downloadRequest =
                    OneTimeWorkRequestBuilder<UpdateDownloadWorker>().setInputData(
                        workDataOf("url" to cachedUrl, "fileName" to fileName)
                    ).setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST).build()

                workManager.enqueueUniqueWork(
                    "app_update_download", ExistingWorkPolicy.REPLACE, downloadRequest
                )
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
                val cachedInfo = updateCacheManager.getCachedUpdateInfo()
                val isAlreadyUpdated =
                    cachedInfo != null && BuildConfig.VERSION_CODE >= cachedInfo.latestVersionCode

                if (isAlreadyUpdated && workInfo.state.isFinished) {
                    workManager.pruneWork()
                    return@collect
                }

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> _appDownloadState.value = AppDownloadState.Connecting
                    WorkInfo.State.RUNNING -> {
                        val progress = workInfo.progress.getInt("progress", -1)
                        if (progress >= 0) _appDownloadState.value =
                            AppDownloadState.Downloading(progress)
                    }

                    WorkInfo.State.SUCCEEDED -> {
                        val path = workInfo.outputData.getString("filePath") ?: ""
                        _appDownloadState.value = AppDownloadState.Success(path)
                        _downloadedFilePath.value = path
                    }

                    WorkInfo.State.FAILED -> {
                        val errorString = workInfo.outputData.getString("error")
                        val finalError = try {
                            if (errorString != null) DownloadError.valueOf(errorString) else DownloadError.GENERIC_ERROR
                        } catch (_: Exception) {
                            DownloadError.GENERIC_ERROR
                        }
                        _appDownloadState.value = AppDownloadState.Error(finalError)
                    }

                    else -> {}
                }
            }
        }
    }
}