package com.vahak.mehrban.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class NotificationDto(
    val id: String,
    val childId: String?,
    val title: String,
    val message: String,
    val type: String,
    val isRead: Boolean,
    val createdAt: Long
)

data class SyncReadStatusRequest(
    val readNotificationIds: List<String>
)

interface NotificationApi {
    @GET("api/identity/v1/notifications")
    suspend fun getNotifications(): Response<List<NotificationDto>>

    @POST("api/identity/v1/notifications/sync-read")
    suspend fun pushReadStatus(@Body request: SyncReadStatusRequest): Response<Unit>
}