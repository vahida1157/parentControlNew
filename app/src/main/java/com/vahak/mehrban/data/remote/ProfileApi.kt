package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SetupSecurityRequestDto(
    @SerializedName("pin") val pin: String,
    @SerializedName("securityQuestion") val securityQuestion: String,
    @SerializedName("securityAnswer") val securityAnswer: String
)

data class GeneralResponseDto(
    @SerializedName("message") val message: String
)
interface ProfileApi {
    @POST("api/identity/v1/profile/security-setup")
    suspend fun setupSecurity(@Body request: SetupSecurityRequestDto): Response<GeneralResponseDto>
}