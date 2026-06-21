package com.vahak.mehrban.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.mehrban.core.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY created_at DESC")
    fun observeAllNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT COUNT(*) FROM notifications WHERE is_read = 0")
    fun observeUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET is_read = 1, is_synced = CASE WHEN is_local_only = 1 THEN 1 ELSE 0 END WHERE id = :id")
    suspend fun markAsReadLocally(id: String)

    // Only fetch notifications that came from the server, have been read, but the server doesn't know yet
    @Query("SELECT * FROM notifications WHERE is_read = 1 AND is_local_only = 0 AND is_synced = 0")
    suspend fun getUnsyncedReadNotifications(): List<NotificationEntity>

    @Query("UPDATE notifications SET is_synced = 1 WHERE id IN (:ids)")
    suspend fun markAsSynced(ids: List<String>)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}