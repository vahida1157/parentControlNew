package com.vahak.parentcontroll.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

@Entity(
    tableName = "daily_usage",
    primaryKeys = ["child_id", "date"],
    foreignKeys = [
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["child_id"])]
)
data class DailyUsageEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "date") val date: LocalDate,
    @ColumnInfo(name = "used_seconds") val usedSeconds: Int = 0,
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),

    // 🚀 NEW: Sync tracking
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "global_used_seconds") val globalUsedSeconds: Int = 0,
)