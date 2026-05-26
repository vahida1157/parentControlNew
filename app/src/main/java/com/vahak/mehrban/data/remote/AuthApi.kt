package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// These data classes match the JSON expected and returned by Spring Boot
data class OtpRequestDto(
    @SerializedName("phoneNumber") val phoneNumber: String
)

data class VerifyRequestDto(
    @SerializedName("phoneNumber") val phoneNumber: String,
    @SerializedName("code") val code: String
)

data class AuthResponseDto(
    @SerializedName("message") val message: String,
    @SerializedName("parentId") val parentId: String?,
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("expiresInSeconds") val expiresInSeconds: Int?,
    @SerializedName("pinPassword") val pinPassword: String? = null,
    @SerializedName("securityQuestion") val securityQuestion: String? = null,
    @SerializedName("securityAnswer") val securityAnswer: String? = null
)

interface AuthApi {
    @POST("api/identity/v1/auth/request-otp")
    suspend fun requestOtp(@Body request: OtpRequestDto): Response<AuthResponseDto>

    @POST("api/identity/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyRequestDto): Response<AuthResponseDto>

    @POST("api/identity/v1/auth/logout")
    suspend fun logout(): Response<GeneralResponseDto>
}