package com.vahak.parentcontroll.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vahak.parentcontroll.core.data.local.converter.DatabaseConverters
import com.vahak.parentcontroll.core.data.local.dao.AppRuleDao
import com.vahak.parentcontroll.core.data.local.dao.ChildDao
import com.vahak.parentcontroll.core.data.local.dao.ChildSettingsDao
import com.vahak.parentcontroll.core.data.local.dao.UsageDao
import com.vahak.parentcontroll.core.data.local.dao.WebDao
import com.vahak.parentcontroll.core.data.local.entity.AppRuleEntity
import com.vahak.parentcontroll.core.data.local.entity.AppUsageRecordEntity
import com.vahak.parentcontroll.core.data.local.entity.BlockedDomainEntity
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.DailyUsageEntity
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity

@Database(
    entities = [
        ChildEntity::class,
        GlobalSettingsEntity::class,
        DailyUsageEntity::class,
        AppRuleEntity::class,
        AppUsageRecordEntity::class,
        BlockedDomainEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class ParentControlDatabase : RoomDatabase() {
    abstract val childSettingsDao: ChildSettingsDao
    abstract val childDao: ChildDao
    abstract val usageDao: UsageDao
    abstract val appRuleDao: AppRuleDao
    abstract val webDao: WebDao
}