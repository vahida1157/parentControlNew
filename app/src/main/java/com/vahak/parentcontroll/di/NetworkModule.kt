package com.vahak.parentcontroll.di

import com.vahak.parentcontroll.data.remote.AuthApi
import com.vahak.parentcontroll.data.remote.ChildApi
import com.vahak.parentcontroll.data.remote.ProfileApi
import com.vahak.parentcontroll.data.remote.interceptor.AuthInterceptor
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

    /**
     * IMPORTANT BASE URL RULES:
     * 1. If testing on Android Studio Emulator: Use "http://10.0.2.2:8080/"
     * 2. If testing on a Physical Device via Wi-Fi: Use your PC's local IP (e.g., "http://192.168.1.15:8080/")
     */
//    private const val BASE_URL = "http://10.0.2.2:8080/"
//    private const val BASE_URL = "http://192.168.0.83:8080/"
    private const val BASE_URL = "http://10.240.183.10:8080/"

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        // This interceptor prints all network requests/responses to Logcat. 
        // It is incredibly helpful for debugging backend connections.
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create()) // Parses JSON to Kotlin Data Classes
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideProfileApi(retrofit: Retrofit): ProfileApi {
        return retrofit.create(ProfileApi::class.java)
    }

    @Provides
    @Singleton
    fun provideChildApi(retrofit: Retrofit): ChildApi {
        return retrofit.create(ChildApi::class.java)
    }
}