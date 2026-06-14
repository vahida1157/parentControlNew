package com.vahak.mehrban.domain.repository

import android.content.Context
import android.util.Log
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.ParentControlDatabase
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.data.remote.AuthApi
import com.vahak.mehrban.data.remote.OtpRequestDto
import com.vahak.mehrban.data.remote.VerifyRequestDto
import com.vahak.mehrban.domain.usecase.OtpValidationResult
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context
) : AuthRepository {
    override suspend fun requestOtp(phone: String): OtpValidationResult {
        return try {
            val response = authApi.requestOtp(OtpRequestDto(phoneNumber = phone))

            if (response.isSuccessful) {
                val ttl = response.body()?.expiresInSeconds ?: 120
                OtpValidationResult.Success(expiresInSeconds = ttl)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorBody!!).getString("error")
                } catch (_: Exception) {
                    context.getString(R.string.error_server_communication)
                }
                OtpValidationResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "FATAL NETWORK CRASH: ", e)
            OtpValidationResult.Error(context.getString(R.string.error_network_check_internet))
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
                Result.failure(Exception(context.getString(R.string.error_wrong_verification_code)))
            }
        } catch (_: Exception) {
            Result.failure(Exception(context.getString(R.string.error_network_connection)))
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