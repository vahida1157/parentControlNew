package com.vahak.parentcontroll.presentation.addchild

import com.vahak.parentcontroll.domain.usecase.ValidateAddChildUseCase
import com.vahak.parentcontroll.domain.usecase.ValidationResult
import com.vahak.parentcontroll.presentation.BaseViewModel
import com.vahak.parentcontroll.ui.component.Gender

// 1. Contract Definition
data class AddChildState(
    val name: String = "",
    val dob: String = "",
    val gender: Gender? = null,
    val errorMessage: String? = null,
    val isSaving: Boolean = false
)

sealed class AddChildEvent {
    data class NameChanged(val name: String) : AddChildEvent()
    data class DobChanged(val dob: String) : AddChildEvent()
    data class GenderSelected(val gender: Gender) : AddChildEvent()
    object SaveClicked : AddChildEvent()
}

sealed class AddChildEffect {
    object NavigateBack : AddChildEffect()
    data class ShowToast(val message: String) : AddChildEffect()
}

// 2. ViewModel Implementation
class AddChildViewModel(
    private val validateUseCase: ValidateAddChildUseCase = ValidateAddChildUseCase()
) : BaseViewModel<AddChildState, AddChildEvent, AddChildEffect>(AddChildState()) {

    override fun onEvent(event: AddChildEvent) {
        when (event) {
            is AddChildEvent.NameChanged -> updateState {
                copy(
                    name = event.name,
                    errorMessage = null
                )
            }

            is AddChildEvent.DobChanged -> updateState {
                copy(
                    dob = event.dob,
                    errorMessage = null
                )
            }

            is AddChildEvent.GenderSelected -> updateState {
                copy(
                    gender = event.gender,
                    errorMessage = null
                )
            }

            is AddChildEvent.SaveClicked -> submitData()
        }
    }

    private fun submitData() {
        val currentState = state.value

        val validationResult = validateUseCase.execute(
            name = currentState.name,
            dob = currentState.dob,
            gender = currentState.gender
        )

        when (validationResult) {
            is ValidationResult.Error -> {
                updateState { copy(errorMessage = validationResult.message) }
            }

            is ValidationResult.Success -> {
                updateState { copy(isSaving = true, errorMessage = null) }

                // Mocking a successful API call to your Spring backend
                println("Saving child to Spring Backend: ${currentState.name}")

                // Trigger the one-off effect to navigate back to the Dashboard!
                sendEffect(AddChildEffect.ShowToast("فرزند با موفقیت اضافه شد."))
                sendEffect(AddChildEffect.NavigateBack)
            }
        }
    }
}