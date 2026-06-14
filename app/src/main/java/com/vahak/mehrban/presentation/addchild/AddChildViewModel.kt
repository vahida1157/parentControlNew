package com.vahak.mehrban.presentation.addchild

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.util.JalaliConverter
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.domain.usecase.ChildValidationResult
import com.vahak.mehrban.domain.usecase.ValidateAddChildUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import com.vahak.mehrban.ui.screens.Gender
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.vahak.mehrban.core.data.local.entity.Gender as DbGender

data class AddChildState(
    val name: String = "",
    val phone: String = "",
    val dob: String = "",
    val gender: Gender? = null,
    val avatarId: Int = 1,
    val isAvatarSheetOpen: Boolean = false,
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val isDobSheetOpen: Boolean = false,
)

sealed class AddChildEvent {
    data class NameChanged(val name: String) : AddChildEvent()
    data class PhoneChanged(val phone: String) : AddChildEvent()
    object OpenDobSheet : AddChildEvent()
    object CloseDobSheet : AddChildEvent()
    data class DobSelected(val year: Int, val month: Int, val day: Int) : AddChildEvent()
    data class GenderSelected(val gender: Gender) : AddChildEvent()
    object OpenAvatarSheet : AddChildEvent()
    object CloseAvatarSheet : AddChildEvent()
    data class AvatarSelected(val id: Int) : AddChildEvent()
    object SaveClicked : AddChildEvent()
}

sealed class AddChildEffect {
    object NavigateBack : AddChildEffect()
    data class ShowToast(val message: String) : AddChildEffect()
}

@HiltViewModel
class AddChildViewModel @Inject constructor(
    private val validateUseCase: ValidateAddChildUseCase,
    private val childRepository: ChildRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<AddChildState, AddChildEvent, AddChildEffect>(AddChildState()) {

    override fun onEvent(event: AddChildEvent) {
        when (event) {
            is AddChildEvent.NameChanged -> updateState {
                copy(name = event.name, errorMessage = null)
            }
            is AddChildEvent.PhoneChanged -> updateState { copy(phone = event.phone) }
            is AddChildEvent.OpenDobSheet -> updateState { copy(isDobSheetOpen = true) }
            is AddChildEvent.CloseDobSheet -> updateState { copy(isDobSheetOpen = false) }
            is AddChildEvent.DobSelected -> {
                val formattedMonth = event.month.toString().padStart(2, '0')
                val formattedDay = event.day.toString().padStart(2, '0')
                val formattedDob = "${event.year}/$formattedMonth/$formattedDay"
                updateState {
                    copy(dob = formattedDob, isDobSheetOpen = false, errorMessage = null)
                }
            }
            is AddChildEvent.GenderSelected -> updateState {
                copy(gender = event.gender, errorMessage = null)
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
            updateState { copy(errorMessage = validationResult.message) }
            return
        }

        updateState { copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val dbGender = if (currentState.gender == Gender.Boy) DbGender.BOY else DbGender.GIRL

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
                sendEffect(AddChildEffect.ShowToast(context.getString(R.string.child_added_success)))
                sendEffect(AddChildEffect.NavigateBack)
            }.onFailure { error ->
                updateState {
                    copy(
                        isSaving = false,
                        errorMessage = error.message ?: context.getString(R.string.error_save_child_generic)
                    )
                }
            }
        }
    }
}