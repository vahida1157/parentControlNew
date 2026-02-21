package com.vahak.parentcontroll.domain.usecase

class VerifyOtpUseCase {
    fun execute(code: String): ValidationResult {
        if (code.length != 4) return ValidationResult.Error("کد باید ۴ رقم باشد.")
        // Mocking a backend validation check
        if (code != "1234") return ValidationResult.Error("کد وارد شده اشتباه است.") 
        return ValidationResult.Success
    }
}