package com.vahak.mehrban.presentation.addchild

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.util.JalaliConverter
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.domain.usecase.ChildValidationError
import com.vahak.mehrban.domain.usecase.ChildValidationResult
import com.vahak.mehrban.domain.usecase.ValidateAddChildUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import com.vahak.mehrban.core.data.local.entity.Gender as DbGender

// 🚀 Created an Enum for repository/saving errors
enum class AddChildSubmitError {
    GENERIC_ERROR
}

data class AddChildState(
    val name: String = "",
    val phone: String = "",
    val dob: String = "",
    val gender: DbGender? = null,
    val avatarId: Int = 1,
    val isAvatarSheetOpen: Boolean = false,
    val errorType: ChildValidationError? = null,
    val submitError: AddChildSubmitError? = null, // 🚀 Replaced hardcoded string with Enum
    val isSaving: Boolean = false,
    val isDobSheetOpen: Boolean = false,
)

sealed class AddChildEvent {
    data class NameChanged(val name: String) : AddChildEvent()
    data class PhoneChanged(val phone: String) : AddChildEvent()
    object OpenDobSheet : AddChildEvent()
    object CloseDobSheet : AddChildEvent()
    data class DobSelected(val year: Int, val month: Int, val day: Int) : AddChildEvent()
    data class GenderSelected(val gender: DbGender) : AddChildEvent()
    object OpenAvatarSheet : AddChildEvent()
    object CloseAvatarSheet : AddChildEvent()
    data class AvatarSelected(val id: Int) : AddChildEvent()
    object SaveClicked : AddChildEvent()
}

sealed class AddChildEffect {
    object NavigateBack : AddChildEffect()
    object ShowSuccessToast : AddChildEffect() // 🚀 Removed string parameter
}

@HiltViewModel
class AddChildViewModel @Inject constructor(
    private val validateUseCase: ValidateAddChildUseCase,
    private val childRepository: ChildRepository
) : BaseViewModel<AddChildState, AddChildEvent, AddChildEffect>(AddChildState()) {

    override fun onEvent(event: AddChildEvent) {
        when (event) {
            is AddChildEvent.NameChanged -> updateState {
                copy(name = event.name, errorType = null, submitError = null)
            }

            is AddChildEvent.PhoneChanged -> updateState { copy(phone = event.phone) }
            is AddChildEvent.OpenDobSheet -> updateState { copy(isDobSheetOpen = true) }
            is AddChildEvent.CloseDobSheet -> updateState { copy(isDobSheetOpen = false) }
            is AddChildEvent.DobSelected -> {
                val formattedMonth = event.month.toString().padStart(2, '0')
                val formattedDay = event.day.toString().padStart(2, '0')
                val formattedDob = "${event.year}/$formattedMonth/$formattedDay"
                updateState {
                    copy(
                        dob = formattedDob,
                        isDobSheetOpen = false,
                        errorType = null,
                        submitError = null
                    )
                }
            }

            is AddChildEvent.GenderSelected -> updateState {
                copy(gender = event.gender, errorType = null, submitError = null)
            }

            is AddChildEvent.OpenAvatarSheet -> updateState { copy(isAvatarSheetOpen = true) }
            is AddChildEvent.CloseAvatarSheet -> updateState { copy(isAvatarSheetOpen = false) }
            is AddChildEvent.AvatarSelected -> updateState {
                copy(avatarId = event.id, isAvatarSheetOpen = false)
            }

            is AddChildEvent.SaveClicked -> submitData()
        }
    }

    private fun submitData() {
        val currentState = state.value

        val validationResult = validateUseCase.execute(
            name = currentState.name, dob = currentState.dob, gender = currentState.gender
        )

        if (validationResult is ChildValidationResult.Error) {
            Timber.w("Child profile validation rejected, reason: %s", validationResult.errorType)
            updateState { copy(errorType = validationResult.errorType) }
            return
        }

        updateState { copy(isSaving = true, errorType = null, submitError = null) }

        viewModelScope.launch {
            try {
                Timber.d("Processing child profile creation payload")
                val dbGender =
                    if (currentState.gender == DbGender.BOY) DbGender.BOY else DbGender.GIRL

                val dobParts = currentState.dob.split("/")
                val jy = dobParts[0].toInt()
                val jm = dobParts[1].toInt()
                val jd = dobParts[2].toInt()

                val gregorianDob = JalaliConverter.jalaliToGregorian(jy, jm, jd)
                val finalPhone = currentState.phone.trim().takeIf { it.isNotEmpty() }

                val result = childRepository.createChild(
                    name = currentState.name,
                    dob = gregorianDob,
                    gender = dbGender,
                    avatarId = currentState.avatarId,
                    phone = finalPhone
                )

                result.onSuccess {
                    Timber.i("Child profile created and saved locally")
                    sendEffect(AddChildEffect.ShowSuccessToast)
                    sendEffect(AddChildEffect.NavigateBack)
                }.onFailure { error ->
                    Timber.w(error, "Failed to create child profile during repository operation")
                    updateState {
                        copy(
                            isSaving = false,
                            submitError = AddChildSubmitError.GENERIC_ERROR
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "System failure processing date conversion or child payload")
                updateState {
                    copy(
                        isSaving = false,
                        submitError = AddChildSubmitError.GENERIC_ERROR
                    )
                }
            }
        }
    }
}