package com.vahak.parentcontroll.domain.repository

import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.domain.usecase.ValidationResult

interface AuthRepository {
    suspend fun requestOtp(phone: String): ValidationResult
    suspend fun verifyOtp(phone: String, code: String): Result<String>
    suspend fun logout()
}

class AuthRepositoryImpl(private val sessionManager: SessionManager) : AuthRepository {

    override suspend fun requestOtp(phone: String): ValidationResult {
        // --- TODO: FUTURE SPRING BACKEND CALL ---
        // val response = apiService.sendOtp(phone)
        // ----------------------------------------
        return ValidationResult.Success
    }

    override suspend fun verifyOtp(phone: String, code: String): Result<String> {
        // --- TODO: FUTURE SPRING BACKEND CALL ---
        // Example: val response = apiService.verify(phone, code)
        // ----------------------------------------

        return if (code == "1234") { // Mock success
            val mockToken = "eyJhbGciOiJIUzI1NiIsIn..."
            sessionManager.saveSession(mockToken, phone)
            Result.success(mockToken)
        } else {
            Result.failure(Exception("کد وارد شده صحیح نیست"))
        }
    }

    override suspend fun logout() {
        sessionManager.clearSession()
    }
}