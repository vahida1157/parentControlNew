package com.vahak.parentcontroll.domain.repository

import android.util.Log
import com.vahak.parentcontroll.core.data.local.dao.ChildSettingsDao
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import com.vahak.parentcontroll.data.remote.SettingsApi
import com.vahak.parentcontroll.data.remote.UpdateSettingsRequestDto
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import javax.inject.Inject

interface SettingsRepository {
    fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?>

    // NEW: Time Limit Update Function
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

    // --- LOCAL WRITES THAT TRIGGER THE SYNC ENGINE ---

    override suspend fun updateTimeLimit(childId: String, isActive: Boolean, limitMins: Int) {
        childSettingsDao.updateTimeLimitToggle(childId, isActive) // Sets is_synced = 0
        childSettingsDao.updateDailyTimeLimit(childId, limitMins) // Sets is_synced = 0
        pushDirtySettings(childId)
    }

    override suspend fun updateSleepTimeToggle(childId: String, isActive: Boolean) {
        childSettingsDao.updateSleepTimeToggle(childId, isActive) // Sets is_synced = 0
        pushDirtySettings(childId)
    }

    override suspend fun updateSleepTimeSchedule(childId: String, startTime: LocalTime, endTime: LocalTime) {
        childSettingsDao.updateSleepTimeSchedule(childId, startTime, endTime) // Sets is_synced = 0
        pushDirtySettings(childId)
    }

    override suspend fun updateSiteManagementToggle(childId: String, isActive: Boolean) {
        childSettingsDao.updateSiteManagementToggle(childId, isActive) // Sets is_synced = 0
        pushDirtySettings(childId)
    }

    // --- PRO OFFLINE FIX: The Sync Engine ---

    private suspend fun pushDirtySettings(childId: String) {
        try {
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
                updatedAt = dirtySettings.updatedAt // CRITICAL FIX: Sent to Spring Boot for Last-Write-Wins
            )

            val response = settingsApi.updateChildSettings(childId, request)

            if (response.isSuccessful) {
                childSettingsDao.markAsSynced(childId)
            }
        } catch (e: Exception) {
            Log.e("SettingsRepo", "Airplane mode. Settings remain dirty for later sync.")
        }
    }

    override suspend fun syncSettingsFromServer(childId: String): Result<Unit> {
        // ALWAYS push offline changes first to prevent server overwrite!
        pushDirtySettings(childId)

        return try {
            val response = settingsApi.getChildSettings(childId)
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!

                // Double check it isn't STILL dirty
                val unsyncedList = childSettingsDao.getUnsyncedSettings()
                if (unsyncedList.any { it.childId == childId }) {
                    return Result.success(Unit) // Skip overwrite! Parent's offline changes win.
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
                    isSynced = true // Fresh from server
                )

                childSettingsDao.upsertGlobalSettings(serverSettings)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to pull settings"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Network error pulling settings"))
        }
    }
}