package com.vahak.mehrban.di

import com.vahak.mehrban.BuildConfig
import com.vahak.mehrban.data.remote.AppUpdateApi
import com.vahak.mehrban.data.remote.ApplicationCrashApi
import com.vahak.mehrban.data.remote.AuthApi
import com.vahak.mehrban.data.remote.ChildApi
import com.vahak.mehrban.data.remote.NotificationApi
import com.vahak.mehrban.data.remote.ProfileApi
import com.vahak.mehrban.data.remote.RuleApi
import com.vahak.mehrban.data.remote.SettingsApi
import com.vahak.mehrban.data.remote.UsageApi
import com.vahak.mehrban.data.remote.WebApi
import com.vahak.mehrban.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Only log full body in Debug mode for security
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BODY
            }
        }

        return OkHttpClient.Builder().addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor).build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // 🚀 Automatically uses IP for Debug, and HTTPS Domain for Release
            .baseUrl(BuildConfig.BASE_URL).client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()).build()
    }

    // ==========================================
    // APIS
    // Note: Ensure your API paths in these interfaces start with
    // the Nginx routing prefix (e.g., @POST("api/identity/login"))
    // ==========================================

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)

    @Provides
    @Singleton
    fun provideChildApi(retrofit: Retrofit): ChildApi = retrofit.create(ChildApi::class.java)

    @Provides
    @Singleton
    fun provideSettingsApi(retrofit: Retrofit): SettingsApi =
        retrofit.create(SettingsApi::class.java)

    @Provides
    @Singleton
    fun provideRuleApi(retrofit: Retrofit): RuleApi = retrofit.create(RuleApi::class.java)

    @Provides
    @Singleton
    fun provideWebApi(retrofit: Retrofit): WebApi = retrofit.create(WebApi::class.java)

    @Provides
    @Singleton
    fun provideUsageApi(retrofit: Retrofit): UsageApi = retrofit.create(UsageApi::class.java)

    @Provides
    @Singleton
    fun provideAppUpdateApi(retrofit: Retrofit): AppUpdateApi =
        retrofit.create(AppUpdateApi::class.java)

    @Provides
    @Singleton
    fun provideApplicationCrashApi(retrofit: Retrofit): ApplicationCrashApi =
        retrofit.create(ApplicationCrashApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi =
        retrofit.create(NotificationApi::class.java)
}