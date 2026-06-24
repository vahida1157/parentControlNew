package com.vahak.mehrban.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.mehrban.core.data.local.entity.BrowserHistoryEntity
import com.vahak.mehrban.core.data.local.entity.BrowserKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserWhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeBrowserDao {

    // --- Whitelist ---
    @Query("SELECT * FROM browser_whitelist WHERE child_id = :childId AND is_active = 1")
    fun observeActiveWhitelist(childId: String): Flow<List<BrowserWhitelistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWhitelistItem(item: BrowserWhitelistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWhitelistItems(items: List<BrowserWhitelistEntity>)

    @Query("SELECT * FROM browser_whitelist WHERE child_id = :childId AND is_synced = 0")
    suspend fun getUnsyncedWhitelist(childId: String): List<BrowserWhitelistEntity>

    @Query("UPDATE browser_whitelist SET is_synced = 1 WHERE child_id = :childId AND url_prefix IN (:urls)")
    suspend fun markWhitelistAsSynced(childId: String, urls: List<String>)

    // --- Keywords ---
    @Query("SELECT * FROM browser_keywords WHERE child_id = :childId AND is_active = 1")
    fun observeActiveKeywords(childId: String): Flow<List<BrowserKeywordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKeyword(item: BrowserKeywordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKeywords(items: List<BrowserKeywordEntity>)

    @Query("SELECT * FROM browser_keywords WHERE child_id = :childId AND is_synced = 0")
    suspend fun getUnsyncedKeywords(childId: String): List<BrowserKeywordEntity>

    @Query("UPDATE browser_keywords SET is_synced = 1 WHERE child_id = :childId AND keyword IN (:keywords)")
    suspend fun markKeywordsAsSynced(childId: String, keywords: List<String>)

    // --- History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: BrowserHistoryEntity)

    @Query("SELECT * FROM browser_history WHERE child_id = :childId AND is_synced = 0 LIMIT 100")
    suspend fun getUnsyncedHistory(childId: String): List<BrowserHistoryEntity>

    @Query("UPDATE browser_history SET is_synced = 1 WHERE child_id = :childId AND id IN (:ids)")
    suspend fun markHistoryAsSynced(childId: String, ids: List<Long>)
    
    // Cleanup old history locally to save space
    @Query("DELETE FROM browser_history WHERE child_id = :childId AND is_synced = 1 AND timestamp < :olderThanTime")
    suspend fun deleteOldSyncedHistory(childId: String, olderThanTime: Long)
}