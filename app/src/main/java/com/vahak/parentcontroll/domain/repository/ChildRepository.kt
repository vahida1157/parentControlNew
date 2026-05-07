package com.vahak.parentcontroll.domain.repository

import com.vahak.parentcontroll.core.data.local.dao.ChildDao
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import com.vahak.parentcontroll.data.remote.ChildApi
import com.vahak.parentcontroll.data.remote.CreateChildRequestDto
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import com.vahak.parentcontroll.core.data.local.entity.Gender as DbGender

interface ChildRepository {
    suspend fun createChild(
        name: String, dob: LocalDate, gender: DbGender, avatarId: Int
    ): Result<Unit>

    suspend fun syncChildrenFromServer(): Result<Unit> // New Sync Method
    fun getAllChildren(): Flow<List<ChildEntity>>
    fun observeChildById(childId: String): Flow<ChildEntity?>
    suspend fun getChildById(childId: String): ChildEntity?
}

class ChildRepositoryImpl @Inject constructor(
    private val childDao: ChildDao,
    private val settingsDao: SettingsDao,
    private val childApi: ChildApi // Injected via NetworkModule
) : ChildRepository {

    override suspend fun createChild(
        name: String, dob: LocalDate, gender: DbGender, avatarId: Int
    ): Result<Unit> {
        return try {
            // 1. Ask the backend to create the child
            val request = CreateChildRequestDto(
                name = name, dob = dob.toString(), gender = gender.name, avatarId = avatarId
            )
            val response = childApi.addChild(request)

            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!

                // 2. Create the entity using the official UUID from the server
                val newChild = ChildEntity(
                    id = body.id,
                    name = body.name,
                    dob = LocalDate.parse(body.dob),
                    gender = gender,
                    avatarId = body.avatarId
                )

                // 3. Save the child profile safely
                childDao.upsertChild(newChild)

                // 4. Automatically create default global settings
                val defaultSettings = GlobalSettingsEntity(childId = newChild.id)
                settingsDao.upsertGlobalSettings(defaultSettings)

                Result.success(Unit)
            } else {
                Result.failure(Exception("خطا در ارتباط با سرور: ${response.code()}"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("خطای شبکه. لطفا اینترنت خود را بررسی کنید."))
        }
    }

    override suspend fun syncChildrenFromServer(): Result<Unit> {
        return try {
            val response = childApi.getChildren()
            if (response.isSuccessful && response.body() != null) {
                val serverChildren = response.body()!!.map { dto ->
                    ChildEntity(
                        id = dto.id,
                        name = dto.name,
                        dob = LocalDate.parse(dto.dob),
                        gender = if (dto.gender == "BOY") DbGender.BOY else DbGender.GIRL,
                        avatarId = dto.avatarId
                    )
                }

                // Safely update all existing children and insert new ones
                childDao.upsertChildren(serverChildren)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to sync"))
            }
        } catch (_: Exception) {
            Result.failure(Exception("Network error"))
        }
    }

    override fun getAllChildren(): Flow<List<ChildEntity>> = childDao.getAllChildren()
    override fun observeChildById(childId: String) = childDao.observeChildById(childId)
    override suspend fun getChildById(childId: String) = childDao.getChildById(childId)
}