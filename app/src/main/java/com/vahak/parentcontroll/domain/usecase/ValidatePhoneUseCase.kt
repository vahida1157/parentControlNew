package com.vahak.parentcontroll.domain.usecase

import javax.inject.Inject

class ValidatePhoneUseCase @Inject constructor() {
    fun execute(phone: String): PhoneValidationResult {
        if (phone.isBlank()) return PhoneValidationResult.Error("شماره تماس نمی‌تواند خالی باشد.")
        if (phone.length != 11 || !phone.startsWith("09")) {
            return PhoneValidationResult.Error("شماره تماس نامعتبر است (مثال: 09123456789)")
        }
        return PhoneValidationResult.Success
    }
}