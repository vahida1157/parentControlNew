package com.vahak.parentcontroll.domain.repository

import com.vahak.parentcontroll.core.data.local.ParentControlDatabase
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.data.remote.AuthApi
import com.vahak.parentcontroll.data.remote.OtpRequestDto
import com.vahak.parentcontroll.data.remote.VerifyRequestDto
import com.vahak.parentcontroll.domain.usecase.OtpValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

interface AuthRepository {
    suspend fun requestOtp(phone: String): OtpValidationResult
    suspend fun verifyOtp(phone: String, code: String): Result<Pair<String, Boolean>>
    suspend fun logout()
}

class AuthRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val authApi: AuthApi,
    private val database: ParentControlDatabase,
) : AuthRepository {
    override suspend fun requestOtp(phone: String): OtpValidationResult {
        return try {
            val response = authApi.requestOtp(OtpRequestDto(phoneNumber = phone))

            if (response.isSuccessful) {
                val ttl = response.body()?.expiresInSeconds ?: 120
                OtpValidationResult.Success(expiresInSeconds = ttl)
            } else {
                // Parse the clean error message from Spring Boot's GlobalExceptionHandler
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorBody!!).getString("error")
                } catch (_: Exception) {
                    "خطا در ارتباط با سرور"
                }
                OtpValidationResult.Error(errorMessage)
            }
        } catch (_: Exception) {
            OtpValidationResult.Error("خطای شبکه. لطفا اینترنت خود را بررسی کنید.")
        }
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<Pair<String, Boolean>> {
        return try {
            val response = authApi.verifyOtp(VerifyRequestDto(phone, code))
            val body = response.body()

            if (response.isSuccessful && body?.accessToken != null) {
                // Save it all directly to DataStore
                sessionManager.saveSession(
                    body.accessToken,
                    phone,
                    body.pinPassword,
                    body.securityQuestion,
                    body.securityAnswer
                )

                // Return the token and the flag
                Result.success(Pair(body.accessToken, !(body.pinPassword.isNullOrEmpty())))
            } else {
                Result.failure(Exception("کد وارد شده صحیح نیست"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("خطای شبکه در ارتباط با سرور"))
        }
    }

    override suspend fun logout() {
        try {
            authApi.logout()
        } catch (_: Exception) {
        }
        sessionManager.clearSession()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
    }
}