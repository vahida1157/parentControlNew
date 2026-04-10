package com.vahak.parentcontroll.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalTime

@Entity(
    tableName = "global_settings",
    foreignKeys = [
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["child_id"])]
)
data class GlobalSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "child_id")
    val childId: String,

    // --- Theme ---
    @ColumnInfo(name = "is_child_theme_active")
    val isChildThemeActive: Boolean = true,

    // --- Time Limit ---
    @ColumnInfo(name = "is_time_limit_active")
    val isTimeLimitActive: Boolean = false,

    @ColumnInfo(name = "daily_time_limit_mins")
    val dailyTimeLimitMins: Int = 60,

    // --- Bedtime Limit ---
    @ColumnInfo(name = "is_bedtime_active")
    val isBedtimeActive: Boolean = false,

    @ColumnInfo(name = "bedtime_start")
    val bedtimeStart: LocalTime = LocalTime.of(22, 0), // 10:00 PM

    @ColumnInfo(name = "bedtime_end")
    val bedtimeEnd: LocalTime = LocalTime.of(7, 0), // 07:00 AM

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)