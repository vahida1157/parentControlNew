package com.vahak.mehrban

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.util.AppUpdateManager
import com.vahak.mehrban.data.remote.AppVersionDto
import com.vahak.mehrban.data.remote.DownloadError
import com.vahak.mehrban.presentation.BaseViewModel
import com.vahak.mehrban.uiv2.navigation.Screen
import com.vahak.mehrban.uiv2.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
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

data class MainState(
    val isInitializing: Boolean = true, // Replaces startDestination == null check
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: String = "fa",
    val startDestination: String = "",
    val activeChildId: String? = null,
    val pendingRoute: String? = null,

    // Update Manager States
    val updateState: UpdateState = UpdateState.Checking,
    val isUpdateIgnored: Boolean = false,
    val downloadState: AppDownloadState = AppDownloadState.Idle,
    val downloadedFilePath: String? = null
)

sealed class MainEvent {
    object ClearActiveLauncherSession : MainEvent()
    data class SetPendingRoute(val route: String) : MainEvent()
    object ConsumePendingRoute : MainEvent()

    object DismissOptionalUpdate : MainEvent()
    object ShowUpdateDialogAgain : MainEvent()
    data class StartDownload(val url: String, val versionName: String) : MainEvent()
    object ClearDownloadedFilePath : MainEvent()
}

sealed class MainEffect {
    // Left empty for now, as navigation is state-driven via pendingRoute
}

// --- 🚀 VIEW MODEL ---

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: SessionManager, private val appUpdateManager: AppUpdateManager
) : BaseViewModel<MainState, MainEvent, MainEffect>(MainState()) {

    init {
        // 1. Combine all Session preferences into a single state update
        viewModelScope.launch {
            combine(
                sessionManager.appThemeFlow,
                sessionManager.appLanguageFlow,
                sessionManager.isLoggedIn,
                sessionManager.activeChildIdFlow
            ) { theme, language, isLoggedIn, childId ->
                updateState {
                    copy(
                        isInitializing = false,
                        theme = theme,
                        language = language,
                        startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route,
                        activeChildId = childId
                    )
                }
            }.collectLatest { }
        }

        // 2. Map AppUpdateManager states seamlessly into our unified MainState
        viewModelScope.launch {
            appUpdateManager.updateState.collect { s ->
                updateState {
                    copy(
                        updateState = s
                    )
                }
            }
        }
        viewModelScope.launch {
            appUpdateManager.isUpdateIgnored.collect { s ->
                updateState {
                    copy(
                        isUpdateIgnored = s
                    )
                }
            }
        }
        viewModelScope.launch {
            appUpdateManager.appDownloadState.collect { s ->
                updateState {
                    copy(
                        downloadState = s
                    )
                }
            }
        }
        viewModelScope.launch {
            appUpdateManager.downloadedFilePath.collect { s ->
                updateState {
                    copy(
                        downloadedFilePath = s
                    )
                }
            }
        }
    }

    override fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.ClearActiveLauncherSession -> viewModelScope.launch { sessionManager.clearActiveChildId() }
            is MainEvent.SetPendingRoute -> updateState { copy(pendingRoute = event.route) }
            is MainEvent.ConsumePendingRoute -> updateState { copy(pendingRoute = null) }

            is MainEvent.DismissOptionalUpdate -> appUpdateManager.dismissOptionalUpdate()
            is MainEvent.ShowUpdateDialogAgain -> appUpdateManager.unignoreUpdate()
            is MainEvent.StartDownload -> appUpdateManager.startDownload(
                event.url, event.versionName
            )

            is MainEvent.ClearDownloadedFilePath -> appUpdateManager.clearDownloadedFilePath()
        }
    }
}