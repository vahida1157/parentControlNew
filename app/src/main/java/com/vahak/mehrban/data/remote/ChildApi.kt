package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class CreateChildRequestDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("dob") val dob: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("avatarId") val avatarId: Int,
    @SerializedName("phone") val phone: String?
)

data class ChildResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("dob") val dob: String,
    @SerializedName("gender") val gender: String,
    @SerializedName("avatarId") val avatarId: Int,
    @SerializedName("phone") val phone: String?
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