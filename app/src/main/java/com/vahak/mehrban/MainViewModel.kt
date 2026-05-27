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
import com.vahak.mehrban.core.util.AppUpdateManager
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
    private val sessionManager: SessionManager,
    private val appUpdateManager: AppUpdateManager // 🚀 Inject the Brain
) : ViewModel() {

    // --- Session Flows remain unchanged ---
    val appTheme = sessionManager.appThemeFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppTheme.SYSTEM)
    val startDestination = sessionManager.isLoggedIn.map { if (it) Screen.Dashboard.route else Screen.Login.route }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val activeChildId = sessionManager.activeChildIdFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun clearActiveLauncherSession() {
        viewModelScope.launch { sessionManager.clearActiveChildId() }
    }

    // --- Update Flows just pass through from the Manager ---
    val updateState = appUpdateManager.updateState
    val isUpdateIgnored = appUpdateManager.isUpdateIgnored
    val appDownloadState = appUpdateManager.appDownloadState
    val downloadedFilePath = appUpdateManager.downloadedFilePath

    fun dismissOptionalUpdate() = appUpdateManager.dismissOptionalUpdate()
    fun showUpdateDialogAgain() = appUpdateManager.unignoreUpdate()
    fun startDownload(url: String, versionName: String) = appUpdateManager.startDownload(url, versionName)
    fun clearDownloadedFilePath() = appUpdateManager.clearDownloadedFilePath()
}