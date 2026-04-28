package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.parentcontroll.core.data.local.entity.BlockedDomainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebDao {

    @Query("SELECT * FROM blocked_domains WHERE child_id = :childId ORDER BY domain ASC")
    fun observeBlockedDomains(childId: String): Flow<List<BlockedDomainEntity>>

    // Used by VPN packet processor — returns only active blocked domains as plain strings
    @Query("SELECT domain FROM blocked_domains WHERE child_id = :childId AND is_active = 1")
    suspend fun getActiveBlockedDomains(childId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDomain(domain: BlockedDomainEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDomains(domains: List<BlockedDomainEntity>)

    @Query("UPDATE blocked_domains SET is_active = :isActive, updated_at = :updatedAt WHERE id = :id")
    suspend fun setDomainActive(id: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteDomain(domain: BlockedDomainEntity)

    @Query("DELETE FROM blocked_domains WHERE child_id = :childId")
    suspend fun deleteAllForChild(childId: String)
}
