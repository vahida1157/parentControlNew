package com.vahak.parentcontroll.domain.repository

import com.vahak.parentcontroll.core.data.local.dao.ChildDao
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface ChildRepository {
    suspend fun createChild(child: ChildEntity)
    fun getAllChildren(): Flow<List<ChildEntity>>
    fun observeChildById(childId: String): Flow<ChildEntity?>
    suspend fun getChildById(childId: String): ChildEntity?
}

class ChildRepositoryImpl @Inject constructor(
    private val childDao: ChildDao,
    private val settingsDao: SettingsDao
) : ChildRepository {

    override suspend fun createChild(child: ChildEntity) {
        // 1. Save the child profile
        childDao.insertChild(child)

        // 2. Automatically create default global settings for this new child
        val defaultSettings = GlobalSettingsEntity(childId = child.id)
        settingsDao.upsertGlobalSettings(defaultSettings)

        // TODO: In the future, make an API call to Spring here: apiService.uploadChild(child)
    }

    override fun getAllChildren(): Flow<List<ChildEntity>> = childDao.getAllChildren()
    override fun observeChildById(childId: String) = childDao.observeChildById(childId)
    override suspend fun getChildById(childId: String) = childDao.getChildById(childId)
}