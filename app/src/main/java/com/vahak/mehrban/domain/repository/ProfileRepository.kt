package com.vahak.mehrban.domain.repository

import android.content.Context
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.data.remote.ProfileApi
import com.vahak.mehrban.data.remote.SetupSecurityRequestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

interface ProfileRepository {
    suspend fun setupSecurity(pin: String, question: String, answer: String): Result<Unit>
}

class ProfileRepositoryImpl @Inject constructor(
    private val profileApi: ProfileApi,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context
) : ProfileRepository {

    

    override suspend fun setupSecurity(pin: String, question: String, answer: String): Result<Unit> {
        return try {
            Timber.d("Initiating security profile setup")
            val request = SetupSecurityRequestDto(pin, question, answer)
            val response = profileApi.setupSecurity(request)

            if (response.isSuccessful) {
                Timber.d("Saving security profile configuration locally")
                sessionManager.setParentPin(pin)
                sessionManager.setSecurityData(question, answer)
                Timber.i("Security profile configured successfully")
                Result.success(Unit)
            } else {
                Timber.w("Failed to configure security profile on server, HTTP status: %d", response.code())
                Result.failure(Exception(context.getString(R.string.error_saving_data_server)))
            }
        } catch (e: Exception) {
            Timber.w(e,"Network error during security profile configuration")
            Result.failure(Exception(context.getString(R.string.error_network_check_internet)))
        }
    }
}