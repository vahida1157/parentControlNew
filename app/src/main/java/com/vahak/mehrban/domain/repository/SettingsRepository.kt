package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.dao.ChildSettingsDao
import com.vahak.mehrban.core.data.local.entity.GlobalSettingsEntity
import com.vahak.mehrban.data.remote.SettingsApi
import com.vahak.mehrban.data.remote.UpdateSettingsRequestDto
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalTime
import javax.inject.Inject

interface SettingsRepository {
    fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?>
    suspend fun updateTimeLimit(childId: String, isActive: Boolean, limitMins: Int)
    suspend fun updateSleepTimeToggle(childId: String, isActive: Boolean)
    suspend fun updateSleepTimeSchedule(childId: String, startTime: LocalTime, endTime: LocalTime)
    suspend fun updateSiteManagementToggle(childId: String, isActive: Boolean)
    suspend fun syncSettingsFromServer(childId: String): Result<Unit>
}

class SettingsRepositoryImpl @Inject constructor(
    private val childSettingsDao: ChildSettingsDao,
    private val settingsApi: SettingsApi
) : SettingsRepository {

    

    override fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?> {
        return childSettingsDao.getGlobalSettings(childId)
    }

    override suspend fun updateTimeLimit(childId: String, isActive: Boolean, limitMins: Int) {
        Timber.d("Updating time limit settings locally, isActive: %b, limitMins: %d", isActive, limitMins)
        childSettingsDao.updateTimeLimitToggle(childId, isActive)
        childSettingsDao.updateDailyTimeLimit(childId, limitMins)
        Timber.i("Time limit settings updated locally")
        pushDirtySettings(childId)
    }

    override suspend fun updateSleepTimeToggle(childId: String, isActive: Boolean) {
        Timber.d("Updating sleep time toggle locally, isActive: %b", isActive)
        childSettingsDao.updateSleepTimeToggle(childId, isActive)
        Timber.i("Sleep time toggle updated locally")
        pushDirtySettings(childId)
    }

    override suspend fun updateSleepTimeSchedule(childId: String, startTime: LocalTime, endTime: LocalTime) {
        Timber.d("Updating sleep time schedule locally")
        childSettingsDao.updateSleepTimeSchedule(childId, startTime, endTime)
        Timber.i("Sleep time schedule updated locally")
        pushDirtySettings(childId)
    }

    override suspend fun updateSiteManagementToggle(childId: String, isActive: Boolean) {
        Timber.d("Updating site management toggle locally, isActive: %b", isActive)
        childSettingsDao.updateSiteManagementToggle(childId, isActive)
        Timber.i("Site management toggle updated locally")
        pushDirtySettings(childId)
    }

    private suspend fun pushDirtySettings(childId: String) {
        try {
            Timber.d("Pushing dirty child settings to server")
            val unsyncedList = childSettingsDao.getUnsyncedSettings()
            val dirtySettings = unsyncedList.find { it.childId == childId } ?: return

            val request = UpdateSettingsRequestDto(
                isChildThemeActive = dirtySettings.isChildThemeActive,
                isTimeLimitActive = dirtySettings.isTimeLimitActive,
                dailyTimeLimitMins = dirtySettings.dailyTimeLimitMins,
                isSleepTimeActive = dirtySettings.isSleepTimeActive,
                sleepTimeStart = dirtySettings.sleepTimeStart.toString(),
                sleepTimeEnd = dirtySettings.sleepTimeEnd.toString(),
                isSiteManagementActive = dirtySettings.isSiteManagementActive,
                updatedAt = dirtySettings.updatedAt
            )

            val response = settingsApi.updateChildSettings(childId, request)

            if (response.isSuccessful) {
                childSettingsDao.markAsSynced(childId)
                Timber.i("Child settings pushed to server successfully")
            } else {
                Timber.w("Failed to push child settings, HTTP status: %d", response.code())
            }
        } catch (e: Exception) {
            Timber.w(e,"Network error while pushing settings, maintaining local dirty state")
        }
    }

    override suspend fun syncSettingsFromServer(childId: String): Result<Unit> {
        pushDirtySettings(childId)

        return try {
            Timber.d("Initiating child settings synchronization from server")
            val response = settingsApi.getChildSettings(childId)

            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val unsyncedList = childSettingsDao.getUnsyncedSettings()

                if (unsyncedList.any { it.childId == childId }) {
                    Timber.d("Local unsynced settings present, bypassing server overwrite to maintain integrity")
                    return Result.success(Unit)
                }

                val serverSettings = GlobalSettingsEntity(
                    childId = childId,
                    isChildThemeActive = dto.isChildThemeActive,
                    isTimeLimitActive = dto.isTimeLimitActive,
                    dailyTimeLimitMins = dto.dailyTimeLimitMins,
                    isSleepTimeActive = dto.isSleepTimeActive,
                    sleepTimeStart = LocalTime.parse(dto.sleepTimeStart),
                    sleepTimeEnd = LocalTime.parse(dto.sleepTimeEnd),
                    isSiteManagementActive = dto.isSiteManagementActive,
                    isSynced = true
                )

                Timber.d("Upserting synchronized child settings locally")
                childSettingsDao.upsertGlobalSettings(serverSettings)
                Timber.i("Child settings synchronized successfully")
                Result.success(Unit)
            } else {
                Timber.w("Failed to pull child settings from server, HTTP status: %d", response.code())
                Result.failure(Exception("Failed to pull settings"))
            }
        } catch (e: Exception) {
            Timber.w(e,"Network error during child settings synchronization")
            Result.failure(Exception("Network error pulling settings"))
        }
    }
}