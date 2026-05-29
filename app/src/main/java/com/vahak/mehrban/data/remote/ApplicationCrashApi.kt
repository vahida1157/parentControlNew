package com.vahak.mehrban.data.remote

import com.vahak.mehrban.core.data.local.entity.CrashLogEntity
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApplicationCrashApi {

    @POST("api/telemetry/v1/crashes/sync") // Adjust the prefix based on your API Gateway routing
    suspend fun syncCrashLogs(
        @Body crashLogs: List<CrashLogEntity>
    ): Response<Void>
}