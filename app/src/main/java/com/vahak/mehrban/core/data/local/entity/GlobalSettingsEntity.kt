package com.vahak.mehrban.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(
    tableName = "global_settings", foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )], indices = [Index(value = ["child_id"])]
)
data class GlobalSettingsEntity(
    @PrimaryKey @ColumnInfo(name = "child_id") val childId: String,

    @ColumnInfo(name = "is_child_theme_active") val isChildThemeActive: Boolean = true,
    @ColumnInfo(name = "is_time_limit_active") val isTimeLimitActive: Boolean = false,
    @ColumnInfo(name = "daily_time_limit_mins") val dailyTimeLimitMins: Int = 60,
    @ColumnInfo(name = "is_sleep_time_active") val isSleepTimeActive: Boolean = false,
    @ColumnInfo(name = "sleep_time_start") val sleepTimeStart: LocalTime = LocalTime.of(22, 0),
    @ColumnInfo(name = "sleep_time_end") val sleepTimeEnd: LocalTime = LocalTime.of(7, 0),
    @ColumnInfo(name = "is_site_management_active") val isSiteManagementActive: Boolean = false,

    // --- OFFLINE SYNC FLAGS ---
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = true,

    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)