package com.vahak.mehrban.data.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class BrowserWhitelistDto(
    @SerializedName("urlPrefix") val urlPrefix: String,
    @SerializedName("label") val label: String,
    @SerializedName("colorKey") val colorKey: String,
    @SerializedName("iconKey") val iconKey: String,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("updatedAt") val updatedAt: Long
)

data class BrowserKeywordDto(
    @SerializedName("keyword") val keyword: String,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("updatedAt") val updatedAt: Long
)

data class BrowserHistoryDto(
    @SerializedName("url") val url: String,
    @SerializedName("title") val title: String,
    @SerializedName("timestamp") val timestamp: Long
)

data class BulkBrowserRequestDto(
    @SerializedName("whitelist") val whitelist: List<BrowserWhitelistDto> = emptyList(),
    @SerializedName("keywords") val keywords: List<BrowserKeywordDto> = emptyList(),
    @SerializedName("history") val history: List<BrowserHistoryDto> = emptyList()
)

data class BrowserSyncResponseDto(
    @SerializedName("whitelist") val whitelist: List<BrowserWhitelistDto>,
    @SerializedName("keywords") val keywords: List<BrowserKeywordDto>
)

@Keep
interface SafeBrowserApi {
    @GET("api/policy/v1/browser/{childId}")
    suspend fun getBrowserSettings(@Path("childId") childId: String): Response<BrowserSyncResponseDto>

    @PUT("api/policy/v1/browser/{childId}/sync")
    suspend fun syncBrowserData(
        @Path("childId") childId: String,
        @Body request: BulkBrowserRequestDto
    ): Response<Unit>
}