package com.vahak.mehrban.presentation.setting

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.util.AppUpdateManager
import com.vahak.mehrban.domain.repository.AuthRepository
import com.vahak.mehrban.presentation.BaseViewModel
import com.vahak.mehrban.uiv2.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class AppSettingsState(
    val parentPhoneNumber: String? = null,
    val isPhoneLoaded: Boolean = false,
    val currentTheme: AppTheme = AppTheme.SYSTEM,
    val currentLanguage: String = "fa",
    val showPrivacyDialog: Boolean = false
)

sealed class AppSettingsEvent {
    object LogoutClicked : AppSettingsEvent()
    data class ThemeSelected(val theme: AppTheme) : AppSettingsEvent()
    data class LanguageSelected(val langCode: String) : AppSettingsEvent()
    data class ShowPrivacyDialog(val show: Boolean) : AppSettingsEvent()
}

sealed class AppSettingsEffect {
    object NavigateToLogin : AppSettingsEffect()
    object ShowUpToDateToast : AppSettingsEffect() // 🚀 Specific UI trigger, no strings
}

@HiltViewModel
class ApplicationSettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
    private val appUpdateManager: AppUpdateManager
    // 🚀 Context completely removed!
) : BaseViewModel<AppSettingsState, AppSettingsEvent, AppSettingsEffect>(AppSettingsState()) {

    val updateState = appUpdateManager.updateState

    init {
        viewModelScope.launch {
            sessionManager.userPhoneFlow.collectLatest { phone ->
                // 🚀 UI can now easily decide what to show based on null/empty
                updateState { copy(parentPhoneNumber = phone, isPhoneLoaded = true) }
            }
        }

        viewModelScope.launch {
            sessionManager.appThemeFlow.collectLatest { theme ->
                updateState { copy(currentTheme = theme) }
            }
        }

        viewModelScope.launch {
            sessionManager.appLanguageFlow.collectLatest { lang ->
                updateState { copy(currentLanguage = lang) }
            }
        }
    }

    override fun onEvent(event: AppSettingsEvent) {
        when (event) {
            is AppSettingsEvent.ThemeSelected -> {
                Timber.i("Application theme preference updated locally: %s", event.theme)
                viewModelScope.launch { sessionManager.setAppTheme(event.theme) }
            }
            is AppSettingsEvent.LanguageSelected -> {
                Timber.i("Application language preference updated locally: %s", event.langCode)
                viewModelScope.launch { sessionManager.setAppLanguage(event.langCode) }
            }
            is AppSettingsEvent.ShowPrivacyDialog -> {
                updateState { copy(showPrivacyDialog = event.show) }
            }
            is AppSettingsEvent.LogoutClicked -> {
                Timber.i("Initiating user session termination via settings panel")
                viewModelScope.launch {
                    authRepository.logout()
                    sendEffect(AppSettingsEffect.NavigateToLogin)
                }
            }
        }
    }

    fun checkForUpdates() {
        Timber.d("Initiating manual application update check")
        appUpdateManager.checkForUpdates(forceNetworkCall = true) { updateFound ->
            if (!updateFound) {
                Timber.i("Application is currently up to date")
                viewModelScope.launch {
                    sendEffect(AppSettingsEffect.ShowUpToDateToast) // 🚀 Clean effect trigger
                }
            } else {
                Timber.i("New application update discovered")
            }
        }
    }

    fun openUpdateDialog() = appUpdateManager.unignoreUpdate()
}