package com.vahak.parentcontroll.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class CreateChildRequestDto(
    val name: String,
    val dob: String, // We will send ISO format "YYYY-MM-DD"
    val gender: String, // "BOY" or "GIRL"
    val avatarId: Int
)

data class ChildResponseDto(
    val id: String,
    val name: String,
    val dob: String,
    val gender: String,
    val avatarId: Int
)

interface ChildApi {
    @POST("api/v1/children")
    suspend fun addChild(@Body request: CreateChildRequestDto): Response<ChildResponseDto>

    @GET("api/v1/children")
    suspend fun getChildren(): Response<List<ChildResponseDto>>
}