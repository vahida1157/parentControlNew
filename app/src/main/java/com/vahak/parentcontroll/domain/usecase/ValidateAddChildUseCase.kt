package com.vahak.parentcontroll.domain.usecase

import com.vahak.parentcontroll.ui.screens.Gender
import javax.inject.Inject


class ValidateAddChildUseCase @Inject constructor() {
    fun execute(name: String, dob: String, gender: Gender?): ChildValidationResult {
        if (name.isBlank()) {
            return ChildValidationResult.Error("لطفا نام کودک را وارد کنید.")
        }
        if (dob.isBlank()) {
            return ChildValidationResult.Error("لطفا تاریخ تولد را وارد کنید.")
        }
        if (gender == null) {
            return ChildValidationResult.Error("لطفا جنسیت کودک را انتخاب کنید.")
        }
        // You can add Regex here later to validate the date format exactly!
        return ChildValidationResult.Success
    }
}