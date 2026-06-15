package com.vahak.mehrban.presentation.login

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.SessionManager // 🚀 Inject SessionManager
import com.vahak.mehrban.domain.repository.AuthRepository
import com.vahak.mehrban.domain.usecase.OtpValidationResult
import com.vahak.mehrban.domain.usecase.PhoneValidationResult
import com.vahak.mehrban.domain.usecase.ValidatePhoneUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginState(
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPrivacyAccepted: Boolean = false,
    val showPrivacyDialog: Boolean = false
)

sealed class LoginEvent {
    data class PhoneChanged(val phone: String) : LoginEvent()
    data class PrivacyAcceptedChanged(val isAccepted: Boolean) : LoginEvent()
    data class ShowPrivacyDialog(val show: Boolean) : LoginEvent()
    data class ChangeLanguage(val langCode: String) : LoginEvent() // 🚀 The Language Event
    object SubmitClicked : LoginEvent()
}

sealed class LoginEffect {
    data class NavigateToOtp(val phone: String, val ttl: Int) : LoginEffect()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validatePhoneUseCase: ValidatePhoneUseCase,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager, // 🚀 Added
    @ApplicationContext private val context: Context
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(LoginState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.PhoneChanged -> {
                if (event.phone.length <= 10 && event.phone.all { it.isDigit() }) {
                    updateState { copy(phoneNumber = event.phone, errorMessage = null) }
                }
            }
            is LoginEvent.PrivacyAcceptedChanged -> {
                updateState { copy(isPrivacyAccepted = event.isAccepted) }
            }
            is LoginEvent.ShowPrivacyDialog -> {
                updateState { copy(showPrivacyDialog = event.show) }
            }
            is LoginEvent.ChangeLanguage -> {
                // 🚀 Instantly save to DataStore. MainActivity reads this and changes UI!
                viewModelScope.launch {
                    sessionManager.setAppLanguage(event.langCode)
                }
            }
            is LoginEvent.SubmitClicked -> {
                if (state.value.isPrivacyAccepted) {
                    submitPhone()
                } else {
                    updateState { copy(errorMessage = context.getString(R.string.error_privacy_not_accepted)) }
                }
            }
        }
    }

    private fun submitPhone() {
        val currentPhone = state.value.phoneNumber
        when (val validationResult = validatePhoneUseCase.execute(currentPhone)) {
            is PhoneValidationResult.Error -> {
                updateState { copy(errorMessage = validationResult.message) }
            }
            is PhoneValidationResult.Success -> {
                viewModelScope.launch {
                    updateState { copy(isLoading = true, errorMessage = null) }
                    when (val result = authRepository.requestOtp(currentPhone)) {
                        is OtpValidationResult.Success -> {
                            updateState { copy(isLoading = false) }
                            sendEffect(LoginEffect.NavigateToOtp(currentPhone, result.expiresInSeconds))
                        }
                        is OtpValidationResult.Error -> {
                            updateState { copy(isLoading = false, errorMessage = result.message) }
                        }
                    }
                }
            }
        }
    }
}