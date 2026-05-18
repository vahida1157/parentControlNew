package com.vahak.parentcontroll.presentation.setting

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettingsState(
    val parentPhoneNumber: String = "در حال بارگذاری...",
    val currentTheme: AppTheme = AppTheme.SYSTEM // NEW: Track the theme
)

sealed class AppSettingsEvent {
    object LogoutClicked : AppSettingsEvent()
    data class ThemeSelected(val theme: AppTheme) : AppSettingsEvent() // NEW: Theme Event
}

sealed class AppSettingsEffect {
    object NavigateToLogin : AppSettingsEffect()
}

@HiltViewModel
class ApplicationSettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : BaseViewModel<AppSettingsState, AppSettingsEvent, AppSettingsEffect>(AppSettingsState()) {

    init {
        // Observe Phone Number
        viewModelScope.launch {
            sessionManager.userPhoneFlow.collectLatest { phone ->
                updateState { copy(parentPhoneNumber = phone ?: "شماره نامشخص") }
            }
        }

        // Observe Theme Preference
        viewModelScope.launch {
            sessionManager.appThemeFlow.collectLatest { theme ->
                // Assuming sessionManager.themeModeFlow emits an AppTheme.
                // If it emits a String, map it to AppTheme here.
                updateState { copy(currentTheme = theme) }
            }
        }
    }

    override fun onEvent(event: AppSettingsEvent) {
        when (event) {
            is AppSettingsEvent.ThemeSelected -> {
                viewModelScope.launch {
                    sessionManager.setAppTheme(event.theme)
                }
            }
            is AppSettingsEvent.LogoutClicked -> {
                viewModelScope.launch {
                    authRepository.logout()
                    sendEffect(AppSettingsEffect.NavigateToLogin)
                }
            }
        }
    }
}