package com.vahak.mehrban.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class BlockedDomainDto(
    @SerializedName("id") val id: String,
    @SerializedName("domain") val domain: String,
    @SerializedName("isActive") val isActive: Boolean
)

data class BulkDomainRequestDto(
    @SerializedName("domains") val domains: List<BlockedDomainDto>
)


interface WebApi {
    @GET("api/policy/v1/domains/{childId}")
    suspend fun getBlockedDomains(@Path("childId") childId: String): Response<List<BlockedDomainDto>>

    @PUT("api/policy/v1/domains/{childId}/bulk")
    suspend fun updateBlockedDomains(
        @Path("childId") childId: String, @Body request: BulkDomainRequestDto
    ): Response<Map<String, String>>

    @DELETE("api/policy/v1/domains/{childId}/{domainId}")
    suspend fun deleteDomain(
        @Path("childId") childId: String, @Path("domainId") domainId: String
    ): Response<Unit>
}