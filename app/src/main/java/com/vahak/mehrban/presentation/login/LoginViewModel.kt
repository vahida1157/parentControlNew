package com.vahak.mehrban.presentation.login

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.domain.repository.AuthRepository
import com.vahak.mehrban.domain.usecase.OtpValidationResult
import com.vahak.mehrban.domain.usecase.PhoneValidationResult
import com.vahak.mehrban.domain.usecase.ValidatePhoneUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Contract Definition
data class LoginState(
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPrivacyAccepted: Boolean = false,
    val showPrivacyDialog: Boolean = false,
)

sealed class LoginEvent {
    data class PhoneChanged(val phone: String) : LoginEvent()
    data class PrivacyAcceptedChanged(val isAccepted: Boolean) : LoginEvent()
    data class ShowPrivacyDialog(val show: Boolean) : LoginEvent()
    object SubmitClicked : LoginEvent()
}

sealed class LoginEffect {
    data class NavigateToOtp(val phone: String, val ttl: Int) : LoginEffect()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validatePhoneUseCase: ValidatePhoneUseCase,
    private val authRepository: AuthRepository
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(LoginState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.PhoneChanged -> {
                if (event.phone.length <= 10 && event.phone.all { it.isDigit() }) {
                    updateState { copy(phoneNumber = event.phone, errorMessage = null) }
                }
            }

            // 🚀 Handle Privacy Events
            is LoginEvent.PrivacyAcceptedChanged -> {
                updateState { copy(isPrivacyAccepted = event.isAccepted) }
            }
            is LoginEvent.ShowPrivacyDialog -> {
                updateState { copy(showPrivacyDialog = event.show) }
            }

            is LoginEvent.SubmitClicked -> {
                // Double check before submitting
                if (state.value.isPrivacyAccepted) {
                    submitPhone()
                } else {
                    updateState { copy(errorMessage = "لطفاً قوانین و حریم خصوصی را تایید کنید.") }
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
                            sendEffect(
                                LoginEffect.NavigateToOtp(currentPhone, result.expiresInSeconds)
                            )
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