package com.vahak.parentcontroll.presentation.password

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class PasswordStep {
    LOADING,
    SETUP_QUESTION, // First time: Set security question
    SETUP_PIN,      // First time: Set PIN
    ENTER_CURRENT,  // Changing PIN: Enter old PIN
    ENTER_NEW,      // Changing PIN: Enter new PIN
    RECOVER         // Forgot PIN: Answer question
}

data class PasswordState(
    val step: PasswordStep = PasswordStep.LOADING,
    val enteredPin: String = "",
    val isError: Boolean = false,
    val errorMessage: String? = null,

    // Security Question Data
    val selectedQuestion: String = "نام اولین معلم شما چیست؟",
    val securityAnswer: String = "",
    val savedQuestion: String? = null
)

sealed class PasswordEvent {
    // Numpad Events
    data class NumberClicked(val num: String) : PasswordEvent()
    object BackspaceClicked : PasswordEvent()
    object ClearClicked : PasswordEvent()

    // Form Events
    data class QuestionSelected(val question: String) : PasswordEvent()
    data class AnswerChanged(val answer: String) : PasswordEvent()
    object SubmitSetupQuestion : PasswordEvent()
    object SubmitRecoveryAnswer : PasswordEvent()

    // Navigation
    object ForgotPinClicked : PasswordEvent()
    object BackClicked : PasswordEvent()
}

sealed class PasswordEffect {
    object NavigateBack : PasswordEffect()
    object NavigateToDashboard : PasswordEffect()
    data class ShowToast(val message: String) : PasswordEffect()
}

@HiltViewModel
class PasswordManagementViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : BaseViewModel<PasswordState, PasswordEvent, PasswordEffect>(PasswordState()) {

    val questionsList = listOf(
        "نام اولین معلم شما چیست؟",
        "نام حیوان خانگی مورد علاقه شما در کودکی؟",
        "نام شهر محل تولد مادرتان چیست؟"
    )

    init {
        viewModelScope.launch {
            val savedPin = sessionManager.parentPinFlow.first()
            val savedQ = sessionManager.securityQuestionFlow.first()

            updateState {
                copy(
                    step = if (savedPin.isNullOrEmpty()) PasswordStep.SETUP_QUESTION else PasswordStep.ENTER_CURRENT,
                    savedQuestion = savedQ
                )
            }
        }
    }

    override fun onEvent(event: PasswordEvent) {
        when (event) {
            is PasswordEvent.BackClicked -> sendEffect(PasswordEffect.NavigateBack)

            // Text Inputs
            is PasswordEvent.QuestionSelected -> updateState { copy(selectedQuestion = event.question) }
            is PasswordEvent.AnswerChanged -> updateState {
                copy(
                    securityAnswer = event.answer,
                    errorMessage = null
                )
            }

            // Button Submissions
            is PasswordEvent.SubmitSetupQuestion -> handleSetupQuestion()
            is PasswordEvent.SubmitRecoveryAnswer -> handleRecovery()
            is PasswordEvent.ForgotPinClicked -> updateState {
                copy(
                    step = PasswordStep.RECOVER,
                    securityAnswer = "",
                    errorMessage = null
                )
            }

            // Numpad
            is PasswordEvent.NumberClicked -> handlePinInput(event.num)
            is PasswordEvent.BackspaceClicked -> {
                if (state.value.enteredPin.isNotEmpty() && !state.value.isError) {
                    updateState { copy(enteredPin = enteredPin.dropLast(1)) }
                }
            }

            is PasswordEvent.ClearClicked -> updateState { copy(enteredPin = "", isError = false) }
        }
    }

    private fun handleSetupQuestion() {
        if (state.value.securityAnswer.trim().length < 2) {
            updateState { copy(errorMessage = "لطفا یک پاسخ معتبر وارد کنید.") }
            return
        }
        viewModelScope.launch {
            sessionManager.setSecurityData(state.value.selectedQuestion, state.value.securityAnswer)
            updateState {
                copy(
                    step = PasswordStep.SETUP_PIN,
                    enteredPin = "",
                    errorMessage = null
                )
            }
        }
    }

    private fun handleRecovery() {
        viewModelScope.launch {
            val correctAns = sessionManager.securityAnswerFlow.first()
            if (state.value.securityAnswer.trim() == correctAns) {
                sendEffect(PasswordEffect.ShowToast("پاسخ صحیح بود. رمز جدید تنظیم کنید."))
                updateState {
                    copy(
                        step = PasswordStep.SETUP_PIN,
                        enteredPin = "",
                        securityAnswer = "",
                        errorMessage = null
                    )
                }
            } else {
                updateState { copy(errorMessage = "پاسخ اشتباه است. دوباره تلاش کنید.") }
            }
        }
    }

    private fun handlePinInput(num: String) {
        val current = state.value.enteredPin
        if (current.length >= 5 || state.value.isError) return

        val newPin = current + num
        updateState { copy(enteredPin = newPin) }

        if (newPin.length == 5) {
            processCompletedPin(newPin)
        }
    }

    private fun processCompletedPin(pin: String) {
        viewModelScope.launch {
            val savedPin = sessionManager.parentPinFlow.first()

            when (state.value.step) {
                PasswordStep.SETUP_PIN -> {
                    withContext(Dispatchers.IO) {
                        sessionManager.setParentPin(pin)
                    }
                    sessionManager.parentPinFlow.first { emittedPin -> emittedPin == pin }
                    sendEffect(PasswordEffect.ShowToast("رمز عبور با موفقیت ذخیره شد!"))
                    sendEffect(PasswordEffect.NavigateToDashboard)
                }

                PasswordStep.ENTER_CURRENT -> {
                    if (pin == savedPin) {
                        updateState { copy(step = PasswordStep.ENTER_NEW, enteredPin = "") }
                    } else {
                        triggerErrorState("رمز فعلی اشتباه است!")
                    }
                }
                PasswordStep.ENTER_NEW -> {
                    withContext(Dispatchers.IO) {
                        sessionManager.setParentPin(pin)
                    }
                    sessionManager.parentPinFlow.first { emittedPin -> emittedPin == pin }
                    sendEffect(PasswordEffect.ShowToast("رمز عبور تغییر یافت!"))
                    sendEffect(PasswordEffect.NavigateBack)
                }
                else -> {}
            }
        }
    }

    private fun triggerErrorState(msg: String) {
        updateState { copy(isError = true, errorMessage = msg) }
        viewModelScope.launch {
            delay(500) // Keep error state for half a second (dots turn red)
            updateState { copy(enteredPin = "", isError = false, errorMessage = null) }
        }
    }
}