package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

@Dao
interface SettingsDao {
    @Query("SELECT * FROM global_settings WHERE child_id = :childId")
    fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalSettings(settings: GlobalSettingsEntity)

    // --- Time Limit Updates ---
    @Query("UPDATE global_settings SET is_time_limit_active = :isActive, updated_at = :time WHERE child_id = :childId")
    suspend fun updateTimeLimitToggle(childId: String, isActive: Boolean, time: Long = System.currentTimeMillis())

    @Query("UPDATE global_settings SET daily_time_limit_mins = :minutes, updated_at = :time WHERE child_id = :childId")
    suspend fun updateDailyTimeLimit(childId: String, minutes: Int, time: Long = System.currentTimeMillis())

    // --- NEW: Bedtime Updates ---
    @Query("UPDATE global_settings SET is_bedtime_active = :isActive, updated_at = :time WHERE child_id = :childId")
    suspend fun updateBedtimeToggle(childId: String, isActive: Boolean, time: Long = System.currentTimeMillis())

    // Room automatically converts LocalTime to String here thanks to our TypeConverter!
    @Query("UPDATE global_settings SET bedtime_start = :startTime, bedtime_end = :endTime, updated_at = :time WHERE child_id = :childId")
    suspend fun updateBedtimeSchedule(childId: String, startTime: LocalTime, endTime: LocalTime, time: Long = System.currentTimeMillis())
}