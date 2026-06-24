package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.dao.SafeBrowserDao
import com.vahak.mehrban.core.data.local.entity.BrowserHistoryEntity
import com.vahak.mehrban.core.data.local.entity.BrowserKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserWhitelistEntity
import com.vahak.mehrban.data.remote.SafeBrowserApi
import com.vahak.mehrban.data.remote.BrowserHistoryDto
import com.vahak.mehrban.data.remote.BrowserKeywordDto
import com.vahak.mehrban.data.remote.BrowserWhitelistDto
import com.vahak.mehrban.data.remote.BulkBrowserRequestDto
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface SafeBrowserRepository {
    fun observeWhitelist(childId: String): Flow<List<BrowserWhitelistEntity>>
    fun observeKeywords(childId: String): Flow<List<BrowserKeywordEntity>>
    
    suspend fun toggleWhitelistSite(childId: String, urlPrefix: String, label: String, isActive: Boolean)
    suspend fun toggleKeyword(childId: String, keyword: String, isActive: Boolean)
    suspend fun logHistory(childId: String, url: String, title: String)
    
    suspend fun syncBrowserDataFromServer(childId: String): Result<Unit>
    suspend fun pushBrowserDataToServer(childId: String): Result<Unit>
}

class SafeBrowserRepositoryImpl @Inject constructor(
    private val dao: SafeBrowserDao,
    private val api: SafeBrowserApi
) : SafeBrowserRepository {

    override fun observeWhitelist(childId: String) = dao.observeActiveWhitelist(childId)
    override fun observeKeywords(childId: String) = dao.observeActiveKeywords(childId)

    override suspend fun toggleWhitelistSite(childId: String, urlPrefix: String, label: String, isActive: Boolean) {
        val entity = BrowserWhitelistEntity(
            childId = childId,
            urlPrefix = urlPrefix,
            label = label,
            isActive = isActive,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertWhitelistItem(entity)
    }

    override suspend fun toggleKeyword(childId: String, keyword: String, isActive: Boolean) {
        val entity = BrowserKeywordEntity(
            childId = childId,
            keyword = keyword,
            isActive = isActive,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        dao.upsertKeyword(entity)
    }

    override suspend fun logHistory(childId: String, url: String, title: String) {
        val entity = BrowserHistoryEntity(
            childId = childId,
            url = url,
            title = title,
            isSynced = false
        )
        dao.insertHistory(entity)
    }

    override suspend fun pushBrowserDataToServer(childId: String): Result<Unit> {
        return try {
            val unsyncedWhite = dao.getUnsyncedWhitelist(childId)
            val unsyncedKeys = dao.getUnsyncedKeywords(childId)
            val unsyncedHist = dao.getUnsyncedHistory(childId)

            if (unsyncedWhite.isEmpty() && unsyncedKeys.isEmpty() && unsyncedHist.isEmpty()) {
                return Result.success(Unit)
            }

            val request = BulkBrowserRequestDto(
                whitelist = unsyncedWhite.map { BrowserWhitelistDto(it.urlPrefix, it.label, it.colorKey, it.iconKey, it.isActive, it.updatedAt) },
                keywords = unsyncedKeys.map { BrowserKeywordDto(it.keyword, it.isActive, it.updatedAt) },
                history = unsyncedHist.map { BrowserHistoryDto(it.url, it.title, it.timestamp) }
            )

            val response = api.syncBrowserData(childId, request)
            if (response.isSuccessful) {
                dao.markWhitelistAsSynced(childId, unsyncedWhite.map { it.urlPrefix })
                dao.markKeywordsAsSynced(childId, unsyncedKeys.map { it.keyword })
                dao.markHistoryAsSynced(childId, unsyncedHist.map { it.id })
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncBrowserDataFromServer(childId: String): Result<Unit> {
        pushBrowserDataToServer(childId)

        return try {
            val response = api.getBrowserSettings(childId)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                
                // Whitelist Sync
                val localUnsyncedW = dao.getUnsyncedWhitelist(childId).map { it.urlPrefix }.toSet()
                val serverW = data.whitelist.filterNot { localUnsyncedW.contains(it.urlPrefix) }.map {
                    BrowserWhitelistEntity(childId = childId, urlPrefix = it.urlPrefix, label = it.label, colorKey = it.colorKey, iconKey = it.iconKey, isActive = it.isActive, isSynced = true, updatedAt = it.updatedAt)
                }
                dao.upsertWhitelistItems(serverW)

                // Keywords Sync
                val localUnsyncedK = dao.getUnsyncedKeywords(childId).map { it.keyword }.toSet()
                val serverK = data.keywords.filterNot { localUnsyncedK.contains(it.keyword) }.map {
                    BrowserKeywordEntity(childId = childId, keyword = it.keyword, isActive = it.isActive, isSynced = true, updatedAt = it.updatedAt)
                }
                dao.upsertKeywords(serverK)
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}