package com.vahak.parentcontroll.presentation.login

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.domain.usecase.ValidationResult
import com.vahak.parentcontroll.domain.usecase.VerifyOtpUseCase
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtpState(
    val otpCode: String = "", val errorMessage: String? = null, val isVerifying: Boolean = false
)

sealed class OtpEvent {
    data class OtpChanged(val code: String) : OtpEvent()
    data class VerifyClicked(val phone: String) : OtpEvent()
    object ResendClicked : OtpEvent()
}

sealed class OtpEffect {
    object NavigateToDashboard : OtpEffect()
    data class ShowToast(val message: String) : OtpEffect()
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val repository: AuthRepository
) : BaseViewModel<OtpState, OtpEvent, OtpEffect>(OtpState()) {

    override fun onEvent(event: OtpEvent) {
        when (event) {
            is OtpEvent.OtpChanged -> updateState {
                copy(
                    otpCode = event.code, errorMessage = null
                )
            }

            is OtpEvent.VerifyClicked -> verifyCode(event.phone)
            is OtpEvent.ResendClicked -> resendCode()
        }
    }

    private fun verifyCode(phone: String) {
        viewModelScope.launch {
            updateState { copy(isVerifying = true) }

            val result = repository.verifyOtp(phone, state.value.otpCode)

            result.onSuccess {
                sendEffect(OtpEffect.NavigateToDashboard)
            }.onFailure { error ->
                updateState { copy(errorMessage = error.message, isVerifying = false) }
            }
        }
    }

    private fun resendCode() {
        // Handle timer logic and API call here
        sendEffect(OtpEffect.ShowToast("کد جدید ارسال شد."))
    }
}