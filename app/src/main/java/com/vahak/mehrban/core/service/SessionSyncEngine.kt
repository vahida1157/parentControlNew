package com.vahak.mehrban.core.service

import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.domain.repository.AppRuleRepository
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.domain.repository.NotificationRepository
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.domain.repository.UsageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionSyncEngine @Inject constructor(
    private val sessionManager: SessionManager,
    private val childRepository: ChildRepository,
    private val settingsRepository: SettingsRepository,
    private val appRuleRepository: AppRuleRepository,
    private val usageRepository: UsageRepository,
    private val notificationRepository: NotificationRepository,
    private val safeBrowserRepository: SafeBrowserRepository,
) {
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        engineScope.launch {
            sessionManager.userPhoneFlow.distinctUntilChanged().collectLatest { phone ->
                if (phone != null) {
                    Timber.d("Valid session detected. Booting all background sync targets.")

                    launch { childRepository.syncChildrenFromServer() }
                    launch { notificationRepository.syncNotificationsFromServer() }

                    launch {
                        sessionManager.viewedChildIdFlow.distinctUntilChanged()
                            .collectLatest { childId ->
                                if (childId != null) {
                                    launch { settingsRepository.syncSettingsFromServer(childId) }
                                    launch { appRuleRepository.syncRulesFromServer(childId) }
                                    launch {
                                        usageRepository.syncUnsyncedData(
                                            childId, forcePing = true
                                        )
                                    }
                                    launch { safeBrowserRepository.syncBrowserDataFromServer(childId) } // 🚀 ADDED THIS
                                }
                            }
                    }

                    launch {
                        sessionManager.activeChildIdFlow.distinctUntilChanged()
                            .collectLatest { childId ->
                                val viewedId = sessionManager.viewedChildIdFlow.firstOrNull()
                                if (childId != null && childId != viewedId) {
                                    launch { settingsRepository.syncSettingsFromServer(childId) }
                                    launch { appRuleRepository.syncRulesFromServer(childId) }
                                    launch { safeBrowserRepository.syncBrowserDataFromServer(childId) }
                                }
                            }
                    }
                } else {
                    Timber.d("No active session. Sync engine is going to sleep.")
                }
            }
        }
    }
}