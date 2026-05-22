package com.vahak.mehrban.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vahak.mehrban.core.data.local.entity.AppRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {

    // The Launcher will observe this to know which apps to show in the Drawer
    @Query("SELECT * FROM app_rules WHERE child_id = :childId AND is_allowed = 1")
    fun observeAllowedApps(childId: String): Flow<List<AppRuleEntity>>

    // The Parent UI will observe this to see all rules
    @Query("SELECT * FROM app_rules WHERE child_id = :childId")
    fun observeAllAppRules(childId: String): Flow<List<AppRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateRule(rule: AppRuleEntity)

    // Batch insert for when the parent first sets up the phone
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRules(rules: List<AppRuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRule(rule: AppRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRules(rules: List<AppRuleEntity>)

    // --- PRO OFFLINE FIX ---
    @Query("SELECT * FROM app_rules WHERE child_id = :childId AND is_synced = 0")
    suspend fun getUnsyncedRules(childId: String): List<AppRuleEntity>

    @Query("UPDATE app_rules SET is_synced = 1 WHERE child_id = :childId AND package_name IN (:packageNames)")
    suspend fun markRulesAsSynced(childId: String, packageNames: List<String>)
}