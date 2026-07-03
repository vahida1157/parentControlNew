package com.vahak.mehrban.presentation.password

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.domain.usecase.SecuritySetupError
import com.vahak.mehrban.domain.usecase.SecuritySetupException
import com.vahak.mehrban.domain.usecase.SetupSecurityUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

data class PasswordStateV2(
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val selectedQuestion: String = "",
    val securityAnswer: String = "",
    val isLoading: Boolean = false,
    val errorType: SecuritySetupError? = null, // 🚀 Pure Enum
    val isGenericError: Boolean = false        // 🚀 Fallback error boolean
) {
    val passwordStrength: PasswordStrength
        get() = when {
            passwordInput.isEmpty() -> PasswordStrength.NONE
            passwordInput.length < 4 -> PasswordStrength.WEAK
            passwordInput.length < 6 -> PasswordStrength.MEDIUM
            else -> PasswordStrength.STRONG
        }

    val passwordsMatch: Boolean
        get() = passwordInput.isNotEmpty() && passwordInput == confirmPasswordInput

    val isFormValid: Boolean
        get() = passwordsMatch && securityAnswer.trim().length >= 2 && passwordInput.length >= 4
}

enum class PasswordStrength { NONE, WEAK, MEDIUM, STRONG }

sealed class PasswordEventV2 {
    data class PasswordChanged(val value: String) : PasswordEventV2()
    data class ConfirmPasswordChanged(val value: String) : PasswordEventV2()
    object TogglePasswordVisibility : PasswordEventV2()
    object ToggleConfirmPasswordVisibility : PasswordEventV2()
    data class QuestionSelected(val question: String) : PasswordEventV2()
    data class AnswerChanged(val answer: String) : PasswordEventV2()
    object SubmitClicked : PasswordEventV2()
    object BackClicked : PasswordEventV2()
}

sealed class PasswordEffectV2 {
    object NavigateBack : PasswordEffectV2()
    object NavigateToDashboard : PasswordEffectV2()
    object ShowSuccessToast : PasswordEffectV2()
}

@HiltViewModel
class PasswordViewModelV2 @Inject constructor(
    private val sessionManager: SessionManager,
    private val setupSecurityUseCase: SetupSecurityUseCase,
    private val analytics: AppAnalytics,
) : BaseViewModel<PasswordStateV2, PasswordEventV2, PasswordEffectV2>(PasswordStateV2()) {

    override fun onEvent(event: PasswordEventV2) {
        when (event) {
            is PasswordEventV2.PasswordChanged -> updateState {
                copy(passwordInput = event.value, errorType = null, isGenericError = false)
            }

            is PasswordEventV2.ConfirmPasswordChanged -> updateState {
                copy(confirmPasswordInput = event.value, errorType = null, isGenericError = false)
            }

            PasswordEventV2.TogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            PasswordEventV2.ToggleConfirmPasswordVisibility -> updateState {
                copy(isConfirmPasswordVisible = !isConfirmPasswordVisible)
            }

            is PasswordEventV2.QuestionSelected -> updateState { copy(selectedQuestion = event.question) }
            is PasswordEventV2.AnswerChanged -> updateState {
                copy(securityAnswer = event.answer, errorType = null, isGenericError = false)
            }

            PasswordEventV2.SubmitClicked -> submitForm()
            PasswordEventV2.BackClicked -> sendEffect(PasswordEffectV2.NavigateBack)
        }
    }

    private fun submitForm() {
        val currentState = state.value
        if (!currentState.isFormValid) {
            Timber.d("Security profile setup rejected locally due to form validation")
            return
        }

        updateState { copy(isLoading = true, errorType = null, isGenericError = false) }

        viewModelScope.launch {
            Timber.d("Initiating security profile setup submission")
            val result = setupSecurityUseCase.execute(
                pin = currentState.passwordInput,
                question = currentState.selectedQuestion,
                answer = currentState.securityAnswer.trim()
            )

            result.onSuccess {
                withContext(Dispatchers.IO) {
                    sessionManager.setSecurityData(
                        currentState.selectedQuestion, currentState.securityAnswer.trim()
                    )
                    sessionManager.setParentPin(currentState.passwordInput)
                }

                sessionManager.parentPinFlow.first { emittedPin -> emittedPin == currentState.passwordInput }

                Timber.i("Security profile configured successfully")
                analytics.logSecuritySetupCompleted()
                sendEffect(PasswordEffectV2.ShowSuccessToast)
                sendEffect(PasswordEffectV2.NavigateToDashboard)

            }.onFailure { error ->
                Timber.w("Security profile setup failed during server synchronization")
                updateState {
                    copy(
                        isLoading = false,
                        // 🚀 Safely unbox the custom exception or flag a generic error
                        errorType = (error as? SecuritySetupException)?.error,
                        isGenericError = error !is SecuritySetupException
                    )
                }
            }
        }
    }
}