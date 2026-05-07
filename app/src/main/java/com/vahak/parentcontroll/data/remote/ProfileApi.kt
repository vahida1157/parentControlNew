package com.vahak.parentcontroll.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SetupSecurityRequestDto(
    val pin: String,
    val securityQuestion: String,
    val securityAnswer: String
)

data class GeneralResponseDto(val message: String)

interface ProfileApi {
    @POST("api/v1/profile/security-setup")
    suspend fun setupSecurity(@Body request: SetupSecurityRequestDto): Response<GeneralResponseDto>
}