package com.vahak.mehrban.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

data class UsageSyncPayload(
    val deviceId: String,
    val deviceName: String,
    val activeChildId: String?,
    val dailyUsages: List<DailyUsageDto>,
    val appUsages: List<AppUsageDto>
)

data class DailyUsageDto(val childId: String, val date: String, val usedSeconds: Int)

data class AppUsageDto(
    val childId: String, val date: String, val packageName: String, val usedSeconds: Int
)

// 🚀 NEW: The response contract from Spring Boot pushing the real-time aggregated metrics
data class GlobalUsageResponse(
    val globalDailySeconds: Map<String, Int>, // Key: childId, Value: Sum of all their devices combined
    val globalAppSeconds: Map<String, Map<String, Int>> // Key: childId -> Map(packageName -> totalSeconds)
)

// 🚀 NEW: The Drill-Down Report Contracts
data class AppReportResponse(
    val totalDailySeconds: Int, val apps: List<AppUsageBreakdown>
)

data class AppUsageBreakdown(
    val packageName: String, val totalSeconds: Int, val devices: List<DeviceUsageDetail>
)

data class DeviceUsageDetail(
    val deviceName: String, val usedSeconds: Int
)

interface UsageApi {
    @POST("api/telemetry/v1/usage/sync")
    suspend fun syncUsageData(@Body payload: UsageSyncPayload): Response<GlobalUsageResponse>

    @GET("api/telemetry/v1/usage/report/{childId}")
    suspend fun getUsageReport(
        @Path("childId") childId: String, @Query("date") date: String
    ): Response<AppReportResponse>
}