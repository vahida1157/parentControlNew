package com.vahak.mehrban.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.mehrban.core.data.local.entity.CrashLogEntity

@Dao
interface CrashLogDao {
    // 🚀 We need a synchronous insert because during a fatal crash, coroutines might die
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCrashLogSync(crashLog: CrashLogEntity)

    @Query("SELECT * FROM crash_logs")
    suspend fun getAllCrashes(): List<CrashLogEntity>

    @Query("DELETE FROM crash_logs WHERE id IN (:ids)")
    suspend fun deleteCrashes(ids: List<String>)
}