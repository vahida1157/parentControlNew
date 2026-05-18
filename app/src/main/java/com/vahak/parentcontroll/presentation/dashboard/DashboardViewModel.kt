package com.vahak.parentcontroll.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.service.RestrictionEnforcerService
import com.vahak.parentcontroll.core.service.WebFilterVpnService
import com.vahak.parentcontroll.core.util.LauncherManager
import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.domain.repository.ChildRepository
import com.vahak.parentcontroll.domain.repository.SettingsRepository
import com.vahak.parentcontroll.domain.repository.UsageRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

// 1. Contract
data class DashboardState(
    val children: List<ChildEntity> = emptyList(),
    val activeChild: ChildEntity? = null,
    val isChildSheetOpen: Boolean = false,
    val isProtectionActive: Boolean = false,
    val showPinRequiredDialog: Boolean = false,

    val activeChildTimeLimitMins: Int = 0,
    val isTimeLimitActive: Boolean = false,

    // 🚀 FIXED: The UI binds to this, so we will manually update it with the max value
    val activeChildUsageSeconds: Int = 0,

    val activeChildLocalSeconds: Int = 0,
    val activeChildGlobalSeconds: Int = 0,
) {
    val effectiveUsageSeconds: Int
        get() = maxOf(activeChildLocalSeconds, activeChildGlobalSeconds)
}

sealed class DashboardEvent {
    object LockClicked : DashboardEvent()
    object OpenChildSheet : DashboardEvent()
    object CloseChildSheet : DashboardEvent()
    data class SelectChild(val child: ChildEntity) : DashboardEvent()
    data class ActivateProtection(val childId: String) : DashboardEvent()
    data class DeactivateProtection(val childId: String) : DashboardEvent()
    object ClosePinRequiredDialog : DashboardEvent()
    object GoToPasswordSetupClicked : DashboardEvent()
}

