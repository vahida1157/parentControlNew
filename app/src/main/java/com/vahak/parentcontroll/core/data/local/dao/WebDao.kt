package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.parentcontroll.core.data.local.entity.BlockedDomainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDao {

    // 1. UI Reads (Ignore soft-deleted)
    @Query("SELECT * FROM blocked_domains WHERE child_id = :childId AND is_deleted = 0 ORDER BY domain ASC")
    fun observeBlockedDomains(childId: String): Flow<List<BlockedDomainEntity>>

    // 2. VPN Processor Reads (Ignore soft-deleted)
    @Query("SELECT domain FROM blocked_domains WHERE child_id = :childId AND is_active = 1 AND is_deleted = 0")
    suspend fun getActiveBlockedDomains(childId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDomain(domain: BlockedDomainEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDomains(domains: List<BlockedDomainEntity>)

    // When toggling a domain on/off, mark it as dirty
    @Query("UPDATE blocked_domains SET is_active = :isActive, is_synced = 0, updated_at = :updatedAt WHERE id = :id")
    suspend fun setDomainActive(id: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    // --- PRO OFFLINE FIXES ---

    // Soft delete: hides from UI and marks for server sync
    @Query("UPDATE blocked_domains SET is_deleted = 1, is_synced = 0, updated_at = :time WHERE id = :id")
    suspend fun softDeleteDomain(id: String, time: Long = System.currentTimeMillis())

    @Query("SELECT * FROM blocked_domains WHERE child_id = :childId AND is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedDomains(childId: String): List<BlockedDomainEntity>

    @Query("SELECT * FROM blocked_domains WHERE child_id = :childId AND is_deleted = 1")
    suspend fun getPendingDeletedDomains(childId: String): List<BlockedDomainEntity>

    @Query("UPDATE blocked_domains SET is_synced = 1 WHERE id IN (:domainIds)")
    suspend fun markAsSynced(domainIds: List<String>)

    // Hard delete: Only called AFTER the server confirms deletion
    @Query("DELETE FROM blocked_domains WHERE id = :id")
    suspend fun hardDeleteDomain(id: String)

    // Clear all for a child (used if the child profile is deleted)
    @Query("DELETE FROM blocked_domains WHERE child_id = :childId")
    suspend fun deleteAllForChild(childId: String)
}