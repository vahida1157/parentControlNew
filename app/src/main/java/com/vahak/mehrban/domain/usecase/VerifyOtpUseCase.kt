package com.vahak.mehrban.domain.usecase

class VerifyOtpUseCase {
    fun execute(code: String): OtpValidationResult {
        if (code.length != 4) return OtpValidationResult.Error("کد باید ۴ رقم باشد.")
        // Mocking a backend validation check
        if (code != "1234") return OtpValidationResult.Error("کد وارد شده اشتباه است.")
        return OtpValidationResult.Success(120)
    }
}