package com.vahak.parentcontroll.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "blocked_domains",
    foreignKeys = [
        ForeignKey(
            entity = ChildEntity::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["child_id", "domain"], unique = true)]
)
data class BlockedDomainEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "child_id")
    val childId: String,

    @ColumnInfo(name = "domain")
    val domain: String,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    // --- OFFLINE SYNC FLAGS ---
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = true, // Default true assuming pulled from server

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false, // Soft delete flag

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)