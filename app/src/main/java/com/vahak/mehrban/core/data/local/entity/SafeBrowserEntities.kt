package com.vahak.mehrban.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.util.UUID

enum class FilterMode {
    WHITELIST_ONLY, BLACKLIST_ONLY, DISABLED
}

// --- 1. SETTINGS ENTITY ---
@Entity(
    tableName = "browser_settings", foreignKeys = [ForeignKey(
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
    primaryKeys = ["child_id", "url"],
    foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BrowserAllowedSiteEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// --- 3. BLOCKED SITES ---
@Entity(
    tableName = "browser_blocked_sites",
    primaryKeys = ["child_id", "url"],
    foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BrowserBlockedSiteEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// --- 4. BLOCKED KEYWORDS ---
@Entity(
    tableName = "browser_blocked_keywords",
    primaryKeys = ["child_id", "keyword"],
    foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BrowserBlockedKeywordEntity(
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "keyword") val keyword: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

// --- 5. HISTORY (Fixed: UUID) ---
@Entity(
    tableName = "browser_history", foreignKeys = [ForeignKey(
        entity = ChildEntity::class,
        parentColumns = ["id"],
        childColumns = ["child_id"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BrowserHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    @ColumnInfo(name = "child_id") val childId: String,
    @ColumnInfo(name = "url") val url: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "timestamp") val timestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)

// --- 6. THE UNIFIED PROFILE ---
data class FullBrowserProfile(
    @Embedded val settings: BrowserSettingsEntity, @Relation(
        parentColumn = "child_id", entityColumn = "child_id"
    ) val allowedSites: List<BrowserAllowedSiteEntity>, @Relation(
        parentColumn = "child_id", entityColumn = "child_id"
    ) val blockedSites: List<BrowserBlockedSiteEntity>, @Relation(
        parentColumn = "child_id", entityColumn = "child_id"
    ) val blockedKeywords: List<BrowserBlockedKeywordEntity>
)