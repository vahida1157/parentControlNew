package com.vahak.mehrban.presentation.password

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.domain.usecase.SetupSecurityUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class PasswordStateV2(
    val passwordInput: String = "",
    val confirmPasswordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val selectedQuestion: String = "",
    val securityAnswer: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
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
    data class ShowToast(val message: String) : PasswordEffectV2()
}

@HiltViewModel
class PasswordViewModelV2 @Inject constructor(
    private val sessionManager: SessionManager,
    private val setupSecurityUseCase: SetupSecurityUseCase,
    @ApplicationContext private val context: Context
) : BaseViewModel<PasswordStateV2, PasswordEventV2, PasswordEffectV2>(PasswordStateV2()) {

    val questionsList = listOf(
        context.getString(R.string.security_question_1),
        context.getString(R.string.security_question_2),
        context.getString(R.string.security_question_3)
    )

    override fun onEvent(event: PasswordEventV2) {
        when (event) {
            is PasswordEventV2.PasswordChanged -> updateState { copy(passwordInput = event.value, errorMessage = null) }
            is PasswordEventV2.ConfirmPasswordChanged -> updateState { copy(confirmPasswordInput = event.value, errorMessage = null) }
            PasswordEventV2.TogglePasswordVisibility -> updateState { copy(isPasswordVisible = !isPasswordVisible) }
            PasswordEventV2.ToggleConfirmPasswordVisibility -> updateState { copy(isConfirmPasswordVisible = !isConfirmPasswordVisible) }
            is PasswordEventV2.QuestionSelected -> updateState { copy(selectedQuestion = event.question) }
            is PasswordEventV2.AnswerChanged -> updateState { copy(securityAnswer = event.answer, errorMessage = null) }
            PasswordEventV2.SubmitClicked -> submitForm()
            PasswordEventV2.BackClicked -> sendEffect(PasswordEffectV2.NavigateBack)
        }
    }

    private fun submitForm() {
        val currentState = state.value
        if (!currentState.isFormValid) return

        updateState { copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            val result = setupSecurityUseCase.execute(
                pin = currentState.passwordInput,
                question = currentState.selectedQuestion,
                answer = currentState.securityAnswer.trim()
            )

            result.onSuccess {
                withContext(Dispatchers.IO) {
                    sessionManager.setSecurityData(currentState.selectedQuestion, currentState.securityAnswer.trim())
                    sessionManager.setParentPin(currentState.passwordInput)
                }

                sessionManager.parentPinFlow.first { emittedPin -> emittedPin == currentState.passwordInput }

                sendEffect(PasswordEffectV2.ShowToast(context.getString(R.string.password_setup_success)))
                sendEffect(PasswordEffectV2.NavigateToDashboard)

            }.onFailure { error ->
                updateState { copy(isLoading = false, errorMessage = error.message ?: context.getString(R.string.error_unknown)) }
            }
        }
    }
}