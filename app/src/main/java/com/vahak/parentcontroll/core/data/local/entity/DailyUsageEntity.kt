package com.vahak.parentcontroll.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.time.LocalDate

@Entity(
    tableName = "daily_usage",
    primaryKeys = ["child_id", "date"], // Composite key: One record per child, per day
    foreignKeys = [
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE // If child is deleted, delete their usage history
        )
    ],
    indices = [Index(value = ["child_id"])]
)
data class DailyUsageEntity(
    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "date")
    val date: LocalDate, // Your converter will handle this perfectly

    @ColumnInfo(name = "used_seconds")
    val usedSeconds: Int = 0,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)