package com.vahak.mehrban.domain.repository

import com.vahak.mehrban.core.data.local.dao.ChildDao
import com.vahak.mehrban.core.data.local.dao.ChildSettingsDao
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.GlobalSettingsEntity
import com.vahak.mehrban.data.remote.ChildApi
import com.vahak.mehrban.data.remote.CreateChildRequestDto
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import com.vahak.mehrban.core.data.local.entity.Gender as DbGender

interface ChildRepository {
    suspend fun createChild(
        name: String, dob: LocalDate, gender: DbGender, avatarId: Int, phone: String?
    ): Result<Unit>

    suspend fun syncChildrenFromServer(): Result<Unit>
    fun getAllChildren(): Flow<List<ChildEntity>>
    fun observeChildById(childId: String): Flow<ChildEntity?>
    suspend fun getChildById(childId: String): ChildEntity?
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
        Timber.d("Initiating child profile creation")
        val newChildId = UUID.randomUUID().toString()

        val newChild = ChildEntity(
            id = newChildId,
            name = name,
            dob = dob,
            gender = gender,
            avatarId = avatarId,
            phone = phone,
            isSynced = false,
            isDeleted = false
        )
        childDao.upsertChild(newChild)

        val defaultSettings = GlobalSettingsEntity(childId = newChildId, isSynced = false)
        childSettingsDao.upsertGlobalSettings(defaultSettings)

        Timber.i("Child profile created locally")
        pushDirtyChildren()
        return Result.success(Unit)
    }

    override suspend fun deleteChildLocally(childId: String) {
        Timber.d("Soft-deleting child profile locally")
        childDao.softDeleteChild(childId)
        Timber.i("Child profile soft-deleted locally")
        pushPendingDeletions()
    }

    override suspend fun syncChildrenFromServer(): Result<Unit> {
        Timber.d("Initiating child profile synchronization from server")
        pushPendingDeletions()
        pushDirtyChildren()

        return try {
            val response = childApi.getChildren()
            if (response.isSuccessful && response.body() != null) {
                val dirtyIds = childDao.getUnsyncedChildren().map { it.id }.toSet()
                val deletedIds = childDao.getPendingDeletedChildren().map { it.id }.toSet()

                val serverChildren = response.body()!!.mapNotNull { dto ->
                    if (dirtyIds.contains(dto.id) || deletedIds.contains(dto.id)) {
                        null
                    } else {
                        ChildEntity(
                            id = dto.id,
                            name = dto.name,
                            dob = LocalDate.parse(dto.dob),
                            gender = if (dto.gender == "BOY") DbGender.BOY else DbGender.GIRL,
                            avatarId = dto.avatarId,
                            phone = dto.phone,
                            isSynced = true
                        )
                    }
                }

                Timber.d(
                    "Upserting synchronized child profiles locally, count: %d", serverChildren.size
                )
                childDao.upsertChildren(serverChildren)
                Timber.i("Child profiles synchronized successfully")
                Result.success(Unit)
            } else {
                Timber.w("Failed to synchronize child profiles, HTTP status: %d", response.code())
                Result.failure(Exception("Failed to sync children"))
            }
        } catch (e: Exception) {
            Timber.w(e, "Network error while syncing child profiles")
            Result.failure(Exception("Network error while syncing children"))
        }
    }

    private suspend fun pushPendingDeletions() {
        try {
            Timber.d("Pushing pending child profile deletions to server")
            val pendingDeletes = childDao.getPendingDeletedChildren()
            for (child in pendingDeletes) {
                val response = childApi.deleteChild(child.id)
                if (response.isSuccessful || response.code() == 404) {
                    Timber.d("Executing local hard delete for child profile")
                    childDao.hardDeleteChild(child.id)
                }
            }
            Timber.i("Pending child profile deletions processed successfully")
        } catch (e: Exception) {
            Timber.w(
                e,
                "Failed to push child profile deletions to server, retaining local soft-delete state"
            )
        }
    }

    private suspend fun pushDirtyChildren() {
        try {
            Timber.d("Pushing dirty child profiles to server")
            val dirtyChildren = childDao.getUnsyncedChildren()
            for (child in dirtyChildren) {
                val request = CreateChildRequestDto(
                    id = child.id,
                    name = child.name,
                    dob = child.dob.toString(),
                    gender = child.gender.name,
                    avatarId = child.avatarId,
                    phone = child.phone
                )
                val response = childApi.upsertChild(child.id, request)
                if (response.isSuccessful) {
                    childDao.markAsSynced(child.id)
                }
            }
            Timber.i("Dirty child profiles pushed successfully")
        } catch (e: Exception) {
            Timber.w(
                e, "Failed to push dirty child profiles to server, retaining local dirty state"
            )
        }
    }

    override fun getAllChildren(): Flow<List<ChildEntity>> = childDao.getAllChildren()
    override fun observeChildById(childId: String) = childDao.observeChildById(childId)
    override suspend fun getChildById(childId: String) = childDao.getChildById(childId)
}