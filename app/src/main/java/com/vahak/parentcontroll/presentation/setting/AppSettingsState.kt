package com.vahak.parentcontroll.presentation.setting

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppSettingsState(
    val parentPhoneNumber: String = "در حال بارگذاری..."
)

sealed class AppSettingsEvent {
    object LogoutClicked : AppSettingsEvent()
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
        viewModelScope.launch {
            sessionManager.userPhoneFlow.collectLatest { phone ->
                updateState { copy(parentPhoneNumber = phone ?: "شماره نامشخص") }
            }
        }
    }

    override fun onEvent(event: AppSettingsEvent) {
        when (event) {
            is AppSettingsEvent.LogoutClicked -> {
                viewModelScope.launch {
                    authRepository.logout() // Clears the session securely
                    sendEffect(AppSettingsEffect.NavigateToLogin)
                }
            }
        }
    }
}