package com.vahak.parentcontroll.domain.repository

import android.util.Log
import com.vahak.parentcontroll.core.data.local.dao.ChildDao
import com.vahak.parentcontroll.core.data.local.dao.ChildSettingsDao
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import com.vahak.parentcontroll.data.remote.ChildApi
import com.vahak.parentcontroll.data.remote.CreateChildRequestDto
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import com.vahak.parentcontroll.core.data.local.entity.Gender as DbGender

interface ChildRepository {
    suspend fun createChild(
        name: String, dob: LocalDate, gender: DbGender, avatarId: Int, phone: String?
    ): Result<Unit>

    suspend fun syncChildrenFromServer(): Result<Unit>
    fun getAllChildren(): Flow<List<ChildEntity>>
    fun observeChildById(childId: String): Flow<ChildEntity?>
    suspend fun getChildById(childId: String): ChildEntity?

    // NEW: Needed for offline-first architecture
    suspend fun deleteChildLocally(childId: String)
}

class ChildRepositoryImpl @Inject constructor(
    private val childDao: ChildDao,
    private val childSettingsDao: ChildSettingsDao,
    private val childApi: ChildApi
) : ChildRepository {

    override suspend fun createChild(
        name: String, dob: LocalDate, gender: DbGender, avatarId: Int, phone: String?
    ): Result<Unit> {
        // 1. GENERATE UUID ON ANDROID
        val newChildId = UUID.randomUUID().toString()

        // 2. SAVE LOCALLY IMMEDIATELY (Mark as DIRTY)
        val newChild = ChildEntity(
            id = newChildId,
            name = name,
            dob = dob,
            gender = gender,
            avatarId = avatarId,
            phone = phone,
            isSynced = false, // DIRTY
            isDeleted = false
        )
        childDao.upsertChild(newChild)

        // 3. GENERATE LOCAL SETTINGS IMMEDIATELY (Mark as DIRTY)
        val defaultSettings = GlobalSettingsEntity(
            childId = newChildId,
            isSynced = false // DIRTY
        )
        childSettingsDao.upsertGlobalSettings(defaultSettings)

        // 4. ATTEMPT TO PUSH IN BACKGROUND
        pushDirtyChildren()

        // 5. RETURN SUCCESS INSTANTLY TO UI
        return Result.success(Unit)
    }

    override suspend fun deleteChildLocally(childId: String) {
        // 1. Mark as soft-deleted locally. UI instantly updates.
        childDao.softDeleteChild(childId)
        // 2. Try to push it immediately
        pushPendingDeletions()
    }

    override suspend fun syncChildrenFromServer(): Result<Unit> {
        // PHASE 2 PROTOCOL: Push before Pull!
        pushPendingDeletions()
        pushDirtyChildren()

        return try {
            val response = childApi.getChildren()
            if (response.isSuccessful && response.body() != null) {

                val dirtyIds = childDao.getUnsyncedChildren().map { it.id }.toSet()
                val deletedIds = childDao.getPendingDeletedChildren().map { it.id }.toSet()

                val serverChildren = response.body()!!.mapNotNull { dto ->
                    // DO NOT overwrite if we have a pending offline change or deletion!
                    if (dirtyIds.contains(dto.id) || deletedIds.contains(dto.id)) {
                        null
                    } else {
                        ChildEntity(
                            id = dto.id, name = dto.name, dob = LocalDate.parse(dto.dob),
                            gender = if (dto.gender == "BOY") DbGender.BOY else DbGender.GIRL,
                            avatarId = dto.avatarId, phone = dto.phone,
                            isSynced = true // Fresh from server
                        )
                    }
                }

                childDao.upsertChildren(serverChildren)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync children"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("Network error while syncing children"))
        }
    }

    private suspend fun pushPendingDeletions() {
        try {
            val pendingDeletes = childDao.getPendingDeletedChildren()
            for (child in pendingDeletes) {
                // We now call the API to delete
                val response = childApi.deleteChild(child.id)
                if (response.isSuccessful || response.code() == 404) {
                    childDao.hardDeleteChild(child.id)
                    // Also clean up local settings/rules/web if CASCADE didn't catch it
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Failed to push child deletions.")
        }
    }

    private suspend fun pushDirtyChildren() {
        try {
            val dirtyChildren = childDao.getUnsyncedChildren()
            for (child in dirtyChildren) {
                // Since Android generated the ID, we send it in the DTO
                val request = CreateChildRequestDto(
                    id = child.id, // NEW: Include the ID in the request
                    name = child.name,
                    dob = child.dob.toString(),
                    gender = child.gender.name,
                    avatarId = child.avatarId,
                    phone = child.phone
                )
                // In REST, when the client provides the ID, we usually use PUT for an Upsert
                val response = childApi.upsertChild(child.id, request)
                if (response.isSuccessful) {
                    childDao.markAsSynced(child.id)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncEngine", "Failed to push dirty children.")
        }
    }

    override fun getAllChildren(): Flow<List<ChildEntity>> = childDao.getAllChildren()
    override fun observeChildById(childId: String) = childDao.observeChildById(childId)
    override suspend fun getChildById(childId: String) = childDao.getChildById(childId)
}