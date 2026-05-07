package com.vahak.parentcontroll.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// These data classes match the JSON expected and returned by Spring Boot
data class OtpRequestDto(val phoneNumber: String)
data class VerifyRequestDto(val phoneNumber: String, val code: String)
data class AuthResponseDto(
    val message: String,
    val parentId: String?,
    val accessToken: String?,
    val expiresInSeconds: Int?,
    val hasPinSetup: Boolean?
)

interface AuthApi {
    @POST("api/v1/auth/request-otp")
    suspend fun requestOtp(@Body request: OtpRequestDto): Response<AuthResponseDto>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyRequestDto): Response<AuthResponseDto>

    @POST("api/v1/auth/logout")
    suspend fun logout(): Response<GeneralResponseDto>
}