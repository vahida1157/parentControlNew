package com.vahak.mehrban.di

import android.content.Context
import androidx.room.Room
import com.vahak.mehrban.core.data.local.ParentControlDatabase
import com.vahak.mehrban.core.data.local.dao.AppRuleDao
import com.vahak.mehrban.core.data.local.dao.ChildDao
import com.vahak.mehrban.core.data.local.dao.ChildSettingsDao
import com.vahak.mehrban.core.data.local.dao.CrashLogDao
import com.vahak.mehrban.core.data.local.dao.UsageDao
import com.vahak.mehrban.core.data.local.dao.WebDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ParentControlDatabase {
        return Room.databaseBuilder(
            context, ParentControlDatabase::class.java, "parent_control_db"
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(db: ParentControlDatabase): ChildSettingsDao {
        return db.childSettingsDao
    }

    @Provides
    @Singleton
    fun provideChildDao(db: ParentControlDatabase): ChildDao = db.childDao

    @Provides
    @Singleton
    fun provideUsageDao(db: ParentControlDatabase): UsageDao = db.usageDao

    @Provides
    @Singleton
    fun provideAppRuleDao(db: ParentControlDatabase): AppRuleDao = db.appRuleDao

    @Provides
    @Singleton
    fun provideWebDao(db: ParentControlDatabase): WebDao = db.webDao

    @Provides
    @Singleton
    fun provideCrashLogDao(db: ParentControlDatabase): CrashLogDao = db.crashLogDao
}