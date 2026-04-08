package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    // 1. Reactive: Best for UI (Automatically updates if name changes)
    @Query("SELECT * FROM children WHERE id = :childId")
    fun observeChildById(childId: String): Flow<ChildEntity?>

    // 2. One-Shot: Best for background tasks or simple checks
    @Query("SELECT * FROM children WHERE id = :childId")
    suspend fun getChildById(childId: String): ChildEntity?

    @Query("SELECT * FROM children")
    fun getAllChildren(): Flow<List<ChildEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChild(child: ChildEntity)
}