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
        indices = [Index(value = ["child_id", "package_name"], unique = true)]
    )
    data class AppRuleEntity(
        @PrimaryKey(autoGenerate = true)
        val id: Long = 0,

        @ColumnInfo(name = "child_id")
        val childId: String,

        @ColumnInfo(name = "package_name")
        val packageName: String,

        @ColumnInfo(name = "is_allowed")
        val isAllowed: Boolean = false,

        // --- PRO OFFLINE FIX ---
        @ColumnInfo(name = "is_synced")
        val isSynced: Boolean = true,

        @ColumnInfo(name = "updated_at")
        val updatedAt: Long = System.currentTimeMillis()
    )