package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class UsageSyncPayload(
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("activeChildId") val activeChildId: String?,
    @SerializedName("dailyUsages") val dailyUsages: List<DailyUsageDto>,
    @SerializedName("appUsages") val appUsages: List<AppUsageDto>
)

data class DailyUsageDto(
    @SerializedName("childId") val childId: String,
    @SerializedName("date") val date: String,
    @SerializedName("usedSeconds") val usedSeconds: Int
)

data class AppUsageDto(
    @SerializedName("childId") val childId: String,
    @SerializedName("date") val date: String,
    @SerializedName("packageName") val packageName: String,
    @SerializedName("usedSeconds") val usedSeconds: Int
)

data class GlobalUsageResponse(
    @SerializedName("globalDailySeconds") val globalDailySeconds: Map<String, Int>,
    @SerializedName("globalAppSeconds") val globalAppSeconds: Map<String, Map<String, Int>>
)

data class AppReportResponse(
    @SerializedName("totalDailySeconds") val totalDailySeconds: Int,
    @SerializedName("apps") val apps: List<AppUsageBreakdown>
)

data class AppUsageBreakdown(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("totalSeconds") val totalSeconds: Int,
    @SerializedName("devices") val devices: List<DeviceUsageDetail>
)

data class DeviceUsageDetail(
    @SerializedName("deviceName") val deviceName: String,
    @SerializedName("usedSeconds") val usedSeconds: Int
)

interface UsageApi {
    @POST("api/telemetry/v1/usage/sync")
    suspend fun syncUsageData(@Body payload: UsageSyncPayload): Response<GlobalUsageResponse>

    @GET("api/telemetry/v1/usage/report/{childId}")
    suspend fun getUsageReport(
        @Path("childId") childId: String, @Query("date") date: String
    ): Response<AppReportResponse>
}