package com.vahak.mehrban.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vahak.mehrban.core.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeBrowserDao {

    @Transaction
    @Query("SELECT * FROM browser_settings WHERE child_id = :childId")
    fun observeFullProfile(childId: String): Flow<FullBrowserProfile?>

    // --- SETTINGS ---
    @Query("SELECT * FROM browser_settings WHERE child_id = :childId LIMIT 1")
    suspend fun getSettingsSync(childId: String): BrowserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: BrowserSettingsEntity)

    @Query("SELECT * FROM browser_settings WHERE child_id = :childId AND is_synced = 0 LIMIT 1")
    suspend fun getUnsyncedSettings(childId: String): BrowserSettingsEntity?

    @Query("UPDATE browser_settings SET is_synced = 1 WHERE child_id = :childId")
    suspend fun markSettingsAsSynced(childId: String)

    // --- ALLOWED SITES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllowedSite(item: BrowserAllowedSiteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllowedSites(items: List<BrowserAllowedSiteEntity>)

    @Query("UPDATE browser_allowed_sites SET is_active = 0, is_synced = 0, updated_at = :timestamp WHERE child_id = :childId AND url = :url")
    suspend fun softDeleteAllowedSite(childId: String, url: String, timestamp: Long)

    @Query("SELECT * FROM browser_allowed_sites WHERE child_id = :childId AND is_synced = 0")
    suspend fun getUnsyncedAllowedSites(childId: String): List<BrowserAllowedSiteEntity>

    @Query("UPDATE browser_allowed_sites SET is_synced = 1 WHERE child_id = :childId AND url IN (:urls)")
    suspend fun markAllowedSitesAsSynced(childId: String, urls: List<String>)

    // --- BLOCKED SITES ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedSite(item: BrowserBlockedSiteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedSites(items: List<BrowserBlockedSiteEntity>)

    @Query("UPDATE browser_blocked_sites SET is_active = 0, is_synced = 0, updated_at = :timestamp WHERE child_id = :childId AND url = :url")
    suspend fun softDeleteBlockedSite(childId: String, url: String, timestamp: Long)

    @Query("SELECT * FROM browser_blocked_sites WHERE child_id = :childId AND is_synced = 0")
    suspend fun getUnsyncedBlockedSites(childId: String): List<BrowserBlockedSiteEntity>

    @Query("UPDATE browser_blocked_sites SET is_synced = 1 WHERE child_id = :childId AND url IN (:urls)")
    suspend fun markBlockedSitesAsSynced(childId: String, urls: List<String>)

    // --- BLOCKED KEYWORDS ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedKeyword(item: BrowserBlockedKeywordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedKeywords(items: List<BrowserBlockedKeywordEntity>)

    @Query("UPDATE browser_blocked_keywords SET is_active = 0, is_synced = 0, updated_at = :timestamp WHERE child_id = :childId AND keyword = :keyword")
    suspend fun softDeleteBlockedKeyword(childId: String, keyword: String, timestamp: Long)

    @Query("SELECT * FROM browser_blocked_keywords WHERE child_id = :childId AND is_synced = 0")
    suspend fun getUnsyncedBlockedKeywords(childId: String): List<BrowserBlockedKeywordEntity>

    @Query("UPDATE browser_blocked_keywords SET is_synced = 1 WHERE child_id = :childId AND keyword IN (:keywords)")
    suspend fun markBlockedKeywordsAsSynced(childId: String, keywords: List<String>)

    // --- HISTORY ---
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistory(item: BrowserHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHistoryList(items: List<BrowserHistoryEntity>)

    @Query("SELECT * FROM browser_history WHERE child_id = :childId ORDER BY timestamp DESC LIMIT 100")
    fun observeHistory(childId: String): Flow<List<BrowserHistoryEntity>>

    @Query("SELECT * FROM browser_history WHERE child_id = :childId AND timestamp BETWEEN :startOfDay AND :endOfDay ORDER BY timestamp DESC")
    fun observeHistoryForDate(
        childId: String,
        startOfDay: Long,
        endOfDay: Long
    ): Flow<List<BrowserHistoryEntity>>

    @Query("SELECT * FROM browser_history WHERE child_id = :childId AND is_synced = 0 LIMIT 100")
    suspend fun getUnsyncedHistory(childId: String): List<BrowserHistoryEntity>

    @Query("UPDATE browser_history SET is_synced = 1 WHERE child_id = :childId AND id IN (:ids)")
    suspend fun markHistoryAsSynced(childId: String, ids: List<String>)

    // --- CLEANUP (HARD DELETES) ---
    @Query("DELETE FROM browser_allowed_sites WHERE child_id = :childId AND url IN (:urls)")
    suspend fun hardDeleteAllowedSites(childId: String, urls: List<String>)

    @Query("DELETE FROM browser_blocked_sites WHERE child_id = :childId AND url IN (:urls)")
    suspend fun hardDeleteBlockedSites(childId: String, urls: List<String>)

    @Query("DELETE FROM browser_blocked_keywords WHERE child_id = :childId AND keyword IN (:keywords)")
    suspend fun hardDeleteBlockedKeywords(childId: String, keywords: List<String>)
}