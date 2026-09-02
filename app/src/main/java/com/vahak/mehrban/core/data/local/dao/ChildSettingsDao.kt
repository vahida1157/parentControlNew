package com.vahak.mehrban.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.mehrban.core.data.local.entity.GlobalSettingsEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime

@Dao
interface ChildSettingsDao {
    @Query("SELECT * FROM global_settings WHERE child_id = :childId")
    fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalSettings(settings: GlobalSettingsEntity)

    @Query("UPDATE global_settings SET is_time_limit_active = :isActive, daily_time_limit_mins = :limitMins, is_exercise_reward_enabled = :isRewardEnabled, max_reward_seconds_per_day = :maxRewardSeconds, is_synced = 0, updated_at = :time WHERE child_id = :childId")
    suspend fun updateTimeLimitSettings(
        childId: String,
        isActive: Boolean,
        limitMins: Int,
        isRewardEnabled: Boolean,
        maxRewardSeconds: Int,
        time: Long = System.currentTimeMillis()
    )

    // --- SleepTime Updates (Now flags is_synced = 0) ---
    @Query("UPDATE global_settings SET is_sleep_time_active = :isActive, is_synced = 0, updated_at = :time WHERE child_id = :childId")
    suspend fun updateSleepTimeToggle(
        childId: String, isActive: Boolean, time: Long = System.currentTimeMillis()
    )

    @Query("UPDATE global_settings SET sleep_time_start = :startTime, sleep_time_end = :endTime, is_synced = 0, updated_at = :time WHERE child_id = :childId")
    suspend fun updateSleepTimeSchedule(
        childId: String,
        startTime: LocalTime,
        endTime: LocalTime,
        time: Long = System.currentTimeMillis()
    )

    // --- Site Management Updates (Now flags is_synced = 0) ---
    @Query("UPDATE global_settings SET is_site_management_active = :isActive, is_synced = 0, updated_at = :time WHERE child_id = :childId")
    suspend fun updateSiteManagementToggle(
        childId: String, isActive: Boolean, time: Long = System.currentTimeMillis()
    )

    // --- PRO OFFLINE FIXES ---
    @Query("SELECT * FROM global_settings WHERE is_synced = 0")
    suspend fun getUnsyncedSettings(): List<GlobalSettingsEntity>

    @Query("UPDATE global_settings SET is_synced = 1 WHERE child_id = :childId")
    suspend fun markAsSynced(childId: String)
}