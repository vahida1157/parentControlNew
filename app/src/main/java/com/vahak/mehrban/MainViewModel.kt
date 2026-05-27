package com.vahak.mehrban

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.data.remote.AppUpdateApi
import com.vahak.mehrban.data.remote.AppVersionDto
import com.vahak.mehrban.uiv2.navigation.Screen
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.worker.UpdateDownloadWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UpdateState {
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class UpdateAvailable(val info: AppVersionDto, val isForced: Boolean) : UpdateState()
}

sealed class AppDownloadState {
    object Idle : AppDownloadState()
    object Connecting : AppDownloadState()
    data class Downloading(val progress: Int) : AppDownloadState()
    data class Success(val path: String) : AppDownloadState()
    data class Error(val message: String) : AppDownloadState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionManager: SessionManager,
    private val appUpdateApi: AppUpdateApi
) : ViewModel() {

    // --- EXISTING FLOWS ---
    val appTheme: StateFlow<AppTheme> = sessionManager.appThemeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppTheme.SYSTEM
        )

    val activeChildId: StateFlow<String?> = sessionManager.activeChildIdFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val startDestination: StateFlow<String?> = sessionManager.isLoggedIn.map { isLoggedIn ->
        if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // --- NEW: SOLID STATE UPDATE FLOWS ---
    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Checking)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _isUpdateIgnored = MutableStateFlow(false)
    val isUpdateIgnored: StateFlow<Boolean> = _isUpdateIgnored.asStateFlow()

    private val _appDownloadState = MutableStateFlow<AppDownloadState>(AppDownloadState.Idle)
    val appDownloadState: StateFlow<AppDownloadState> = _appDownloadState.asStateFlow()

    private val _downloadedFilePath = MutableStateFlow<String?>(null)
    val downloadedFilePath: StateFlow<String?> = _downloadedFilePath.asStateFlow()

    init {
        checkForUpdates()
    }

    fun clearActiveLauncherSession() {
        viewModelScope.launch {
            sessionManager.clearActiveChildId()
        }
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val response = appUpdateApi.getAppVersion()
                if (response.isSuccessful && response.body() != null) {
                    val serverInfo = response.body()!!
                    val currentVersionCode = BuildConfig.VERSION_CODE

                    if (serverInfo.latestVersionCode > currentVersionCode) {
                        _updateState.value = UpdateState.UpdateAvailable(serverInfo, serverInfo.isForced)
                    } else {
                        _updateState.value = UpdateState.UpToDate
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (_: Exception) {
                _updateState.value = UpdateState.UpToDate
            }
        }
    }

    fun dismissOptionalUpdate() {
        _isUpdateIgnored.value = true
    }

    fun showUpdateDialogAgain() {
        _isUpdateIgnored.value = false
    }

    fun clearDownloadedFilePath() {
        _downloadedFilePath.value = null
        _appDownloadState.value = AppDownloadState.Idle
    }

    fun startDownload(url: String, versionName: String) {
        val fileName = "mehrban-update-v$versionName.apk"
        val inputData = workDataOf("url" to url, "fileName" to fileName)

        val downloadRequest = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            "app_update_download",
            ExistingWorkPolicy.REPLACE,
            downloadRequest
        )

        _appDownloadState.value = AppDownloadState.Connecting

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(downloadRequest.id).collect { workInfo ->
                if (workInfo != null) {
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED -> {
                            _appDownloadState.value = AppDownloadState.Connecting
                        }
                        WorkInfo.State.RUNNING -> {
                            val progress = workInfo.progress.getInt("progress", -1)
                            if (progress >= 0) {
                                _appDownloadState.value = AppDownloadState.Downloading(progress)
                            }
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            val path = workInfo.outputData.getString("filePath") ?: ""
                            _appDownloadState.value = AppDownloadState.Success(path)
                            _downloadedFilePath.value = path
                        }
                        WorkInfo.State.FAILED -> {
                            val errorMsg = workInfo.outputData.getString("error") ?: "خطا در دانلود فایل بروزرسانی"
                            _appDownloadState.value = AppDownloadState.Error(errorMsg)
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}