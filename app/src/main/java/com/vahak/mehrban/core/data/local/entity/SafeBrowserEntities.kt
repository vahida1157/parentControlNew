package com.vahak.mehrban.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

enum class FilterMode {
    WHITELIST_ONLY, // Block everything except allowed sites
    BLACKLIST_ONLY, // Allow everything except blocked sites
    DISABLED        // No filtering (search engine safety only)
}

// --- 1. SETTINGS ENTITY ---
@Entity(
    tableName = "browser_settings",
    foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BrowserSettingsEntity(
    @PrimaryKey @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "search_engine") val searchEngine: String = "shaadbin",
    @ColumnInfo(name = "is_cartoon_world_enabled") val isCartoonWorldEnabled: Boolean = true,
    @ColumnInfo(name = "filter_mode") val filterMode: FilterMode = FilterMode.WHITELIST_ONLY,

    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// --- 2. ALLOWED SITES ---
@Entity(
    tableName = "browser_allowed_sites",
    foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["child_id", "url"], unique = true)] // Prevents duplicates
)
data class BrowserAllowedSiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "label") val label: String,

    @ColumnInfo(name = "is_active") val isActive: Boolean = true, // Soft delete for syncing
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// --- 3. BLOCKED SITES ---
@Entity(
    tableName = "browser_blocked_sites",
    foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["child_id", "url"], unique = true)]
)
data class BrowserBlockedSiteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "url") val url: String,

    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// --- 4. BLOCKED KEYWORDS ---
@Entity(
    tableName = "browser_blocked_keywords",
    foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["child_id", "keyword"], unique = true)]
)
data class BrowserBlockedKeywordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "keyword") val keyword: String,

    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "browser_history",
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
data class BrowserHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)

// --- 5. THE UNIFIED PROFILE (For ViewModel/UI use) ---
data class FullBrowserProfile(
    @Embedded val settings: BrowserSettingsEntity,

    // Room automatically fetches all active rules for this child!
    @Relation(parentColumn = "child_id", entityColumn = "child_id")
    val allowedSites: List<BrowserAllowedSiteEntity>,

    @Relation(parentColumn = "child_id", entityColumn = "child_id")
    val blockedSites: List<BrowserBlockedSiteEntity>,

    @Relation(parentColumn = "child_id", entityColumn = "child_id")
    val blockedKeywords: List<BrowserBlockedKeywordEntity>
)