package com.vahak.mehrban.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vahak.mehrban.core.data.local.converter.DatabaseConverters
import com.vahak.mehrban.core.data.local.dao.*
import com.vahak.mehrban.core.data.local.entity.*

@Database(
    entities = [
        ChildEntity::class,
        GlobalSettingsEntity::class,
        DailyUsageEntity::class,
        AppRuleEntity::class,
        AppUsageRecordEntity::class,
        BlockedDomainEntity::class,
        CrashLogEntity::class,
        NotificationEntity::class,
        BrowserSettingsEntity::class,
        BrowserAllowedSiteEntity::class,
        BrowserBlockedSiteEntity::class,
        BrowserBlockedKeywordEntity::class,
        BrowserHistoryEntity::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class ParentControlDatabase : RoomDatabase() {
    abstract val childSettingsDao: ChildSettingsDao
    abstract val childDao: ChildDao
    abstract val usageDao: UsageDao
    abstract val appRuleDao: AppRuleDao
    abstract val webDao: WebDao
    abstract val crashLogDao: CrashLogDao
    abstract val notificationDao: NotificationDao
    abstract val safeBrowserDao: SafeBrowserDao
}