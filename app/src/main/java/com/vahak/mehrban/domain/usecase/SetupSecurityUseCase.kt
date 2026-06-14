package com.vahak.mehrban.domain.usecase

import android.content.Context
import com.vahak.mehrban.R
import com.vahak.mehrban.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SetupSecurityUseCase @Inject constructor(
    private val repository: ProfileRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(pin: String, question: String, answer: String): Result<Unit> {
        if (pin.length !in 4..8) return Result.failure(Exception(context.getString(R.string.error_pin_length)))
        if (answer.trim().length < 2) return Result.failure(Exception(context.getString(R.string.error_security_answer_short)))
        if (question.isBlank()) return Result.failure(Exception(context.getString(R.string.error_security_question_empty)))

        return repository.setupSecurity(pin, question, answer)
    }
}