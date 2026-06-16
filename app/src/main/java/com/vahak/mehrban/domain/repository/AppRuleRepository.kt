package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.dao.AppRuleDao
import com.vahak.mehrban.core.data.local.entity.AppRuleEntity
import com.vahak.mehrban.data.remote.AppRuleDto
import com.vahak.mehrban.data.remote.BulkRuleRequestDto
import com.vahak.mehrban.data.remote.RuleApi
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import javax.inject.Inject

interface AppRuleRepository {
    fun observeAllRules(childId: String): Flow<List<AppRuleEntity>>
    suspend fun toggleAppRule(childId: String, packageName: String, isAllowed: Boolean)
    suspend fun syncRulesFromServer(childId: String): Result<Unit>
    suspend fun pushRulesToServer(childId: String): Result<Unit>
}

class AppRuleRepositoryImpl @Inject constructor(
    private val appRuleDao: AppRuleDao, private val ruleApi: RuleApi
) : AppRuleRepository {


    override fun observeAllRules(childId: String): Flow<List<AppRuleEntity>> =
        appRuleDao.observeAllAppRules(childId)

    override suspend fun toggleAppRule(childId: String, packageName: String, isAllowed: Boolean) {
        Timber.d(
            "Upserting application rule state locally, packageName: %s, isAllowed: %b",
            packageName,
            isAllowed
        )
        val newRule = AppRuleEntity(
            childId = childId,
            packageName = packageName,
            isAllowed = isAllowed,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )
        appRuleDao.upsertRule(newRule)
        Timber.i("Application rule toggled successfully, packageName: %s", packageName)
    }

    override suspend fun pushRulesToServer(childId: String): Result<Unit> {
        return try {
            Timber.d("Fetching unsynced application rules for server push")
            val unsyncedRules = appRuleDao.getUnsyncedRules(childId)

            if (unsyncedRules.isEmpty()) {
                Timber.d("No unsynced application rules found, skipping push")
                return Result.success(Unit)
            }

            val dtoList = unsyncedRules.map {
                AppRuleDto(
                    packageName = it.packageName, isAllowed = it.isAllowed, updatedAt = it.updatedAt
                )
            }

            Timber.d("Pushing bulk application rules to server, ruleCount: %d", dtoList.size)
            val response = ruleApi.updateAppRules(childId, BulkRuleRequestDto(rules = dtoList))

            if (response.isSuccessful) {
                val pushedPackages = unsyncedRules.map { it.packageName }
                appRuleDao.markRulesAsSynced(childId, pushedPackages)
                Timber.i(
                    "Application rules pushed successfully, ruleCount: %d", pushedPackages.size
                )
                Result.success(Unit)
            } else {
                Timber.w("Failed to push application rules, HTTP status: %d", response.code())
                Result.failure(Exception("Failed to push rules"))
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error while pushing application rules")
            Result.failure(Exception("Network error while pushing rules"))
        }
    }

    override suspend fun syncRulesFromServer(childId: String): Result<Unit> {
        pushRulesToServer(childId)

        return try {
            Timber.d("Initiating application rule synchronization from server")
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
                            updatedAt = dto.updatedAt
                        )
                    }
                }

                Timber.d(
                    "Upserting synchronized application rules locally, newRuleCount: %d",
                    serverRules.size
                )
                appRuleDao.upsertRules(serverRules)
                Timber.i("Application rules synchronized successfully")
                Result.success(Unit)
            } else {
                Timber.w(
                    "Failed to fetch application rules from server, HTTP status: %d",
                    response.code()
                )
                Result.failure(Exception("Failed to fetch rules"))
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error during application rule synchronization")
            Result.failure(Exception("Network error during rule sync"))
        }
    }
}