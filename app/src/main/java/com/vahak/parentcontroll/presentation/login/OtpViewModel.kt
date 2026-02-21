package com.vahak.parentcontroll.presentation.login

import com.vahak.parentcontroll.domain.usecase.ValidationResult
import com.vahak.parentcontroll.domain.usecase.VerifyOtpUseCase
import com.vahak.parentcontroll.presentation.BaseViewModel

data class OtpState(
    val otpCode: String = "", val errorMessage: String? = null, val isVerifying: Boolean = false
)

sealed class OtpEvent {
    data class OtpChanged(val code: String) : OtpEvent()
    object VerifyClicked : OtpEvent()
    object ResendClicked : OtpEvent()
}

sealed class OtpEffect {
    object NavigateToDashboard : OtpEffect()
    data class ShowToast(val message: String) : OtpEffect()
}

class OtpViewModel(
    private val verifyOtpUseCase: VerifyOtpUseCase = VerifyOtpUseCase()
) : BaseViewModel<OtpState, OtpEvent, OtpEffect>(OtpState()) {

    override fun onEvent(event: OtpEvent) {
        when (event) {
            is OtpEvent.OtpChanged -> updateState {
                copy(
                    otpCode = event.code, errorMessage = null
                )
            }

            is OtpEvent.VerifyClicked -> verifyCode()
            is OtpEvent.ResendClicked -> resendCode()
        }
    }

    private fun verifyCode() {
        val result = verifyOtpUseCase.execute(state.value.otpCode)
        when (result) {
            is ValidationResult.Error -> updateState { copy(errorMessage = result.message) }
            is ValidationResult.Success -> {
                // Success! Tell UI to navigate
                sendEffect(OtpEffect.NavigateToDashboard)
            }
        }
    }

    private fun resendCode() {
        // Handle timer logic and API call here
        sendEffect(OtpEffect.ShowToast("کد جدید ارسال شد."))
    }
}