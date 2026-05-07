package com.vahak.parentcontroll.data.remote.interceptor

import com.vahak.parentcontroll.core.data.local.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Fetch the token synchronously from DataStore
        // This is safe because Retrofit already runs network requests on a background thread.
        val token = runBlocking { sessionManager.authToken.first() }

        // 2. Get the original request
        val originalRequest = chain.request()

        // 3. If we don't have a token, just proceed normally (e.g., for Login/OTP endpoints)
        if (token.isNullOrEmpty()) {
            return chain.proceed(originalRequest)
        }

        // 4. Attach the Bearer token to the Authorization header
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()

        // 5. Fire the request
        return chain.proceed(authenticatedRequest)
    }
}