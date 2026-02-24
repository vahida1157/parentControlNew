package com.vahak.parentcontroll.di

import android.content.Context
import androidx.room.Room
import com.vahak.parentcontroll.core.data.local.ParentControlDatabase
import com.vahak.parentcontroll.core.data.local.dao.ChildDao
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
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
            context,
            ParentControlDatabase::class.java,
            "parent_control_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(db: ParentControlDatabase): SettingsDao {
        return db.settingsDao
    }

    @Provides
    @Singleton
    fun provideChildDao(db: ParentControlDatabase): ChildDao = db.childDao
}