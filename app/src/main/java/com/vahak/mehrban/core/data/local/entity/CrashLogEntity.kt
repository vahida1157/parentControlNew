package com.vahak.mehrban.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "crash_logs")
data class CrashLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String,
    val androidVersion: String,
    val deviceModel: String,
    val exceptionType: String,
    val stackTrace: String
)