package com.vahak.parentcontroll.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

data class BlockedDomainDto(
    val id: String,
    val domain: String,
    val isActive: Boolean
)

data class BulkDomainRequestDto(
    val domains: List<BlockedDomainDto>
)

interface WebApi {
    @GET("api/v1/domains/{childId}")
    suspend fun getBlockedDomains(@Path("childId") childId: String): Response<List<BlockedDomainDto>>

    // Bulk upsert for new or toggled domains
    @PUT("api/v1/domains/{childId}/bulk")
    suspend fun updateBlockedDomains(
        @Path("childId") childId: String,
        @Body request: BulkDomainRequestDto
    ): Response<Map<String, String>>

    // Delete a specific domain
    @DELETE("api/v1/domains/{childId}/{domainId}")
    suspend fun deleteDomain(
        @Path("childId") childId: String,
        @Path("domainId") domainId: String
    ): Response<Unit>
}