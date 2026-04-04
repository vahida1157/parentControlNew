package com.vahak.parentcontroll.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

@Entity(
    tableName = "app_usage_records",
    // Composite Primary Key: One record per child, per day, per app!
    primaryKeys = ["child_id", "date", "package_name"],
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
data class AppUsageRecordEntity(
    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "date")
    val date: LocalDate,

    @ColumnInfo(name = "package_name")
    val packageName: String, // e.g., "com.mojang.minecraftpe"

    @ColumnInfo(name = "used_seconds")
    val usedSeconds: Int = 0
)