package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.dao.SafeBrowserDao
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedSiteEntity
import com.vahak.mehrban.core.data.local.entity.BrowserHistoryEntity
import com.vahak.mehrban.core.data.local.entity.BrowserSettingsEntity
import com.vahak.mehrban.core.data.local.entity.FilterMode
import com.vahak.mehrban.core.data.local.entity.FullBrowserProfile
import com.vahak.mehrban.data.remote.BrowserHistoryDto
import com.vahak.mehrban.data.remote.BrowserKeywordDto
import com.vahak.mehrban.data.remote.BrowserPolicyApi
import com.vahak.mehrban.data.remote.BrowserPolicySyncRequestDto
import com.vahak.mehrban.data.remote.BrowserSettingsDto
import com.vahak.mehrban.data.remote.BrowserSiteDto
import com.vahak.mehrban.data.remote.BrowserTelemetryApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SafeBrowserRepository {
    fun observeFullProfile(childId: String): Flow<FullBrowserProfile?>
    fun observeHistory(childId: String): Flow<List<BrowserHistoryEntity>>

    suspend fun updateFilterMode(childId: String, mode: FilterMode)
    suspend fun updateSearchEngine(childId: String, engine: String)
    suspend fun updateCartoonWorld(childId: String, isEnabled: Boolean)

    suspend fun addAllowedSite(childId: String, url: String, label: String)
    suspend fun removeAllowedSite(childId: String, url: String)

    suspend fun addBlockedSite(childId: String, url: String)
    suspend fun removeBlockedSite(childId: String, url: String)

    suspend fun addBlockedKeyword(childId: String, keyword: String)
    suspend fun removeBlockedKeyword(childId: String, keyword: String)

    suspend fun logHistory(childId: String, url: String, title: String)
    fun observeHistoryForDate(
        childId: String, startOfDay: Long, endOfDay: Long
    ): Flow<List<BrowserHistoryEntity>>

    suspend fun syncBrowserDataFromServer(childId: String): Result<Unit>
    suspend fun pushBrowserDataToServer(childId: String): Result<Unit>
    suspend fun fetchHistoryFromServerForDate(childId: String, startOfDay: Long, endOfDay: Long)
}

