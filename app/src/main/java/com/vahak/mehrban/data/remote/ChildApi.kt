package com.vahak.mehrban.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class CreateChildRequestDto(
    val id: String,
    val name: String,
    val dob: String,
    val gender: String,
    val avatarId: Int,
    val phone: String?
)

data class ChildResponseDto(
    val id: String,
    val name: String,
    val dob: String,
    val gender: String,
    val avatarId: Int,
    val phone: String?
)

interface ChildApi {
    @GET("api/identity/v1/children")
    suspend fun getChildren(): Response<List<ChildResponseDto>>

    @PUT("api/identity/v1/children/{childId}")
    suspend fun upsertChild(
        @Path("childId") childId: String,
        @Body request: CreateChildRequestDto
    ): Response<ChildResponseDto>

    @DELETE("api/identity/v1/children/{childId}")
    suspend fun deleteChild(@Path("childId") childId: String): Response<Unit>
}