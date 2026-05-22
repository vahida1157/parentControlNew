package com.vahak.mehrban.domain.usecase

import com.vahak.mehrban.domain.repository.ProfileRepository
import javax.inject.Inject

class SetupSecurityUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend fun execute(pin: String, question: String, answer: String): Result<Unit> {
        if (pin.length !in 4..8) return Result.failure(Exception("رمز عبور باید 4 الی 8 رقم باشد."))
        if (answer.trim().length < 2) return Result.failure(Exception("پاسخ امنیتی بسیار کوتاه است."))
        if (question.isBlank()) return Result.failure(Exception("سوال امنیتی انتخاب نشده است."))

        return repository.setupSecurity(pin, question, answer)
    }
}