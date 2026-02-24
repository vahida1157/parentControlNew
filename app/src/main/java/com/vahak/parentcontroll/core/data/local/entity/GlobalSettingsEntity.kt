package com.vahak.parentcontroll.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

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

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)