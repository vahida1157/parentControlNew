package com.vahak.parentcontroll.di

import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.domain.repository.AuthRepositoryImpl
import com.vahak.parentcontroll.domain.repository.ChildRepository
import com.vahak.parentcontroll.domain.repository.ChildRepositoryImpl
import com.vahak.parentcontroll.domain.repository.SettingsRepository
import com.vahak.parentcontroll.domain.repository.SettingsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChildRepository(
        childRepositoryImpl: ChildRepositoryImpl
    ): ChildRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        settingsRepositoryImpl: SettingsRepositoryImpl
    ): SettingsRepository
}