package com.vahak.mehrban.domain.usecase

import com.vahak.mehrban.domain.repository.ProfileRepository
import javax.inject.Inject

enum class SecuritySetupError {
    PIN_LENGTH,
    ANSWER_SHORT,
    QUESTION_EMPTY
}
class SecuritySetupException(val error: SecuritySetupError) : Exception()

class SetupSecurityUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend fun execute(pin: String, question: String, answer: String): Result<Unit> {
        if (pin.length !in 4..8) return Result.failure(SecuritySetupException(SecuritySetupError.PIN_LENGTH))
        if (answer.trim().length < 2) return Result.failure(SecuritySetupException(SecuritySetupError.ANSWER_SHORT))
        if (question.isBlank()) return Result.failure(SecuritySetupException(SecuritySetupError.QUESTION_EMPTY))

        return repository.setupSecurity(pin, question, answer)
    }
}