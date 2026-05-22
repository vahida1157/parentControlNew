package com.vahak.mehrban.di

import com.vahak.mehrban.domain.repository.AppRuleRepository
import com.vahak.mehrban.domain.repository.AppRuleRepositoryImpl
import com.vahak.mehrban.domain.repository.AuthRepository
import com.vahak.mehrban.domain.repository.AuthRepositoryImpl
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.domain.repository.ChildRepositoryImpl
import com.vahak.mehrban.domain.repository.ProfileRepository
import com.vahak.mehrban.domain.repository.ProfileRepositoryImpl
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.domain.repository.SettingsRepositoryImpl
import com.vahak.mehrban.domain.repository.UsageRepository
import com.vahak.mehrban.domain.repository.UsageRepositoryImpl
import com.vahak.mehrban.domain.repository.WebRepository
import com.vahak.mehrban.domain.repository.WebRepositoryImpl
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

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        profileRepositoryImpl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAppRuleRepository(
        appRuleRepositoryImpl: AppRuleRepositoryImpl
    ): AppRuleRepository

    @Binds
    @Singleton
    abstract fun bindWebRepository(
        webRepositoryImpl: WebRepositoryImpl
    ): WebRepository

    @Binds
    @Singleton
    abstract fun bindUsageRepository(
        usageRepositoryImpl: UsageRepositoryImpl
    ): UsageRepository
}