package com.vahak.parentcontroll.domain.repository

import com.vahak.parentcontroll.core.data.local.dao.AppRuleDao
import com.vahak.parentcontroll.core.data.local.entity.AppRuleEntity
import com.vahak.parentcontroll.data.remote.AppRuleDto
import com.vahak.parentcontroll.data.remote.BulkRuleRequestDto
import com.vahak.parentcontroll.data.remote.RuleApi
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface AppRuleRepository {
    fun observeAllRules(childId: String): Flow<List<AppRuleEntity>>
    suspend fun toggleAppRule(childId: String, packageName: String, isAllowed: Boolean)
    suspend fun syncRulesFromServer(childId: String): Result<Unit>
    suspend fun pushRulesToServer(childId: String): Result<Unit>
}

class AppRuleRepositoryImpl @Inject constructor(
    private val appRuleDao: AppRuleDao,
    private val ruleApi: RuleApi
) : AppRuleRepository {

    override fun observeAllRules(childId: String): Flow<List<AppRuleEntity>> =
        appRuleDao.observeAllAppRules(childId)

    override suspend fun toggleAppRule(childId: String, packageName: String, isAllowed: Boolean) {
        val newRule = AppRuleEntity(
            childId = childId,
            packageName = packageName,
            isAllowed = isAllowed,
            isSynced = false, // Mark as DIRTY
            updatedAt = System.currentTimeMillis()
        )
        appRuleDao.upsertRule(newRule)
    }

    override suspend fun pushRulesToServer(childId: String): Result<Unit> {
        return try {
            val unsyncedRules = appRuleDao.getUnsyncedRules(childId)

            if (unsyncedRules.isEmpty()) return Result.success(Unit)

            val dtoList = unsyncedRules.map {
                AppRuleDto(
                    packageName = it.packageName,
                    isAllowed = it.isAllowed,
                    updatedAt = it.updatedAt // BUG FIX: Added the missing timestamp!
                )
            }

            val response = ruleApi.updateAppRules(childId, BulkRuleRequestDto(rules = dtoList))

            if (response.isSuccessful) {
                val pushedPackages = unsyncedRules.map { it.packageName }
                appRuleDao.markRulesAsSynced(childId, pushedPackages)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to push rules"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("Network error while pushing rules"))
        }
    }

    override suspend fun syncRulesFromServer(childId: String): Result<Unit> {
        pushRulesToServer(childId)

        return try {
            val response = ruleApi.getAppRules(childId)
            if (response.isSuccessful && response.body() != null) {

                val unsyncedPackages =
                    appRuleDao.getUnsyncedRules(childId).map { it.packageName }.toSet()

                val serverRules = response.body()!!.mapNotNull { dto ->
                    if (unsyncedPackages.contains(dto.packageName)) {
                        null
                    } else {
                        AppRuleEntity(
                            childId = childId,
                            packageName = dto.packageName,
                            isAllowed = dto.isAllowed,
                            isSynced = true,
                            updatedAt = dto.updatedAt // Keep the server's timestamp
                        )
                    }
                }

                appRuleDao.upsertRules(serverRules)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to fetch rules"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("Network error during rule sync"))
        }
    }
}