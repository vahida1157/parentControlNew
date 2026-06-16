package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.dao.UsageDao
import com.vahak.mehrban.core.data.local.entity.AppUsageRecordEntity
import com.vahak.mehrban.core.data.local.entity.DailyUsageEntity
import com.vahak.mehrban.data.remote.AppReportResponse
import com.vahak.mehrban.data.remote.AppUsageDto
import com.vahak.mehrban.data.remote.DailyUsageDto
import com.vahak.mehrban.data.remote.GlobalUsageResponse
import com.vahak.mehrban.data.remote.UsageApi
import com.vahak.mehrban.data.remote.UsageSyncPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

interface UsageRepository {
    fun observeDailyUsage(childId: String, date: LocalDate): Flow<DailyUsageEntity?>
    fun observeAppUsageForDay(childId: String, date: LocalDate): Flow<List<AppUsageRecordEntity>>
    suspend fun syncUnsyncedData(
        activeChildId: String? = null,
        forcePing: Boolean = false
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
        childId: String,
        date: LocalDate
    ): Flow<List<AppUsageRecordEntity>> {
        return usageDao.observeAppUsageForDay(childId, date)
    }

    override suspend fun syncUnsyncedData(
        activeChildId: String?, forcePing: Boolean
    ): GlobalUsageResponse? = withContext(Dispatchers.IO) {
        try {
            Timber.d("Initiating usage data synchronization, forcePing: %b", forcePing)
            val deviceId = sessionManager.getOrCreateDeviceId()
            val deviceName = sessionManager.getDeviceName()

            val unsyncedDaily = usageDao.getUnsyncedDailyUsages()
            val unsyncedApps = usageDao.getUnsyncedAppUsages()

            if (unsyncedDaily.isEmpty() && unsyncedApps.isEmpty() && !forcePing) {
                Timber.d("No unsynced usage data found, skipping synchronization")
                return@withContext null
            }

            val payload = UsageSyncPayload(
                deviceId = deviceId,
                deviceName = deviceName,
                activeChildId = activeChildId,
                dailyUsages = unsyncedDaily.map {
                    DailyUsageDto(it.childId, it.date.toString(), it.usedSeconds)
                },
                appUsages = unsyncedApps.map {
                    AppUsageDto(it.childId, it.date.toString(), it.packageName, it.usedSeconds)
                }
            )

            Timber.d(
                "Pushing usage data payload to server, dailyRecords: %d, appRecords: %d",
                unsyncedDaily.size,
                unsyncedApps.size
            )
            val response = usageApi.syncUsageData(payload)

            if (response.isSuccessful && response.body() != null) {
                val globalData = response.body()!!
                val today = LocalDate.now()

                Timber.d("Updating global usage cache locally")
                globalData.globalDailySeconds.forEach { (childIdStr, globalSecs) ->
                    val rows = usageDao.updateGlobalDailyUsage(childIdStr, today, globalSecs)
                    if (rows == 0) usageDao.insertOrUpdateDailyUsage(
                        DailyUsageEntity(
                            childIdStr,
                            today,
                            0,
                            System.currentTimeMillis(),
                            true,
                            globalSecs
                        )
                    )
                }

                globalData.globalAppSeconds.forEach { (childIdStr, appMap) ->
                    appMap.forEach { (pkg, globalSecs) ->
                        val rows = usageDao.updateGlobalAppUsage(childIdStr, today, pkg, globalSecs)
                        if (rows == 0) usageDao.insertOrUpdateAppUsages(
                            listOf(
                                AppUsageRecordEntity(
                                    childIdStr,
                                    today,
                                    pkg,
                                    0,
                                    true,
                                    globalSecs
                                )
                            )
                        )
                    }
                }
                Timber.i("Usage data synchronized successfully")
                return@withContext globalData
            } else {
                Timber.w("Failed to synchronize usage data, HTTP status: %d", response.code())
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error during usage data synchronization")
        }
        return@withContext null
    }

    override suspend fun getDailyUsageReport(childId: String, date: LocalDate): AppReportResponse? =
        withContext(Dispatchers.IO) {
            try {
                Timber.d("Fetching daily usage report from server")
                val response = usageApi.getUsageReport(childId, date.toString())
                if (response.isSuccessful) {
                    Timber.i("Daily usage report fetched successfully")
                    return@withContext response.body()
                } else {
                    Timber.w("Failed to fetch daily usage report, HTTP status: %d", response.code())
                }
            } catch (e: Exception) {
                Timber.w(e, "Network error fetching daily usage report")
            }
            return@withContext null
        }
}