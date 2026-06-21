package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.dao.NotificationDao
import com.vahak.mehrban.core.data.local.entity.NotificationEntity
import com.vahak.mehrban.core.data.local.entity.NotificationType
import com.vahak.mehrban.data.remote.NotificationApi
import com.vahak.mehrban.data.remote.SyncReadStatusRequest
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface NotificationRepository {
    fun observeAllNotifications(): Flow<List<NotificationEntity>>
    fun observeUnreadCount(): Flow<Int>
    suspend fun insertLocalNotification(childId: String?, title: String, message: String, type: NotificationType)
    suspend fun markAsRead(id: String)
    suspend fun syncNotificationsFromServer(): Result<Unit>
    suspend fun pushReadStatusToServer(): Result<Unit>
}

class NotificationRepositoryImpl @Inject constructor(
    private val dao: NotificationDao,
    private val api: NotificationApi
) : NotificationRepository {

    override fun observeAllNotifications() = dao.observeAllNotifications()
    override fun observeUnreadCount() = dao.observeUnreadCount()

    // 🚀 Use this in your Android Workers to generate Time Limit / Offline alerts
    override suspend fun insertLocalNotification(childId: String?, title: String, message: String, type: NotificationType) {
        val localNotification = NotificationEntity(
            childId = childId,
            title = title,
            message = message,
            type = type,
            isLocalOnly = true,
            isSynced = true // Local notifications don't need to sync to the backend
        )
        dao.upsertNotifications(listOf(localNotification))
    }

    override suspend fun markAsRead(id: String) {
        Timber.d("Marking notification as read locally: %s", id)
        dao.markAsReadLocally(id)
        pushReadStatusToServer() // Attempt to inform the backend immediately
    }

    override suspend fun pushReadStatusToServer(): Result<Unit> {
        return try {
            val unsynced = dao.getUnsyncedReadNotifications()
            if (unsynced.isEmpty()) return Result.success(Unit)

            val remoteIds = unsynced.map { it.id }
            Timber.d("Pushing read status for %d notifications", remoteIds.size)

            val response = api.pushReadStatus(SyncReadStatusRequest(remoteIds))
            if (response.isSuccessful) {
                dao.markAsSynced(remoteIds)
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to push read statuses")
            Result.failure(e)
        }
    }

    override suspend fun syncNotificationsFromServer(): Result<Unit> {
        pushReadStatusToServer()

        return try {
            Timber.d("Fetching latest notifications from server")
            val response = api.getNotifications()

            if (response.isSuccessful && response.body() != null) {
                val entities = response.body()!!.map { dto ->
                    NotificationEntity(
                        id = dto.id,
                        childId = dto.childId,
                        title = dto.title,
                        message = dto.message,
                        type = NotificationType.valueOf(dto.type),
                        isRead = dto.isRead,
                        createdAt = dto.createdAt,
                        isLocalOnly = false,
                        isSynced = true
                    )
                }
                dao.upsertNotifications(entities)
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error syncing notifications")
            Result.failure(e)
        }
    }
}