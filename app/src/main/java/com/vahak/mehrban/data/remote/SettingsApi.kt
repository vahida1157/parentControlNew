package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class UpdateSettingsRequestDto(
    @SerializedName("isChildThemeActive") val isChildThemeActive: Boolean,
    @SerializedName("isTimeLimitActive") val isTimeLimitActive: Boolean,
    @SerializedName("dailyTimeLimitMins") val dailyTimeLimitMins: Int,
    @SerializedName("isSleepTimeActive") val isSleepTimeActive: Boolean,
    @SerializedName("sleepTimeStart") val sleepTimeStart: String,
    @SerializedName("sleepTimeEnd") val sleepTimeEnd: String,
    @SerializedName("isSiteManagementActive") val isSiteManagementActive: Boolean,
    @SerializedName("updatedAt") val updatedAt: Long
)

data class GlobalSettingsResponseDto(
    @SerializedName("childId") val childId: String,
    @SerializedName("isChildThemeActive") val isChildThemeActive: Boolean,
    @SerializedName("isTimeLimitActive") val isTimeLimitActive: Boolean,
    @SerializedName("dailyTimeLimitMins") val dailyTimeLimitMins: Int,
    @SerializedName("isSleepTimeActive") val isSleepTimeActive: Boolean,
    @SerializedName("sleepTimeStart") val sleepTimeStart: String,
    @SerializedName("sleepTimeEnd") val sleepTimeEnd: String,
    @SerializedName("isSiteManagementActive") val isSiteManagementActive: Boolean
)


interface SettingsApi {
    @GET("api/policy/v1/settings/{childId}")
    suspend fun getChildSettings(@Path("childId") childId: String): Response<GlobalSettingsResponseDto>

    @PUT("api/policy/v1/settings/{childId}")
    suspend fun updateChildSettings(
        @Path("childId") childId: String, @Body request: UpdateSettingsRequestDto
    ): Response<Unit>
}