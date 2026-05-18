package com.vahak.parentcontroll.domain.repository

import android.util.Log
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.dao.UsageDao
import com.vahak.parentcontroll.core.data.local.entity.AppUsageRecordEntity
import com.vahak.parentcontroll.core.data.local.entity.DailyUsageEntity
import com.vahak.parentcontroll.data.remote.AppReportResponse
import com.vahak.parentcontroll.data.remote.AppUsageDto
import com.vahak.parentcontroll.data.remote.DailyUsageDto
import com.vahak.parentcontroll.data.remote.GlobalUsageResponse
import com.vahak.parentcontroll.data.remote.UsageApi
import com.vahak.parentcontroll.data.remote.UsageSyncPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

interface UsageRepository {
    fun observeDailyUsage(childId: String, date: LocalDate): Flow<DailyUsageEntity?>
    fun observeAppUsageForDay(childId: String, date: LocalDate): Flow<List<AppUsageRecordEntity>>
    suspend fun syncUnsyncedData(
        activeChildId: String? = null, forcePing: Boolean = false
    ): GlobalUsageResponse?

    suspend fun getDailyUsageReport(childId: String, date: LocalDate): AppReportResponse?
}

class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
    private val usageApi: UsageApi,
    private val sessionManager: SessionManager
) : UsageRepository {

    override fun observeDailyUsage(childId: String, date: LocalDate): Flow<DailyUsageEntity?> {
        return usageDao.observeDailyUsage(childId, date)
    }

    override fun observeAppUsageForDay(
        childId: String, date: LocalDate
    ): Flow<List<AppUsageRecordEntity>> {
        return usageDao.observeAppUsageForDay(childId, date)
    }

    override suspend fun syncUnsyncedData(
        activeChildId: String?, forcePing: Boolean
    ): GlobalUsageResponse? = withContext(Dispatchers.IO) {
        try {
            val deviceId = sessionManager.getOrCreateDeviceId()
            val deviceName = sessionManager.getDeviceName() // 🚀 Fetch device name

            val unsyncedDaily = usageDao.getUnsyncedDailyUsages()
            val unsyncedApps = usageDao.getUnsyncedAppUsages()

            if (unsyncedDaily.isEmpty() && unsyncedApps.isEmpty() && !forcePing) return@withContext null

            val payload =
                UsageSyncPayload(
                    deviceId = deviceId, deviceName = deviceName, // 🚀 Send to server
                    activeChildId = activeChildId, dailyUsages = unsyncedDaily.map {
                        DailyUsageDto(
                            it.childId, it.date.toString(), it.usedSeconds
                        )
                    }, appUsages = unsyncedApps.map {
                        AppUsageDto(
                            it.childId, it.date.toString(), it.packageName, it.usedSeconds
                        )
                    })

            val response = usageApi.syncUsageData(payload)

            if (response.isSuccessful && response.body() != null) {
                val globalData = response.body()!!
                val today = LocalDate.now()

                // 🚀 SAVE SERVER TOTALS TO ROOM (Global Cache)
                globalData.globalDailySeconds.forEach { (childIdStr, globalSecs) ->
                    val rows = usageDao.updateGlobalDailyUsage(childIdStr, today, globalSecs)
                    if (rows == 0) usageDao.insertOrUpdateDailyUsage(
                        DailyUsageEntity(
                            childIdStr, today, 0, System.currentTimeMillis(), true, globalSecs
                        )
                    )
                }

                globalData.globalAppSeconds.forEach { (childIdStr, appMap) ->
                    appMap.forEach { (pkg, globalSecs) ->
                        val rows = usageDao.updateGlobalAppUsage(childIdStr, today, pkg, globalSecs)
                        if (rows == 0) usageDao.insertOrUpdateAppUsages(
                            listOf(
                                AppUsageRecordEntity(
                                    childIdStr, today, pkg, 0, true, globalSecs
                                )
                            )
                        )
                    }
                }
                return@withContext globalData
            } else {
                Log.e("UsageRepo", "❌ Sync failed: HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("UsageRepo", "❌ Network error: ${e.message}")
        }
        return@withContext null
    }

    override suspend fun getDailyUsageReport(childId: String, date: LocalDate): AppReportResponse? =
        withContext(Dispatchers.IO) {
            try {
                val response = usageApi.getUsageReport(childId, date.toString())
                if (response.isSuccessful) {
                    return@withContext response.body()
                }
            } catch (e: Exception) {
                Log.e("UsageRepo", "❌ Failed to fetch report: ${e.message}")
            }
            return@withContext null
        }
}