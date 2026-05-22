package com.vahak.mehrban.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.util.UUID

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "dob")
    val dob: LocalDate,

    @ColumnInfo(name = "gender")
    val gender: Gender,

    @ColumnInfo(name = "avatar_id")
    val avatarId: Int = 0,

    @ColumnInfo(name = "phone")
    val phone: String? = null,

    // --- OFFLINE SYNC FLAGS ---
    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false, // False when created locally, True when server confirms

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false, // Soft delete flag

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)