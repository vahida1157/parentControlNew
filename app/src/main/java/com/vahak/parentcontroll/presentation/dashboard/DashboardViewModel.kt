package com.vahak.parentcontroll.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.SessionManager
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.service.TimeLimitEnforcerService
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
) : BaseViewModel<DashboardState, DashboardEvent, DashboardEffect>(DashboardState()) {

    init {
        // --- 1. PRO FIX: IMMEDIATE PIN CHECK ON LOAD ---
        viewModelScope.launch {
            val savedPin = sessionManager.parentPinFlow.first()
            if (savedPin.isNullOrEmpty()) {
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
            // --- 3. HARD GATE: Check PIN before activating launcher ---
            val savedPin = sessionManager.parentPinFlow.first()
            if (savedPin.isNullOrEmpty()) {
                // Show dialog instead of locking the phone without a key!
                updateState { copy(showPinRequiredDialog = true) }
                return@launch
            }

            sessionManager.setActiveChildId(childId)

            val intent = Intent(context, TimeLimitEnforcerService::class.java).apply {
                action = TimeLimitEnforcerService.ACTION_START
                putExtra(TimeLimitEnforcerService.EXTRA_CHILD_ID, childId)
            }
            context.startForegroundService(intent)

            LauncherManager.enableLauncherMode(context)
        }
    }

    private fun stopProtectionService() {
        viewModelScope.launch {
            sessionManager.clearActiveChildId()

            val intent = Intent(context, TimeLimitEnforcerService::class.java).apply {
                action = TimeLimitEnforcerService.ACTION_STOP
            }
            context.startService(intent)

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