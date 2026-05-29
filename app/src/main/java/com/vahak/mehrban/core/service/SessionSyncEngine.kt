package com.vahak.mehrban.core.service

import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.domain.repository.AppRuleRepository
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionSyncEngine @Inject constructor(
    private val sessionManager: SessionManager,
    private val childRepository: ChildRepository,
    private val settingsRepository: SettingsRepository,
    private val appRuleRepository: AppRuleRepository,
    private val usageRepository: UsageRepository
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        engineScope.launch {
            childRepository.syncChildrenFromServer()
        }

        // 🚀 SYNC TARGET 1: The child the parent is currently managing in the UI
        engineScope.launch {
            sessionManager.viewedChildIdFlow
                .distinctUntilChanged()
                .collectLatest { childId ->
                    if (childId != null) {
                        launch { settingsRepository.syncSettingsFromServer(childId) }
                        launch { appRuleRepository.syncRulesFromServer(childId) }
                        launch { usageRepository.syncUnsyncedData(childId, forcePing = true) }
                    }
                }
        }

        // 🚀 SYNC TARGET 2: The child currently being restricted (if Launcher/VPN is on)
        engineScope.launch {
            sessionManager.activeChildIdFlow
                .distinctUntilChanged()
                .collectLatest { childId ->
                    // Only sync if it's different from the viewed child to avoid double-fetching
                    val viewedId = sessionManager.viewedChildIdFlow.first()
                    if (childId != null && childId != viewedId) {
                        launch { settingsRepository.syncSettingsFromServer(childId) }
                        launch { appRuleRepository.syncRulesFromServer(childId) }
                    }
                }
        }
    }
}