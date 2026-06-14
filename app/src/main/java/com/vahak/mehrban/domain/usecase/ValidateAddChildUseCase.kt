package com.vahak.mehrban.domain.usecase

import android.content.Context
import com.vahak.mehrban.R
import com.vahak.mehrban.ui.screens.Gender
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class ValidateAddChildUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun execute(name: String, dob: String, gender: Gender?): ChildValidationResult {
        if (name.isBlank()) {
            return ChildValidationResult.Error(context.getString(R.string.error_child_name_empty))
        }
        if (dob.isBlank()) {
            return ChildValidationResult.Error(context.getString(R.string.error_child_dob_empty))
        }
        if (gender == null) {
            return ChildValidationResult.Error(context.getString(R.string.error_child_gender_empty))
        }
        return ChildValidationResult.Success
    }
}