package com.vahak.parentcontroll.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.service.RestrictionEnforcerService
import com.vahak.parentcontroll.core.service.WebFilterVpnService
import com.vahak.parentcontroll.core.util.LauncherManager
import com.vahak.parentcontroll.domain.repository.AuthRepository
import com.vahak.parentcontroll.domain.repository.ChildRepository
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Contract
data class DashboardState(
    val children: List<ChildEntity> = emptyList(),
    val activeChild: ChildEntity? = null,
    val isChildSheetOpen: Boolean = false,
    val isProtectionActive: Boolean = false,
    val showPinRequiredDialog: Boolean = false // NEW: Dialog state
)

sealed class DashboardEvent {
    object LockClicked : DashboardEvent()
    object OpenChildSheet : DashboardEvent()
    object CloseChildSheet : DashboardEvent()
    data class SelectChild(val child: ChildEntity) : DashboardEvent()
    data class ActivateProtection(val childId: String) : DashboardEvent()
    data class DeactivateProtection(val childId: String) : DashboardEvent()

    // NEW: Dialog Events
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
    private val settingsDao: SettingsDao,
) : BaseViewModel<DashboardState, DashboardEvent, DashboardEffect>(DashboardState()) {

    init {
        // --- 1. PRO FIX: IMMEDIATE PIN CHECK ON LOAD ---
        viewModelScope.launch {
            val savedPin = sessionManager.hasPinSetupFlow.first()
            if (!savedPin) {
                // If it's their first time logging in, send them straight to password setup!
                sendEffect(DashboardEffect.NavigateToPasswordSetup)
            }
        }

        // --- 2. Observe Data ---
        viewModelScope.launch {
            childRepository.getAllChildren().collectLatest { childList ->
                updateState {
                    copy(
                        children = childList,
                        activeChild = activeChild ?: childList.firstOrNull()
                    )
                }
            }
        }

        viewModelScope.launch {
            sessionManager.activeChildIdFlow.collectLatest { activeId ->
                updateState { copy(isProtectionActive = activeId != null) }
            }
        }

        viewModelScope.launch {
            childRepository.getAllChildren().collectLatest { childList ->
                updateState {
                    copy(
                        children = childList,
                        activeChild = activeChild ?: childList.firstOrNull()
                    )
                }
            }
        }

        // 2. Silently sync fresh data from the backend
        viewModelScope.launch {
            childRepository.syncChildrenFromServer()
        }
    }

    override fun onEvent(event: DashboardEvent) {
        when (event) {
            is DashboardEvent.LockClicked -> performLogout()
            is DashboardEvent.OpenChildSheet -> updateState { copy(isChildSheetOpen = true) }
            is DashboardEvent.CloseChildSheet -> updateState { copy(isChildSheetOpen = false) }
            is DashboardEvent.SelectChild -> {
                updateState { copy(activeChild = event.child, isChildSheetOpen = false) }
            }

            is DashboardEvent.ActivateProtection -> startProtectionService(event.childId)
            is DashboardEvent.DeactivateProtection -> stopProtectionService()

            // Handle Dialog
            is DashboardEvent.ClosePinRequiredDialog -> updateState { copy(showPinRequiredDialog = false) }
            is DashboardEvent.GoToPasswordSetupClicked -> {
                updateState { copy(showPinRequiredDialog = false) }
                sendEffect(DashboardEffect.NavigateToPasswordSetup)
            }
        }
    }

    private fun startProtectionService(childId: String) {
        viewModelScope.launch {
            val savedPin = sessionManager.hasPinSetupFlow.first()
            if (!savedPin) {
                updateState { copy(showPinRequiredDialog = true) }
                return@launch
            }

            // 1. THIS is the master switch. Setting this will trigger MainActivity!
            sessionManager.setActiveChildId(childId)

            val intent = Intent(context, RestrictionEnforcerService::class.java).apply {
                action = RestrictionEnforcerService.ACTION_START
                putExtra(RestrictionEnforcerService.EXTRA_CHILD_ID, childId)
            }
            ContextCompat.startForegroundService(context, intent)

            val settings = settingsDao.getGlobalSettings(childId).first()
            if (settings?.isSiteManagementActive == true) {
                val vpnIntent = Intent(context, WebFilterVpnService::class.java).apply {
                    action = WebFilterVpnService.ACTION_START
                }
                ContextCompat.startForegroundService(context, vpnIntent)
            }

            // 2. Tell the OS we are the Home screen now
            LauncherManager.enableLauncherMode(context)
        }
    }

    private fun stopProtectionService() {
        viewModelScope.launch {
            sessionManager.clearActiveChildId()

            val intent = Intent(context, RestrictionEnforcerService::class.java).apply {
                action = RestrictionEnforcerService.ACTION_STOP
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