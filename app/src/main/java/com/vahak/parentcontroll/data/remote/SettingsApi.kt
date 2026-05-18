package com.vahak.parentcontroll.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class UpdateSettingsRequestDto(
    val isChildThemeActive: Boolean,
    val isTimeLimitActive: Boolean,
    val dailyTimeLimitMins: Int,
    val isSleepTimeActive: Boolean,
    val sleepTimeStart: String, // LocalTime sent as String "22:00"
    val sleepTimeEnd: String,   // LocalTime sent as String "07:00"
    val isSiteManagementActive: Boolean,
    val updatedAt: Long         // MANDATORY: Triggers Last-Write-Wins on backend
)

data class GlobalSettingsResponseDto(
    val childId: String,
    val isChildThemeActive: Boolean,
    val isTimeLimitActive: Boolean,
    val dailyTimeLimitMins: Int,
    val isSleepTimeActive: Boolean,
    val sleepTimeStart: String,
    val sleepTimeEnd: String,
    val isSiteManagementActive: Boolean
)

interface SettingsApi {
    @GET("api/v1/settings/{childId}")
    suspend fun getChildSettings(
        @Path("childId") childId: String
    ): Response<GlobalSettingsResponseDto>

    @PUT("api/v1/settings/{childId}")
    suspend fun updateChildSettings(
        @Path("childId") childId: String,
        @Body request: UpdateSettingsRequestDto
    ): Response<Unit>
}