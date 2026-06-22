package com.vahak.mehrban.domain.usecase

import com.vahak.mehrban.core.data.local.entity.Gender
import javax.inject.Inject

class ValidateAddChildUseCase @Inject constructor() {

    fun execute(name: String, dob: String, gender: Gender?): ChildValidationResult {
        if (name.isBlank()) {
            return ChildValidationResult.Error(ChildValidationError.NAME_EMPTY)
        }
        if (dob.isBlank()) {
            return ChildValidationResult.Error(ChildValidationError.DOB_EMPTY)
        }
        if (gender == null) {
            return ChildValidationResult.Error(ChildValidationError.GENDER_EMPTY)
        }
        return ChildValidationResult.Success
    }
}

sealed class ChildValidationResult {
    object Success : ChildValidationResult()
    data class Error(val errorType: ChildValidationError) : ChildValidationResult()
}

enum class ChildValidationError {
    NAME_EMPTY, DOB_EMPTY, GENDER_EMPTY
}