package com.vahak.parentcontroll.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class AppRuleDto(
    val packageName: String,
    val isAllowed: Boolean,
    val updatedAt: Long,
)

data class BulkRuleRequestDto(
    val rules: List<AppRuleDto>
)

interface RuleApi {
    @GET("api/v1/rules/{childId}")
    suspend fun getAppRules(@Path("childId") childId: String): Response<List<AppRuleDto>>

    @PUT("api/v1/rules/{childId}/bulk")
    suspend fun updateAppRules(
        @Path("childId") childId: String,
        @Body request: BulkRuleRequestDto
    ): Response<Map<String, String>>
}