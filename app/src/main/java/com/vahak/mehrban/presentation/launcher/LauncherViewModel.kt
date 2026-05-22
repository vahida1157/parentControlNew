package com.vahak.mehrban.presentation.launcher

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.dao.AppRuleDao
import com.vahak.mehrban.core.util.AppInfo
import com.vahak.mehrban.core.util.AppManager
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 1. Contract ---
data class LauncherState(
    val installedApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val showExitDialog: Boolean = false,
    val exitErrorMessage: String? = null
)

sealed class LauncherEvent {
    data class AppClicked(val packageName: String) : LauncherEvent()
    object ExitLauncherClicked : LauncherEvent()
    object DismissExitDialog : LauncherEvent()
    data class SubmitExitPin(val pin: String) : LauncherEvent()
}

sealed class LauncherEffect {
    object RequestExit : LauncherEffect()
}

// --- 2. ViewModel ---
@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRuleDao: AppRuleDao,
    private val sessionManager: SessionManager,
) : BaseViewModel<LauncherState, LauncherEvent, LauncherEffect>(LauncherState()) {

    init {
        observeActiveSession()
    }

    private fun observeActiveSession() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionManager.activeChildIdFlow.distinctUntilChanged().collectLatest { childId ->
                if (childId != null) {
                    loadAppsForChild(childId)
                } else {
                    updateState { copy(installedApps = emptyList(), isLoading = false) }
                }
            }
        }
    }

    private suspend fun loadAppsForChild(childId: String) {
        updateState { copy(isLoading = true) }

        appRuleDao.observeAllowedApps(childId).distinctUntilChanged().collectLatest { rules ->
            val allowedPackages = rules.map { it.packageName }.toSet()
            val specificApps = AppManager.getSpecificApps(context, allowedPackages)

            updateState { copy(installedApps = specificApps, isLoading = false) }
        }
    }

    override fun onEvent(event: LauncherEvent) {
        when (event) {
            is LauncherEvent.AppClicked -> {
                AppManager.launchApp(context, event.packageName)
            }
            is LauncherEvent.ExitLauncherClicked -> {
                updateState { copy(showExitDialog = true, exitErrorMessage = null) }
            }
            is LauncherEvent.DismissExitDialog -> {
                updateState { copy(showExitDialog = false, exitErrorMessage = null) }
            }
            is LauncherEvent.SubmitExitPin -> {
                viewModelScope.launch {
                    val savedPin = sessionManager.parentPinFlow.first()
                    // If no pin is set (edge case), or the pin matches, let them out!
                    if (savedPin.isNullOrEmpty() || event.pin == savedPin) {
                        updateState { copy(showExitDialog = false) }
                        sendEffect(LauncherEffect.RequestExit)
                    } else {
                        updateState { copy(exitErrorMessage = "رمز عبور اشتباه است") }
                    }
                }
            }
        }
    }
}