package com.vahak.mehrban.data.remote

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

// --- DTOs ---
data class BrowserSettingsDto(
    @SerializedName("searchEngine") val searchEngine: String,
    @SerializedName("isCartoonWorldEnabled") val isCartoonWorldEnabled: Boolean,
    @SerializedName("filterMode") val filterMode: String, // "WHITELIST_ONLY", "BLACKLIST_ONLY", "DISABLED"
    @SerializedName("updatedAt") val updatedAt: Long
)

data class BrowserSiteDto(
    @SerializedName("url") val url: String,
    @SerializedName("label") val label: String?, // Nullable for blocked sites
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

// Used for pushing data to the server
data class BulkBrowserRequestDto(
    @SerializedName("settings") val settings: BrowserSettingsDto? = null,
    @SerializedName("allowedSites") val allowedSites: List<BrowserSiteDto> = emptyList(),
    @SerializedName("blockedSites") val blockedSites: List<BrowserSiteDto> = emptyList(),
    @SerializedName("blockedKeywords") val blockedKeywords: List<BrowserKeywordDto> = emptyList(),
    @SerializedName("history") val history: List<BrowserHistoryDto> = emptyList()
)

// Used for pulling data from the server
data class BrowserSyncResponseDto(
    @SerializedName("settings") val settings: BrowserSettingsDto?,
    @SerializedName("allowedSites") val allowedSites: List<BrowserSiteDto>,
    @SerializedName("blockedSites") val blockedSites: List<BrowserSiteDto>,
    @SerializedName("blockedKeywords") val blockedKeywords: List<BrowserKeywordDto>
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