package com.vahak.parentcontroll.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.vahak.parentcontroll.core.data.local.converter.DatabaseConverters
import com.vahak.parentcontroll.core.data.local.dao.ChildDao
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity

@Database(
    entities = [ChildEntity::class, GlobalSettingsEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class ParentControlDatabase : RoomDatabase() {
    abstract val settingsDao: SettingsDao
    abstract val childDao: ChildDao
}