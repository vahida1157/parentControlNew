package com.vahak.parentcontroll.domain.usecase

class ValidatePhoneUseCase {
    fun execute(phone: String): ValidationResult {
        if (phone.isBlank()) return ValidationResult.Error("شماره تماس نمی‌تواند خالی باشد.")
        if (phone.length != 11 || !phone.startsWith("09")) {
            return ValidationResult.Error("شماره تماس نامعتبر است (مثال: 09123456789)")
        }
        return ValidationResult.Success
    }
}