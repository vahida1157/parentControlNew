package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class OtpRequestDto(
	@SerializedName("phoneNumber") val phoneNumber: String,
	@SerializedName("appName") val appName: String = "MEHRBAN_KID",
)

data class VerifyRequestDto(
	@SerializedName("phoneNumber") val phoneNumber: String,
	@SerializedName("code") val code: String,
	@SerializedName("appName") val appName: String = "MEHRBAN_KID",
)

data class OtpResponseDto(
	@SerializedName("message") val message: String,
	@SerializedName("expiresInSeconds") val expiresInSeconds: Int
)

data class VerifyResponseDto(
	@SerializedName("message") val message: String,
	@SerializedName("parentId") val parentId: String,
	@SerializedName("accessToken") val accessToken: String,
	@SerializedName("pinPassword") val pinPassword: String? = null,
	@SerializedName("securityQuestion") val securityQuestion: String? = null,
	@SerializedName("securityAnswer") val securityAnswer: String? = null,
	@SerializedName("hasInstalledExercise") val hasInstalledExercise: Boolean = false,
	@SerializedName("hasInstalledMehrban") val hasInstalledMehrban: Boolean = false,
)

data class AppInstalledStatusResponseDto(
	@SerializedName("hasInstalledExercise") val hasInstalledExercise: Boolean = false,
	@SerializedName("hasInstalledMehrban") val hasInstalledMehrban: Boolean = false
)

interface AuthApi {
	@POST("api/identity/v1/auth/request-otp")
	suspend fun requestOtp(@Body request: OtpRequestDto): Response<OtpResponseDto>

	@POST("api/identity/v1/auth/verify-otp")
	suspend fun verifyOtp(@Body request: VerifyRequestDto): Response<VerifyResponseDto>

	@POST("api/identity/v1/auth/logout")
	suspend fun logout(): Response<GeneralResponseDto>

	@GET("api/identity/v1/auth/app-installed-status")
	suspend fun checkAppInstalledStatus(): Response<AppInstalledStatusResponseDto>
}