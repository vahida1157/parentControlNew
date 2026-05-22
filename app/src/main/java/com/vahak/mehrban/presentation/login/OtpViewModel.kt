package com.vahak.mehrban.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.domain.repository.AuthRepository
import com.vahak.mehrban.domain.usecase.OtpValidationResult
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtpState(
    val otpCode: String = "",
    val errorMessage: String? = null,
    val isVerifying: Boolean = false,
    val timerSeconds: Int = 120,
    val canResend: Boolean = false
)

sealed class OtpEvent {
    data class OtpChanged(val code: String) : OtpEvent()
    data class VerifyClicked(val phone: String) : OtpEvent()
    data class ResendClicked(val phone: String) : OtpEvent()
}

sealed class OtpEffect {
    object NavigateToDashboard : OtpEffect()
    object NavigateToPasswordSetup : OtpEffect()
    data class ShowToast(val message: String) : OtpEffect()
}

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val repository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<OtpState, OtpEvent, OtpEffect>(OtpState()) {

    private var timerJob: Job? = null
    init {
        val expirationString: String? = savedStateHandle["expiresInSeconds"]
        val initialTimer = expirationString?.toIntOrNull() ?: 120
        startTimer(initialTimer)
    }

    fun startTimer(seconds: Int) {
        timerJob?.cancel()
        updateState { copy(timerSeconds = seconds, canResend = false) }

        timerJob = viewModelScope.launch {
            for (i in seconds downTo 0) {
                updateState { copy(timerSeconds = i) }
                delay(1000L)
            }
            updateState { copy(canResend = true) }
        }
    }

    override fun onEvent(event: OtpEvent) {
        when (event) {
            is OtpEvent.OtpChanged -> updateState {
                copy(
                    otpCode = event.code, errorMessage = null
                )
            }

            is OtpEvent.VerifyClicked -> verifyCode(event.phone)
            is OtpEvent.ResendClicked -> resendCode(event.phone)
        }
    }

    private fun verifyCode(phone: String) {
        viewModelScope.launch {
            updateState { copy(isVerifying = true) }

            val result = repository.verifyOtp(phone, state.value.otpCode)

            result.onSuccess { pair ->
                val (_, hasPinSetup) = pair // Extract the flag

                if (hasPinSetup) {
                    sendEffect(OtpEffect.NavigateToDashboard)
                } else {
                    sendEffect(OtpEffect.NavigateToPasswordSetup)
                }
            }.onFailure { error ->
                updateState { copy(errorMessage = error.message, isVerifying = false) }
            }
        }
    }

    private fun resendCode(phone: String) {
        if (!state.value.canResend) return

        viewModelScope.launch {
            val result = repository.requestOtp(phone)

            if (result is OtpValidationResult.Success) {
                sendEffect(OtpEffect.ShowToast("کد جدید با موفقیت ارسال شد"))
                startTimer(result.expiresInSeconds)
            } else if (result is OtpValidationResult.Error) {
                updateState { copy(errorMessage = result.message) }
            }
        }
    }
}