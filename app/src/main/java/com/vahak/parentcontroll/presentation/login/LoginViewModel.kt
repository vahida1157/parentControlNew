package com.vahak.parentcontroll.presentation.login

import com.vahak.parentcontroll.domain.usecase.ValidatePhoneUseCase
import com.vahak.parentcontroll.domain.usecase.ValidationResult
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// 1. Contract Definition
data class LoginState(
    val phoneNumber: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed class LoginEvent {
    data class PhoneChanged(val phone: String) : LoginEvent()
    object SubmitClicked : LoginEvent()
}

sealed class LoginEffect {
    data class NavigateToOtp(val phone: String) : LoginEffect()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validatePhoneUseCase: ValidatePhoneUseCase
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(LoginState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.PhoneChanged -> {
                // Ensure only digits and max 11 characters
                if (event.phone.length <= 11 && event.phone.all { it.isDigit() }) {
                    updateState { copy(phoneNumber = event.phone, errorMessage = null) }
                }
            }

            is LoginEvent.SubmitClicked -> submitPhone()
        }
    }

    private fun submitPhone() {
        val currentPhone = state.value.phoneNumber
        val result = validatePhoneUseCase.execute(currentPhone)

        when (result) {
            is ValidationResult.Error -> {
                updateState { copy(errorMessage = result.message) }
            }

            is ValidationResult.Success -> {
                // Future Backend Call: Send OTP API request here.

                // Trigger Navigation Effect!
                sendEffect(LoginEffect.NavigateToOtp(currentPhone))
            }
        }
    }
}