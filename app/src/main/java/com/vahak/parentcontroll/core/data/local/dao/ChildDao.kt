package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    // 1. Standard UI Reads (Ignore soft-deleted)
    @Query("SELECT * FROM children WHERE id = :childId AND is_deleted = 0")
    fun observeChildById(childId: String): Flow<ChildEntity?>

    @Query("SELECT * FROM children WHERE id = :childId AND is_deleted = 0")
    suspend fun getChildById(childId: String): ChildEntity?

    @Query("SELECT * FROM children WHERE is_deleted = 0")
    fun getAllChildren(): Flow<List<ChildEntity>>

    @Upsert
    suspend fun upsertChild(child: ChildEntity)

    @Upsert
    suspend fun upsertChildren(children: List<ChildEntity>)

    // --- PRO OFFLINE FIXES ---

    // Soft delete locally, mark as unsynced so it gets pushed to the server
    @Query("UPDATE children SET is_deleted = 1, is_synced = 0, updated_at = :time WHERE id = :childId")
    suspend fun softDeleteChild(childId: String, time: Long = System.currentTimeMillis())

    @Query("SELECT * FROM children WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedChildren(): List<ChildEntity>

    @Query("SELECT * FROM children WHERE is_deleted = 1")
    suspend fun getPendingDeletedChildren(): List<ChildEntity>

    @Query("UPDATE children SET is_synced = 1 WHERE id = :childId")
    suspend fun markAsSynced(childId: String)

    // Hard delete: Only called AFTER the server confirms it deleted the child
    @Query("DELETE FROM children WHERE id = :childId")
    suspend fun hardDeleteChild(childId: String)
}