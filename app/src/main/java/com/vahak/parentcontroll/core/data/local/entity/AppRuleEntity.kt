package com.vahak.parentcontroll.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_rules",
    foreignKeys = [
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // Unique index ensures a child can only have ONE rule per app package
    indices = [Index(value = ["child_id", "package_name"], unique = true)]
)
data class AppRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "package_name")
    val packageName: String, // e.g., "com.mojang.minecraftpe"

    @ColumnInfo(name = "is_allowed")
    val isAllowed: Boolean = false // Default is blocked for safety
)