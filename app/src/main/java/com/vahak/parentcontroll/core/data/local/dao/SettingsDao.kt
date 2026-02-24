package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    // 1. Reactive Read: The UI observes this
    @Query("SELECT * FROM global_settings WHERE child_id = :childId")
    fun getGlobalSettings(childId: String): Flow<GlobalSettingsEntity?>

    // 2. Create or Fully Replace
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGlobalSettings(settings: GlobalSettingsEntity)

    // 3. Partial Updates (Pro optimization: Don't rewrite the whole row for one toggle)
    @Query("UPDATE global_settings SET is_time_limit_active = :isActive, updated_at = :time WHERE child_id = :childId")
    suspend fun updateTimeLimitToggle(childId: String, isActive: Boolean, time: Long = System.currentTimeMillis())

    @Query("UPDATE global_settings SET daily_time_limit_mins = :minutes, updated_at = :time WHERE child_id = :childId")
    suspend fun updateDailyTimeLimit(childId: String, minutes: Int, time: Long = System.currentTimeMillis())
}