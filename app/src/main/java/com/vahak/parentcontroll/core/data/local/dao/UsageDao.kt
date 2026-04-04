package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.parentcontroll.core.data.local.entity.AppUsageRecordEntity
import com.vahak.parentcontroll.core.data.local.entity.DailyUsageEntity
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
}