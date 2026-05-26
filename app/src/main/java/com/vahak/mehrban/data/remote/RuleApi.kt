package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class AppRuleDto(
    @SerializedName("packageName") val packageName: String,
    @SerializedName("isAllowed") val isAllowed: Boolean,
    @SerializedName("updatedAt") val updatedAt: Long
)

data class BulkRuleRequestDto(
    @SerializedName("rules") val rules: List<AppRuleDto>
)

interface RuleApi {
    @GET("api/policy/v1/rules/{childId}")
    suspend fun getAppRules(@Path("childId") childId: String): Response<List<AppRuleDto>>

    @PUT("api/policy/v1/rules/{childId}/bulk")
    suspend fun updateAppRules(
        @Path("childId") childId: String, @Body request: BulkRuleRequestDto
    ): Response<Map<String, String>>
}