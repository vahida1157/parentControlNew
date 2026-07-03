package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.ParentControlDatabase
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.data.remote.AuthApi
import com.vahak.mehrban.data.remote.OtpRequestDto
import com.vahak.mehrban.data.remote.VerifyRequestDto
import com.vahak.mehrban.domain.error.AuthError
import com.vahak.mehrban.domain.error.AuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import javax.inject.Inject

interface AuthRepository {
    suspend fun requestOtp(phone: String): Result<Int>
    suspend fun verifyOtp(phone: String, code: String): Result<Pair<String, Boolean>>
    suspend fun logout()
}

class AuthRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val authApi: AuthApi,
    private val database: ParentControlDatabase
) : AuthRepository {

    override suspend fun requestOtp(phone: String): Result<Int> {
        return try {
            Timber.d("Initiating OTP request")
            val response = authApi.requestOtp(OtpRequestDto(phoneNumber = phone))

            if (response.isSuccessful) {
                val ttl = response.body()?.expiresInSeconds ?: 120
                Timber.i("OTP requested successfully, ttlSeconds: %d", ttl)
                Result.success(ttl)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorBody!!).getString("errorMessage")
                } catch (_: Exception) {
                    null
                }
                Timber.w("Failed to request OTP, server rejection")
                Result.failure(AuthException(AuthError.SERVER_REJECTION, errorMessage))
            }
        } catch (e: Exception) {
            Timber.e(e, "System failure during OTP request")
            Result.failure(AuthException(AuthError.NETWORK_UNAVAILABLE))
        }
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<Pair<String, Boolean>> {
        return try {
            Timber.d("Initiating OTP verification")
            val response = authApi.verifyOtp(VerifyRequestDto(phone, code))
            val body = response.body()

            if (response.isSuccessful && body != null) {
                Timber.d("Saving session configuration locally")
                sessionManager.saveSession(
                    body.accessToken,
                    phone,
                    body.parentId,
                    body.pinPassword,
                    body.securityQuestion,
                    body.securityAnswer
                )
                Timber.i("OTP verified successfully, session established")
                Result.success(Pair(body.accessToken, !body.pinPassword.isNullOrEmpty()))
            } else {
                Timber.w("Failed to verify OTP, invalid credentials or bad request")
                Result.failure(AuthException(AuthError.WRONG_VERIFICATION_CODE))
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error during OTP verification")
            Result.failure(AuthException(AuthError.NETWORK_UNAVAILABLE))
        }
    }

    override suspend fun logout() {
        Timber.d("Initiating user logout process")
        try {
            authApi.logout()
        } catch (e: Exception) {
            Timber.w(e, "Server logout request failed, proceeding with local cleanup")
        }

        Timber.d("Clearing local session and database tables")
        sessionManager.clearSession()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
        Timber.i("User logged out successfully")
    }
}