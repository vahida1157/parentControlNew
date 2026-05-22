package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.data.remote.ProfileApi
import com.vahak.mehrban.data.remote.SetupSecurityRequestDto
import javax.inject.Inject

interface ProfileRepository {
    suspend fun setupSecurity(pin: String, question: String, answer: String): Result<Unit>
}

class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    private val sessionManager: SessionManager
) : ProfileRepository {

    override suspend fun setupSecurity(pin: String, question: String, answer: String): Result<Unit> {
        return try {
            val request = SetupSecurityRequestDto(pin, question, answer)
            val response = profileApi.setupSecurity(request)

            if (response.isSuccessful) {
                // Backend saved it! Now we can safely save it locally for offline checks.
                sessionManager.setParentPin(pin)
                sessionManager.setSecurityData(question, answer)
                Result.success(Unit)
            } else {
                Result.failure(Exception("خطا در ذخیره اطلاعات در سرور"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("خطای شبکه. لطفا اینترنت خود را بررسی کنید."))
        }
    }
}