class SafeBrowserRepositoryImpl @Inject constructor(
    private val dao: SafeBrowserDao,
    private val policyApi: BrowserPolicyApi,
    private val telemetryApi: BrowserTelemetryApi,
) : SafeBrowserRepository {

    override fun observeFullProfile(childId: String) = dao.observeFullProfile(childId)
    override fun observeHistory(childId: String) = dao.observeHistory(childId)

    // Ensure settings exist without forcing a fake "updatedAt" timestamp that breaks syncing
    private suspend fun ensureSettingsExist(childId: String): BrowserSettingsEntity {
        return dao.getSettingsSync(childId) ?: BrowserSettingsEntity(childId = childId)
    }

    override suspend fun updateFilterMode(childId: String, mode: FilterMode) {
        val settings = ensureSettingsExist(childId).copy(
            filterMode = mode, isSynced = false, updatedAt = System.currentTimeMillis()
        )
        dao.upsertSettings(settings)
    }

    override suspend fun updateSearchEngine(childId: String, engine: String) {
        val settings = ensureSettingsExist(childId).copy(
            searchEngine = engine, isSynced = false, updatedAt = System.currentTimeMillis()
        )
        dao.upsertSettings(settings)
    }

    override suspend fun updateCartoonWorld(childId: String, isEnabled: Boolean) {
        val settings = ensureSettingsExist(childId).copy(
            isCartoonWorldEnabled = isEnabled,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertSettings(settings)
    }

    // --- Toggles (Upserts & Soft Deletes) ---
    override suspend fun addAllowedSite(childId: String, url: String, label: String) {
        dao.upsertAllowedSite(
            BrowserAllowedSiteEntity(
                childId = childId, url = url, label = label, isActive = true, isSynced = false
            )
        )
    }

    override suspend fun removeAllowedSite(childId: String, url: String) {
        dao.softDeleteAllowedSite(childId, url, System.currentTimeMillis())
    }

    override suspend fun addBlockedSite(childId: String, url: String) {
        dao.upsertBlockedSite(
            BrowserBlockedSiteEntity(
                childId = childId, url = url, isActive = true, isSynced = false
            )
        )
    }

    override suspend fun removeBlockedSite(childId: String, url: String) {
        dao.softDeleteBlockedSite(childId, url, System.currentTimeMillis())
    }

    override suspend fun addBlockedKeyword(childId: String, keyword: String) {
        dao.upsertBlockedKeyword(
            BrowserBlockedKeywordEntity(
                childId = childId, keyword = keyword, isActive = true, isSynced = false
            )
        )
    }

    override suspend fun removeBlockedKeyword(childId: String, keyword: String) {
        dao.softDeleteBlockedKeyword(childId, keyword, System.currentTimeMillis())
    }

    override suspend fun logHistory(childId: String, url: String, title: String) {
        // UUID is auto-generated inside the entity default parameters
        dao.insertHistory(BrowserHistoryEntity(childId = childId, url = url, title = title))
    }

    override fun observeHistoryForDate(childId: String, startOfDay: Long, endOfDay: Long) =
        dao.observeHistoryForDate(childId, startOfDay, endOfDay)

    // --- Sync Logic ---
    override suspend fun pushBrowserDataToServer(childId: String): Result<Unit> {
        return try {
            val unsyncedSettings = dao.getUnsyncedSettings(childId)
            val unsyncedAllowed = dao.getUnsyncedAllowedSites(childId)
            val unsyncedBlocked = dao.getUnsyncedBlockedSites(childId)
            val unsyncedKeys = dao.getUnsyncedBlockedKeywords(childId)
            val unsyncedHist = dao.getUnsyncedHistory(childId)

            var policySuccess = true
            var telemetrySuccess = true

            // 🚀 1. PUSH POLICY
            if (unsyncedSettings != null || unsyncedAllowed.isNotEmpty() || unsyncedBlocked.isNotEmpty() || unsyncedKeys.isNotEmpty()) {
                val policyRequest = BrowserPolicySyncRequestDto(settings = unsyncedSettings?.let {
                    BrowserSettingsDto(
                        it.searchEngine, it.isCartoonWorldEnabled, it.filterMode.name, it.updatedAt
                    )
                }, allowedSites = unsyncedAllowed.map {
                    BrowserSiteDto(
                        it.url, it.label, it.isActive, it.updatedAt
                    )
                }, blockedSites = unsyncedBlocked.map {
                    BrowserSiteDto(
                        it.url, null, it.isActive, it.updatedAt
                    )
                }, blockedKeywords = unsyncedKeys.map {
                    BrowserKeywordDto(
                        it.keyword, it.isActive, it.updatedAt
                    )
                })

                val policyResponse = policyApi.syncBrowserPolicy(childId, policyRequest)

                if (policyResponse.isSuccessful) {
                    if (unsyncedSettings != null) dao.markSettingsAsSynced(childId)

                    // CLEANUP: Split into active (keep & mark synced) and inactive (hard delete)
                    val (activeAllowed, deletedAllowed) = unsyncedAllowed.partition { it.isActive }
                    if (activeAllowed.isNotEmpty()) dao.markAllowedSitesAsSynced(
                        childId, activeAllowed.map { it.url })
                    if (deletedAllowed.isNotEmpty()) dao.hardDeleteAllowedSites(
                        childId, deletedAllowed.map { it.url })

                    val (activeBlocked, deletedBlocked) = unsyncedBlocked.partition { it.isActive }
                    if (activeBlocked.isNotEmpty()) dao.markBlockedSitesAsSynced(
                        childId, activeBlocked.map { it.url })
                    if (deletedBlocked.isNotEmpty()) dao.hardDeleteBlockedSites(
                        childId, deletedBlocked.map { it.url })

                    val (activeKeys, deletedKeys) = unsyncedKeys.partition { it.isActive }
                    if (activeKeys.isNotEmpty()) dao.markBlockedKeywordsAsSynced(
                        childId, activeKeys.map { it.keyword })
                    if (deletedKeys.isNotEmpty()) dao.hardDeleteBlockedKeywords(
                        childId, deletedKeys.map { it.keyword })
                } else {
                    policySuccess = false
                }
            }

            // 🚀 2. PUSH TELEMETRY (HISTORY)
            if (unsyncedHist.isNotEmpty()) {
                val historyRequest =
                    unsyncedHist.map { BrowserHistoryDto(it.id, it.url, it.title, it.timestamp) }
                val telemetryResponse = telemetryApi.syncBrowserHistory(childId, historyRequest)

                if (telemetryResponse.isSuccessful) {
                    // ID is now a String (UUID)
                    dao.markHistoryAsSynced(childId, unsyncedHist.map { it.id })
                } else {
                    telemetrySuccess = false
                }
            }

            if (policySuccess && telemetrySuccess) Result.success(Unit)
            else Result.failure(Exception("Sync partially or fully failed"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun fetchHistoryFromServerForDate(
        childId: String, startOfDay: Long, endOfDay: Long
    ) {
        try {
            val response = telemetryApi.getBrowserHistoryForDate(childId, startOfDay, endOfDay)
            if (response.isSuccessful && response.body() != null) {
                val serverHistory = response.body()!!
                val historyEntities = serverHistory.map { dto ->
                    BrowserHistoryEntity(
                        id = dto.id,
                        childId = childId,
                        url = dto.url,
                        title = dto.title,
                        timestamp = dto.timestamp,
                        isSynced = true
                    )
                }
                if (historyEntities.isNotEmpty()) {
                    dao.insertHistoryList(historyEntities)
                }
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun syncBrowserDataFromServer(childId: String): Result<Unit> {
        pushBrowserDataToServer(childId) // Push local changes first

        return try {
            val response = policyApi.getBrowserSettings(childId)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!

                // 1. Sync Settings
                data.settings?.let { s ->
                    val localSettings = dao.getSettingsSync(childId)
                    if (localSettings == null || s.updatedAt > localSettings.updatedAt) {
                        dao.upsertSettings(
                            BrowserSettingsEntity(
                                childId = childId,
                                searchEngine = s.searchEngine,
                                isCartoonWorldEnabled = s.isCartoonWorldEnabled,
                                filterMode = FilterMode.valueOf(s.filterMode),
                                isSynced = true,
                                updatedAt = s.updatedAt
                            )
                        )
                    }
                }

                // 2. Sync Allowed Sites
                val localUnsyncedA = dao.getUnsyncedAllowedSites(childId).map { it.url }.toSet()
                val serverA =
                    data.allowedSites.filterNot { localUnsyncedA.contains(it.url) }.map {
                        BrowserAllowedSiteEntity(
                            childId = childId,
                            url = it.url,
                            label = it.label ?: "",
                            isActive = it.isActive,
                            isSynced = true,
                            updatedAt = it.updatedAt
                        )
                    }
                if (serverA.isNotEmpty()) dao.upsertAllowedSites(serverA)

                // 3. Sync Blocked Sites
                val localUnsyncedB = dao.getUnsyncedBlockedSites(childId).map { it.url }.toSet()
                val serverB =
                    data.blockedSites.filterNot { localUnsyncedB.contains(it.url) }.map {
                        BrowserBlockedSiteEntity(
                            childId = childId,
                            url = it.url,
                            isActive = it.isActive,
                            isSynced = true,
                            updatedAt = it.updatedAt
                        )
                    }
                if (serverB.isNotEmpty()) dao.upsertBlockedSites(serverB)

                // 4. Sync Keywords
                val localUnsyncedK =
                    dao.getUnsyncedBlockedKeywords(childId).map { it.keyword }.toSet()
                val serverK =
                    data.blockedKeywords.filterNot { localUnsyncedK.contains(it.keyword) }
                        .map {
                            BrowserBlockedKeywordEntity(
                                childId = childId,
                                keyword = it.keyword,
                                isActive = it.isActive,
                                isSynced = true,
                                updatedAt = it.updatedAt
                            )
                        }
                if (serverK.isNotEmpty()) dao.upsertBlockedKeywords(serverK)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}