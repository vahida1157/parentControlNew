package com.vahak.mehrban.domain.usecase

import javax.inject.Inject

enum class PhoneValidationError {
    EMPTY,
    INVALID_FORMAT,
    PRIVACY_NOT_ACCEPTED
}

sealed class PhoneValidationResult {
    object Success : PhoneValidationResult()
    data class Error(val error: PhoneValidationError) : PhoneValidationResult()
}

class ValidatePhoneUseCase @Inject constructor() {
    fun execute(phone: String): PhoneValidationResult {
        if (phone.isBlank()) return PhoneValidationResult.Error(PhoneValidationError.EMPTY)
        if (phone.length != 10 || !phone.startsWith("9")) {
            return PhoneValidationResult.Error(PhoneValidationError.INVALID_FORMAT)
        }
        return PhoneValidationResult.Success
    }
}