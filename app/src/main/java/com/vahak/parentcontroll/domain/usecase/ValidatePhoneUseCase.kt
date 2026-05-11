package com.vahak.parentcontroll.domain.usecase

import javax.inject.Inject

class ValidatePhoneUseCase @Inject constructor() {
    fun execute(phone: String): PhoneValidationResult {
        if (phone.isBlank()) return PhoneValidationResult.Error("شماره تماس نمی‌تواند خالی باشد.")
        if (phone.length != 10 || !phone.startsWith("9")) {
            return PhoneValidationResult.Error("شماره تماس نامعتبر است (مثال: 912 345 6789)")
        }
        return PhoneValidationResult.Success
    }
}