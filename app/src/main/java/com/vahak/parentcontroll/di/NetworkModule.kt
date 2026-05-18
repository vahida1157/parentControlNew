package com.vahak.parentcontroll.di

import com.vahak.parentcontroll.data.remote.AuthApi
import com.vahak.parentcontroll.data.remote.ChildApi
import com.vahak.parentcontroll.data.remote.ProfileApi
import com.vahak.parentcontroll.data.remote.RuleApi
import com.vahak.parentcontroll.data.remote.SettingsApi
import com.vahak.parentcontroll.data.remote.UsageApi
import com.vahak.parentcontroll.data.remote.WebApi
import com.vahak.parentcontroll.data.remote.interceptor.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // Centralize your testing IP here so you only have to change it in one place!
//    private const val HOST_IP = "192.168.0.83"
    private const val HOST_IP = "62.60.191.233"

    private const val IDENTITY_BASE_URL = "http://$HOST_IP:8080/"
    private const val POLICY_BASE_URL = "http://$HOST_IP:8081/"
    private const val TELEMETRY_BASE_URL = "http://$HOST_IP:8082/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    // --- 1. IDENTITY RETROFIT (Port 8080) ---
    @Provides
    @Singleton
    @Named("IdentityRetrofit")
    fun provideIdentityRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(IDENTITY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // --- 2. POLICY RETROFIT (Port 8081) ---
    @Provides
    @Singleton
    @Named("PolicyRetrofit")
    fun providePolicyRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(POLICY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    // --- 3. TELEMETRY RETROFIT (Port 8082) ---
    @Provides
    @Singleton
    @Named("TelemetryRetrofit")
    fun provideTelemetryRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TELEMETRY_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ==========================================
    // IDENTITY APIS
    // ==========================================

    @Provides
    @Singleton
    fun provideAuthApi(@Named("IdentityRetrofit") retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApi(@Named("IdentityRetrofit") retrofit: Retrofit): ProfileApi {
        return retrofit.create(ProfileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChildApi(@Named("IdentityRetrofit") retrofit: Retrofit): ChildApi {
        return retrofit.create(ChildApi::class.java)
    }

    // ==========================================
    // POLICY APIS
    // ==========================================

    @Provides
    @Singleton
    fun provideSettingsApi(@Named("PolicyRetrofit") retrofit: Retrofit): SettingsApi {
        return retrofit.create(SettingsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRuleApi(@Named("PolicyRetrofit") retrofit: Retrofit): RuleApi {
        return retrofit.create(RuleApi::class.java)
    }

    @Provides
    @Singleton
    fun provideWebApi(@Named("PolicyRetrofit") retrofit: Retrofit): WebApi {
        return retrofit.create(WebApi::class.java)
    }

    // ==========================================
    // TELEMETRY APIS
    // ==========================================

    @Provides
    @Singleton
    fun provideUsageApi(@Named("TelemetryRetrofit") retrofit: Retrofit): UsageApi {
        return retrofit.create(UsageApi::class.java)
    }
}