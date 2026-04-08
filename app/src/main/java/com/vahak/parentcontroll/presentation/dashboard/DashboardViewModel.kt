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
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Contract
data class DashboardState(
    val children: List<ChildEntity> = emptyList(),
    val activeChild: ChildEntity? = null,
    val isChildSheetOpen: Boolean = false,
    val isProtectionActive: Boolean = false,
)

sealed class DashboardEvent {
    object LockClicked : DashboardEvent()
    object OpenChildSheet : DashboardEvent()
    object CloseChildSheet : DashboardEvent()
    data class SelectChild(val child: ChildEntity) : DashboardEvent()
    data class ActivateProtection(val childId: String) : DashboardEvent()
    data class DeactivateProtection(val childId: String) : DashboardEvent()
}

sealed class DashboardEffect {
    object NavigateToLogin : DashboardEffect()
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val childRepository: ChildRepository, // <-- Inject the repo!
    private val sessionManager: SessionManager,
) : BaseViewModel<DashboardState, DashboardEvent, DashboardEffect>(DashboardState()) {

    init {
        // Observe the Room Database reactively
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

        // NEW: Observe the Session! If it becomes null (launcher exited), update the UI state.
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

            is DashboardEvent.ActivateProtection -> {
                startProtectionService(event.childId)
            }

            is DashboardEvent.DeactivateProtection -> {
                stopProtectionService()
            }
        }
    }

    private fun startProtectionService(childId: String) {
        viewModelScope.launch {
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