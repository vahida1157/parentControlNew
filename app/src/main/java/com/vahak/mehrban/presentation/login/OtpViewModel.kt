package com.vahak.mehrban.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.domain.error.AuthError
import com.vahak.mehrban.domain.error.AuthException
import com.vahak.mehrban.domain.repository.AuthRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class OtpState(
    val otpCode: String = "",
    val authError: AuthError? = null,
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
    object ShowResendSuccessToast : OtpEffect()
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
                copy(otpCode = event.code, authError = null)
            }
            is OtpEvent.VerifyClicked -> verifyCode(event.phone)
            is OtpEvent.ResendClicked -> resendCode(event.phone)
        }
    }

    private fun verifyCode(phone: String) {
        viewModelScope.launch {
            Timber.d("Initiating OTP verification sequence")
            updateState { copy(isVerifying = true, authError = null) }

            repository.verifyOtp(phone, state.value.otpCode).onSuccess { pair ->
                val (_, hasPinSetup) = pair
                if (hasPinSetup) {
                    Timber.i("OTP verified successfully, session restored, routing to dashboard")
                    sendEffect(OtpEffect.NavigateToDashboard)
                } else {
                    Timber.i("OTP verified successfully, new session, routing to security setup")
                    sendEffect(OtpEffect.NavigateToPasswordSetup)
                }
            }.onFailure { error ->
                Timber.w("OTP verification failed due to bad credentials or network error")
                updateState { copy(isVerifying = false) }
                if (error is AuthException) {
                    updateState { copy(authError = error.error) }
                } else {
                    updateState { copy(authError = AuthError.UNKNOWN) }
                }
            }
        }
    }

    private fun resendCode(phone: String) {
        if (!state.value.canResend) return

        viewModelScope.launch {
            Timber.d("Initiating OTP resend request")
            updateState { copy(authError = null) }

            repository.requestOtp(phone).onSuccess { ttl ->
                Timber.i("OTP resent successfully")
                sendEffect(OtpEffect.ShowResendSuccessToast)
                startTimer(ttl)
            }.onFailure { error ->
                Timber.w("OTP resend request failed")
                if (error is AuthException) {
                    updateState { copy(authError = error.error) }
                } else {
                    updateState { copy(authError = AuthError.UNKNOWN) }
                }
            }
        }
    }
}