package com.vahak.mehrban.domain.repository

import android.util.Log
import com.vahak.mehrban.core.data.local.dao.WebDao
import com.vahak.mehrban.core.data.local.entity.BlockedDomainEntity
import com.vahak.mehrban.data.remote.BlockedDomainDto
import com.vahak.mehrban.data.remote.BulkDomainRequestDto
import com.vahak.mehrban.data.remote.WebApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface WebRepository {
    fun observeBlockedDomains(childId: String): Flow<List<BlockedDomainEntity>>
    suspend fun addDomain(childId: String, domainUrl: String)
    suspend fun removeDomain(domain: BlockedDomainEntity)
    suspend fun toggleDomainStatus(domainId: String, isActive: Boolean)
    suspend fun syncDomainsFromServer(childId: String): Result<Unit>
}

class WebRepositoryImpl @Inject constructor(
    private val webDao: WebDao,
    private val webApi: WebApi
) : WebRepository {

    override fun observeBlockedDomains(childId: String): Flow<List<BlockedDomainEntity>> {
        return webDao.observeBlockedDomains(childId)
    }

    override suspend fun addDomain(childId: String, domainUrl: String) {
        val newDomain = BlockedDomainEntity(
            childId = childId,
            domain = domainUrl,
            isActive = true,
            isSynced = false, // DIRTY
            isDeleted = false
        )
        webDao.insertDomain(newDomain)
        pushDirtyDomains(childId)
    }

    override suspend fun removeDomain(domain: BlockedDomainEntity) {
        // Soft Delete locally
        webDao.softDeleteDomain(domain.id)
        pushPendingDeletions(domain.childId)
    }

    override suspend fun toggleDomainStatus(domainId: String, isActive: Boolean) {
        webDao.setDomainActive(domainId, isActive) // DAO handles is_synced = 0
        // We need the childId to push, so we fetch it quickly. 
        // (Alternatively, pass childId into this function from the ViewModel)
        val childId = webDao.getUnsyncedDomains("").firstOrNull { it.id == domainId }?.childId
        if (childId != null) pushDirtyDomains(childId)
    }

    // --- SYNC ENGINE ---

    private suspend fun pushPendingDeletions(childId: String) {
        try {
            val pendingDeletes = webDao.getPendingDeletedDomains(childId)
            for (domain in pendingDeletes) {
                val response = webApi.deleteDomain(childId, domain.id)
                if (response.isSuccessful || response.code() == 404) {
                    // Server deleted it (or it was never there). Safe to hard delete locally.
                    webDao.hardDeleteDomain(domain.id)
                }
            }
        } catch (e: Exception) {
            Log.e("WebRepo", "Airplane mode. Deletions remain pending.")
        }
    }

    private suspend fun pushDirtyDomains(childId: String) {
        try {
            val unsyncedDomains = webDao.getUnsyncedDomains(childId)
            if (unsyncedDomains.isEmpty()) return

            val dtoList = unsyncedDomains.map {
                BlockedDomainDto(id = it.id, domain = it.domain, isActive = it.isActive)
            }

            val response = webApi.updateBlockedDomains(childId, BulkDomainRequestDto(domains = dtoList))

            if (response.isSuccessful) {
                val syncedIds = unsyncedDomains.map { it.id }
                webDao.markAsSynced(syncedIds)
            }
        } catch (e: Exception) {
            Log.e("WebRepo", "Airplane mode. Domains remain dirty.")
        }
    }

    override suspend fun syncDomainsFromServer(childId: String): Result<Unit> {
        // 1. Always push offline changes first!
        pushPendingDeletions(childId)
        pushDirtyDomains(childId)

        return try {
            val response = webApi.getBlockedDomains(childId)
            if (response.isSuccessful && response.body() != null) {

                val unsyncedIds = webDao.getUnsyncedDomains(childId).map { it.id }.toSet()
                val deletedIds = webDao.getPendingDeletedDomains(childId).map { it.id }.toSet()

                val serverDomains = response.body()!!.mapNotNull { dto ->
                    // Do not overwrite domains the parent JUST changed offline
                    if (unsyncedIds.contains(dto.id) || deletedIds.contains(dto.id)) {
                        null
                    } else {
                        BlockedDomainEntity(
                            id = dto.id,
                            childId = childId,
                            domain = dto.domain,
                            isActive = dto.isActive,
                            isSynced = true // Clean from server
                        )
                    }
                }

                webDao.insertDomains(serverDomains)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch domains"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error fetching domains"))
        }
    }
}