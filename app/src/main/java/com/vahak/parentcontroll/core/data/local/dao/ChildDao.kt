package com.vahak.parentcontroll.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM children WHERE id = :childId")
    fun observeChildById(childId: String): Flow<ChildEntity?>

    @Query("SELECT * FROM children WHERE id = :childId")
    suspend fun getChildById(childId: String): ChildEntity?

    @Query("SELECT * FROM children")
    fun getAllChildren(): Flow<List<ChildEntity>>

    // PRO FIX: Upsert prevents CASCADE deletes of your settings/rules
    @Upsert
    suspend fun upsertChild(child: ChildEntity)

    @Upsert
    suspend fun upsertChildren(children: List<ChildEntity>)
}