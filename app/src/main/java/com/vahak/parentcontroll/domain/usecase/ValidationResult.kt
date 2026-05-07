package com.vahak.parentcontroll.domain.usecase

// 1. For Phone Input
sealed interface PhoneValidationResult {
    object Success : PhoneValidationResult
    data class Error(val message: String) : PhoneValidationResult
}

// 2. For OTP Input (Includes our dynamic timer)
sealed interface OtpValidationResult {
    data class Success(val expiresInSeconds: Int) : OtpValidationResult
    data class Error(val message: String) : OtpValidationResult
}

// 3. For Child Creation
sealed interface ChildValidationResult {
    object Success : ChildValidationResult
    data class Error(val message: String) : ChildValidationResult
}