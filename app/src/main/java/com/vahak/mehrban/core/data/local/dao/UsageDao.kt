package com.vahak.mehrban.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.mehrban.core.data.local.entity.AppUsageRecordEntity
import com.vahak.mehrban.core.data.local.entity.DailyUsageEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface UsageDao {

    // --- TOTAL DAILY USAGE ---

    @Query("SELECT * FROM daily_usage WHERE child_id = :childId AND date = :date")
    suspend fun getDailyUsage(childId: String, date: LocalDate): DailyUsageEntity?

    @Query("SELECT * FROM daily_usage WHERE child_id = :childId AND date = :date")
    fun observeDailyUsage(childId: String, date: LocalDate): Flow<DailyUsageEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyUsage(usage: DailyUsageEntity)

    // --- PER-APP USAGE ---

    @Query("SELECT * FROM app_usage_records WHERE child_id = :childId AND date = :date ORDER BY used_seconds DESC")
    fun observeAppUsageForDay(childId: String, date: LocalDate): Flow<List<AppUsageRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAppUsages(records: List<AppUsageRecordEntity>)

    // --- TELEMETRY SYNC SUPPORT ---

    @Query("SELECT * FROM daily_usage WHERE is_synced = 0")
    suspend fun getUnsyncedDailyUsages(): List<DailyUsageEntity>

    @Query("SELECT * FROM app_usage_records WHERE is_synced = 0")
    suspend fun getUnsyncedAppUsages(): List<AppUsageRecordEntity>

    @Query("UPDATE daily_usage SET is_synced = 1 WHERE child_id = :childId AND date = :date")
    suspend fun markDailyUsageSynced(childId: String, date: LocalDate)

    @Query("UPDATE app_usage_records SET is_synced = 1 WHERE child_id = :childId AND date = :date AND package_name = :packageName")
    suspend fun markAppUsageSynced(childId: String, date: LocalDate, packageName: String)

    // --- GLOBAL CACHE UPDATES ---
    @Query("UPDATE daily_usage SET global_used_seconds = :globalSeconds, is_synced = 1 WHERE child_id = :childId AND date = :date")
    suspend fun updateGlobalDailyUsage(childId: String, date: LocalDate, globalSeconds: Int): Int

    @Query("UPDATE app_usage_records SET global_used_seconds = :globalSeconds, is_synced = 1 WHERE child_id = :childId AND date = :date AND package_name = :packageName")
    suspend fun updateGlobalAppUsage(childId: String, date: LocalDate, packageName: String, globalSeconds: Int): Int
}