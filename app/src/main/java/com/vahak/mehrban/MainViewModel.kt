package com.vahak.mehrban

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.util.AppUpdateManager
import com.vahak.mehrban.data.remote.AppVersionDto
import com.vahak.mehrban.data.remote.DownloadError
import com.vahak.mehrban.uiv2.navigation.Screen
import com.vahak.mehrban.uiv2.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
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
    data class Error(val error: DownloadError) : AppDownloadState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    val appTheme = sessionManager.appThemeFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AppTheme.SYSTEM
    )
    val startDestination =
        sessionManager.isLoggedIn.map { if (it) Screen.Dashboard.route else Screen.Login.route }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val activeChildId = sessionManager.activeChildIdFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // 🚀 NEW: Expose the Language State
    val appLanguage = sessionManager.appLanguageFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "fa"
    )

    fun clearActiveLauncherSession() {
        viewModelScope.launch { sessionManager.clearActiveChildId() }
    }

    val updateState = appUpdateManager.updateState
    val isUpdateIgnored = appUpdateManager.isUpdateIgnored
    val appDownloadState = appUpdateManager.appDownloadState
    val downloadedFilePath = appUpdateManager.downloadedFilePath

    fun dismissOptionalUpdate() = appUpdateManager.dismissOptionalUpdate()
    fun showUpdateDialogAgain() = appUpdateManager.unignoreUpdate()
    fun startDownload(url: String, versionName: String) =
        appUpdateManager.startDownload(url, versionName)

    fun clearDownloadedFilePath() = appUpdateManager.clearDownloadedFilePath()
}