sealed class DashboardEffect {
    object NavigateToLogin : DashboardEffect()
    object NavigateToPasswordSetup : DashboardEffect()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val childRepository: ChildRepository,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
    private val usageRepository: UsageRepository
) : BaseViewModel<DashboardState, DashboardEvent, DashboardEffect>(DashboardState()) {

    init {
        viewModelScope.launch {
            if (!sessionManager.hasParentPin()) {
                sendEffect(DashboardEffect.NavigateToPasswordSetup)
            }
        }

        viewModelScope.launch {
            childRepository.getAllChildren().collectLatest { childList ->
                updateState {
                    copy(children = childList, activeChild = activeChild ?: childList.firstOrNull())
                }
            }
        }

        viewModelScope.launch {
            sessionManager.activeChildIdFlow.collectLatest { activeId ->
                updateState { copy(isProtectionActive = activeId != null) }
            }
        }

        viewModelScope.launch { childRepository.syncChildrenFromServer() }

        val activeChildIdFlow = state.map { it.activeChild?.id }.distinctUntilChanged()

        // A) Network Sync: Trigger server fetch when child changes
        viewModelScope.launch {
            activeChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    settingsRepository.syncSettingsFromServer(childId)

                    // 🚀 FIXED: Apply the fetched global time directly to activeChildUsageSeconds
                    val globalResponse = usageRepository.syncUnsyncedData(activeChildId = childId, forcePing = true)
                    if (globalResponse != null) {
                        val fetchedGlobal = globalResponse.globalDailySeconds[childId] ?: 0
                        updateState {
                            val newGlobal = maxOf(activeChildGlobalSeconds, fetchedGlobal)
                            copy(
                                activeChildGlobalSeconds = newGlobal,
                                activeChildUsageSeconds = maxOf(activeChildLocalSeconds, newGlobal)
                            )
                        }
                    }
                }
            }
        }

        // B) Observe Settings
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            activeChildIdFlow
                .flatMapLatest { childId ->
                    if (childId != null) settingsRepository.getGlobalSettings(childId)
                    else kotlinx.coroutines.flow.flowOf(null)
                }
                .collectLatest { settings ->
                    updateState {
                        copy(
                            activeChildTimeLimitMins = settings?.dailyTimeLimitMins ?: 0,
                            isTimeLimitActive = settings?.isTimeLimitActive ?: false
                        )
                    }
                }
        }

        // C) Observe LOCAL Usage & Room Cache
        viewModelScope.launch {
            @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
            activeChildIdFlow
                .flatMapLatest { childId ->
                    if (childId != null) usageRepository.observeDailyUsage(childId, LocalDate.now())
                    else kotlinx.coroutines.flow.flowOf(null)
                }
                .collectLatest { daily ->
                    val local = daily?.usedSeconds ?: 0
                    val cachedGlobal = daily?.globalUsedSeconds ?: 0

                    updateState {
                        val newGlobal = maxOf(activeChildGlobalSeconds, cachedGlobal)
                        copy(
                            activeChildLocalSeconds = local,
                            activeChildGlobalSeconds = newGlobal,
                            // 🚀 FIXED: Instantly pushes the highest value directly to the UI card!
                            activeChildUsageSeconds = maxOf(local, newGlobal)
                        )
                    }
                }
        }
    }

    override fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.LockClicked -> performLogout()
            is DashboardEvent.OpenChildSheet -> updateState { copy(isChildSheetOpen = true) }
            is DashboardEvent.CloseChildSheet -> updateState { copy(isChildSheetOpen = false) }
            is DashboardEvent.SelectChild -> updateState {
                copy(
                    activeChild = event.child,
                    isChildSheetOpen = false,
                    activeChildTimeLimitMins = 0,
                    isTimeLimitActive = false,
                    activeChildUsageSeconds = 0,
                    activeChildLocalSeconds = 0,
                    activeChildGlobalSeconds = 0
                )
            }
            is DashboardEvent.ActivateProtection -> startProtectionService(event.childId)
            is DashboardEvent.DeactivateProtection -> stopProtectionService()
            is DashboardEvent.ClosePinRequiredDialog -> updateState { copy(showPinRequiredDialog = false) }
            is DashboardEvent.GoToPasswordSetupClicked -> {
                updateState { copy(showPinRequiredDialog = false) }
                sendEffect(DashboardEffect.NavigateToPasswordSetup)
            }
        }
    }

    private fun startProtectionService(childId: String) {
        viewModelScope.launch {
            if (!sessionManager.hasParentPin()) {
                updateState { copy(showPinRequiredDialog = true) }
                return@launch
            }

            sessionManager.setActiveChildId(childId)

            val intent = Intent(context, RestrictionEnforcerService::class.java).apply {
                action = RestrictionEnforcerService.ACTION_START
                putExtra(RestrictionEnforcerService.EXTRA_CHILD_ID, childId)
            }
            ContextCompat.startForegroundService(context, intent)

            val settings = settingsRepository.getGlobalSettings(childId).first()
            if (settings?.isSiteManagementActive == true) {
                val vpnIntent = Intent(context, WebFilterVpnService::class.java).apply {
                    action = WebFilterVpnService.ACTION_START
                }
                ContextCompat.startForegroundService(context, vpnIntent)
            }

            LauncherManager.enableLauncherMode(context)
        }
    }

    private fun stopProtectionService() {
        viewModelScope.launch {
            sessionManager.clearActiveChildId()

            val intent = Intent(context, RestrictionEnforcerService::class.java).apply {
                action = RestrictionEnforcerService.ACTION_START
            }
            context.startService(intent)

            val vpnIntent = Intent(context, WebFilterVpnService::class.java).apply {
                action = WebFilterVpnService.ACTION_STOP
            }
            context.startService(vpnIntent)

            LauncherManager.disableLauncherMode(context)
        }
    }

    private fun performLogout() {
        viewModelScope.launch {
            authRepository.logout()
            sendEffect(DashboardEffect.NavigateToLogin)
        }
    }
}