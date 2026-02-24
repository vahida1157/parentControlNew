package com.vahak.parentcontroll.domain.usecase

import com.vahak.parentcontroll.ui.component.Gender
import javax.inject.Inject


class ValidateAddChildUseCase @Inject constructor() {
    fun execute(name: String, dob: String, gender: Gender?): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult.Error("لطفا نام کودک را وارد کنید.")
        }
        if (dob.isBlank()) {
            return ValidationResult.Error("لطفا تاریخ تولد را وارد کنید.")
        }
        if (gender == null) {
            return ValidationResult.Error("لطفا جنسیت کودک را انتخاب کنید.")
        }
        // You can add Regex here later to validate the date format exactly!
        return ValidationResult.Success
    }
}