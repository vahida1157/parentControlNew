package com.vahak.parentcontroll.domain.repository

import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import javax.inject.Inject

interface SettingsRepository {
    fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?>
    suspend fun updateBedtimeToggle(childId: String, isActive: Boolean)
    suspend fun updateBedtimeSchedule(childId: String, startTime: LocalTime, endTime: LocalTime)
    suspend fun updateSiteManagementToggle(childId: String, isActive: Boolean)
}

class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao
) : SettingsRepository {

    override fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?> {
        return settingsDao.getGlobalSettings(childId)
    }

    override suspend fun updateBedtimeToggle(childId: String, isActive: Boolean) {
        settingsDao.updateBedtimeToggle(childId, isActive)
    }

    override suspend fun updateBedtimeSchedule(
        childId: String, startTime: LocalTime, endTime: LocalTime
    ) {
        settingsDao.updateBedtimeSchedule(childId, startTime, endTime)
    }

    override suspend fun updateSiteManagementToggle(childId: String, isActive: Boolean) {
        settingsDao.updateSiteManagementToggle(childId, isActive)
    }
}