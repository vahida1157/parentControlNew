package com.vahak.parentcontroll.presentation.login

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.domain.usecase.OtpValidationResult
import com.vahak.parentcontroll.domain.usecase.PhoneValidationResult
import com.vahak.parentcontroll.domain.usecase.ValidatePhoneUseCase
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Contract Definition
data class LoginState(
    val phoneNumber: String = "", val isLoading: Boolean = false, val errorMessage: String? = null
)

sealed class LoginEvent {
    data class PhoneChanged(val phone: String) : LoginEvent()
    object SubmitClicked : LoginEvent()
}

sealed class LoginEffect {
    data class NavigateToOtp(val phone: String, val ttl: Int) : LoginEffect()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val validatePhoneUseCase: ValidatePhoneUseCase,
    private val authRepository: AuthRepository // INJECT THE REPOSITORY
) : BaseViewModel<LoginState, LoginEvent, LoginEffect>(LoginState()) {

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.PhoneChanged -> {
                if (event.phone.length <= 10 && event.phone.all { it.isDigit() }) {
                    updateState { copy(phoneNumber = event.phone, errorMessage = null) }
                }
            }

            is LoginEvent.SubmitClicked -> submitPhone()
        }
    }

    private fun submitPhone() {
        val currentPhone = state.value.phoneNumber
        when (val validationResult = validatePhoneUseCase.execute(currentPhone)) {
            is PhoneValidationResult.Error -> {
                updateState { copy(errorMessage = validationResult.message) }
            }

            is PhoneValidationResult.Success -> {
                // Call the Spring Boot Backend
                viewModelScope.launch {
                    updateState { copy(isLoading = true, errorMessage = null) }

                    when (val result = authRepository.requestOtp(currentPhone)) {
                        is OtpValidationResult.Success -> {
                            updateState { copy(isLoading = false) }
                            sendEffect(
                                LoginEffect.NavigateToOtp(
                                    currentPhone, result.expiresInSeconds
                                )
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