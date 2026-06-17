package com.vahak.mehrban.domain.repository

import android.content.Context
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
import timber.log.Timber
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
            Timber.d("Initiating OTP request")
            val response = authApi.requestOtp(OtpRequestDto(phoneNumber = phone))

            if (response.isSuccessful) {
                val ttl = response.body()?.expiresInSeconds ?: 120
                Timber.i("OTP requested successfully, ttlSeconds: %d", ttl)
                OtpValidationResult.Success(expiresInSeconds = ttl)
            } else {
                val errorBody = response.errorBody()?.string()
                val errorMessage = try {
                    JSONObject(errorBody!!).getString("error")
                } catch (_: Exception) {
                    context.getString(R.string.error_server_communication)
                }
                Timber.w("Failed to request OTP, server rejection")
                OtpValidationResult.Error(errorMessage)
            }
        } catch (e: Exception) {
            Timber.e(e, "System failure during OTP request")
            OtpValidationResult.Error(context.getString(R.string.error_network_check_internet))
        }
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<Pair<String, Boolean>> {
        return try {
            Timber.d("Initiating OTP verification")
            val response = authApi.verifyOtp(VerifyRequestDto(phone, code))
            val body = response.body()

            if (response.isSuccessful && body?.accessToken != null) {
                Timber.d("Saving session configuration locally")
                sessionManager.saveSession(
                    body.accessToken,
                    phone,
                    body.pinPassword,
                    body.securityQuestion,
                    body.securityAnswer
                )
                Timber.i("OTP verified successfully, session established")
                Result.success(Pair(body.accessToken, !(body.pinPassword.isNullOrEmpty())))
            } else {
                Timber.w("Failed to verify OTP, invalid credentials or bad request")
                Result.failure(Exception(context.getString(R.string.error_wrong_verification_code)))
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error during OTP verification")
            Result.failure(Exception(context.getString(R.string.error_network_connection)))
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