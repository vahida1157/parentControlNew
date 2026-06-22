package com.vahak.mehrban.domain.usecase

sealed interface OtpValidationResult {
    data class Success(val expiresInSeconds: Int) : OtpValidationResult
    data class Error(val message: String) : OtpValidationResult
}