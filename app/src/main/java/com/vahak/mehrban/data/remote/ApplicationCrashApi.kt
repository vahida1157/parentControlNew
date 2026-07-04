package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApplicationCrashApi {

    data class CrashLogDto(
        @SerializedName("id") val id: String,
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("appVersion") val appVersion: String,
        @SerializedName("androidVersion") val androidVersion: String,
        @SerializedName("deviceModel") val deviceModel: String,
        @SerializedName("exceptionType") val exceptionType: String,
        @SerializedName("stackTrace") val stackTrace: String
    )

    @POST("api/telemetry/v1/crashes/sync")
    suspend fun syncCrashLogs(
        @Body crashLogs: List<CrashLogDto>
    ): Response<Void>
}