package com.vahak.parentcontroll.presentation.password

import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PasswordState(
    val currentSavedPin: String? = null,
    val step: PasswordStep = PasswordStep.LOADING,
    val enteredPin: String = "",
    val firstPinEntry: String = "",
    val errorMessage: String? = null
)

enum class PasswordStep {
    LOADING, ENTER_CURRENT, // Changing existing PIN
    ENTER_NEW,     // Setting up first time
    CONFIRM_NEW    // Confirming the new PIN
}

sealed class PasswordEvent {
    data class PinChanged(val pin: String) : PasswordEvent()
    object BackClicked : PasswordEvent()
}

sealed class PasswordEffect {
    object NavigateBack : PasswordEffect()
    data class ShowToast(val message: String) : PasswordEffect()
}

@HiltViewModel
class PasswordManagementViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : BaseViewModel<PasswordState, PasswordEvent, PasswordEffect>(PasswordState()) {

    init {
        viewModelScope.launch {
            val savedPin = sessionManager.parentPinFlow.first()
            updateState {
                copy(
                    currentSavedPin = savedPin,
                    step = if (savedPin == null) PasswordStep.ENTER_NEW else PasswordStep.ENTER_CURRENT
                )
            }
        }
    }

    override fun onEvent(event: PasswordEvent) {
        when (event) {
            is PasswordEvent.BackClicked -> sendEffect(PasswordEffect.NavigateBack)
            is PasswordEvent.PinChanged -> handlePinEntry(event.pin)
        }
    }

    private fun handlePinEntry(newPin: String) {
        updateState { copy(enteredPin = newPin, errorMessage = null) }

        if (newPin.length == 4) {
            when (state.value.step) {
                PasswordStep.ENTER_CURRENT -> {
                    if (newPin == state.value.currentSavedPin) {
                        updateState { copy(step = PasswordStep.ENTER_NEW, enteredPin = "") }
                    } else {
                        updateState { copy(errorMessage = "رمز عبور اشتباه است", enteredPin = "") }
                    }
                }

                PasswordStep.ENTER_NEW -> {
                    updateState {
                        copy(
                            step = PasswordStep.CONFIRM_NEW, firstPinEntry = newPin, enteredPin = ""
                        )
                    }
                }

                PasswordStep.CONFIRM_NEW -> {
                    if (newPin == state.value.firstPinEntry) {
                        savePinAndExit(newPin)
                    } else {
                        updateState {
                            copy(
                                errorMessage = "رمز عبور مطابقت ندارد. دوباره تلاش کنید.",
                                step = PasswordStep.ENTER_NEW,
                                enteredPin = "",
                                firstPinEntry = ""
                            )
                        }
                    }
                }

                else -> {}
            }
        }
    }

    private fun savePinAndExit(pin: String) {
        viewModelScope.launch {
            sessionManager.setParentPin(pin)
            sendEffect(PasswordEffect.ShowToast("رمز عبور با موفقیت ثبت شد!"))
            sendEffect(PasswordEffect.NavigateBack)
        }
    }
}