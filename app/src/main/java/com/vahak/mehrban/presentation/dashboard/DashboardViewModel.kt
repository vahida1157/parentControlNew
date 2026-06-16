package com.vahak.mehrban.presentation.dashboard

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.service.RestrictionEnforcerService
import com.vahak.mehrban.core.util.AppUpdateManager
import com.vahak.mehrban.core.util.LauncherManager
import com.vahak.mehrban.domain.repository.AuthRepository
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.domain.repository.UsageRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject

// Contract remains identical
data class DashboardState(
    val children: List<ChildEntity> = emptyList(),
    val activeChild: ChildEntity? = null,
    val isChildSheetOpen: Boolean = false,
    val isProtectionActive: Boolean = false,
    val showPinRequiredDialog: Boolean = false,
    val activeChildTimeLimitMins: Int = 0,
    val isTimeLimitActive: Boolean = false,
    val activeChildUsageSeconds: Int = 0,
    val activeChildLocalSeconds: Int = 0,
    val activeChildGlobalSeconds: Int = 0,
)

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
    private val usageRepository: UsageRepository,
    private val appUpdateManager: AppUpdateManager,
) : BaseViewModel<DashboardState, DashboardEvent, DashboardEffect>(DashboardState()) {

    

    val updateState = appUpdateManager.updateState
    val isUpdateIgnored = appUpdateManager.isUpdateIgnored

    fun showUpdateDialogAgain() {
        appUpdateManager.unignoreUpdate()
    }

    init {
        viewModelScope.launch {
            if (!sessionManager.hasParentPin()) {
                Timber.w("Parent PIN missing, redirecting to security setup")
                sendEffect(DashboardEffect.NavigateToPasswordSetup)
            }
        }

        viewModelScope.launch {
            childRepository.getAllChildren().collectLatest { childList ->
                val lastViewedId = sessionManager.viewedChildIdFlow.first()
                val targetChild =
                    childList.find { it.id == lastViewedId } ?: childList.firstOrNull()

                if (targetChild != null && targetChild.id != lastViewedId) {
                    sessionManager.setViewedChildId(targetChild.id)
                }

                updateState { copy(children = childList, activeChild = targetChild) }
            }
        }

        viewModelScope.launch {
            sessionManager.activeChildIdFlow.collectLatest { activeId ->
                updateState { copy(isProtectionActive = activeId != null) }
            }
        }

        val viewedChildIdFlow = state.map { it.activeChild?.id }.distinctUntilChanged()

        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class) viewedChildIdFlow.flatMapLatest { childId ->
                if (childId != null) settingsRepository.getGlobalSettings(childId)
                else kotlinx.coroutines.flow.flowOf(null)
            }.collectLatest { settings ->
                updateState {
                    copy(
                        activeChildTimeLimitMins = settings?.dailyTimeLimitMins ?: 0,
                        isTimeLimitActive = settings?.isTimeLimitActive ?: false
                    )
                }
            }
        }

        viewModelScope.launch {
            @OptIn(ExperimentalCoroutinesApi::class) viewedChildIdFlow.flatMapLatest { childId ->
                if (childId != null) usageRepository.observeDailyUsage(childId, LocalDate.now())
                else kotlinx.coroutines.flow.flowOf(null)
            }.collectLatest { daily ->
                val local = daily?.usedSeconds ?: 0
                val cachedGlobal = daily?.globalUsedSeconds ?: 0
                updateState {
                    val newGlobal = maxOf(activeChildGlobalSeconds, cachedGlobal)
                    copy(
                        activeChildLocalSeconds = local,
                        activeChildGlobalSeconds = newGlobal,
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
            is DashboardEvent.SelectChild -> {
                Timber.d("Dashboard context switched to new child profile")
                viewModelScope.launch {
                    sessionManager.setViewedChildId(event.child.id)
                }
                updateState {
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
                Timber.w("Protection activation blocked: Parent PIN required")
                updateState { copy(showPinRequiredDialog = true) }
                return@launch
            }
            Timber.i("Activating device protection service for child profile")
            sessionManager.setActiveChildId(childId)
            val intent = Intent(context, RestrictionEnforcerService::class.java).apply {
                action = RestrictionEnforcerService.ACTION_START
                putExtra(RestrictionEnforcerService.EXTRA_CHILD_ID, childId)
            }
            ContextCompat.startForegroundService(context, intent)
            LauncherManager.enableLauncherMode(context)
        }
    }

    private fun stopProtectionService() {
        Timber.i("Deactivating device protection service")
        viewModelScope.launch {
            sessionManager.clearActiveChildId()
            val intent = Intent(context, RestrictionEnforcerService::class.java).apply {
                action = RestrictionEnforcerService.ACTION_START
            }
            context.startService(intent)
            LauncherManager.disableLauncherMode(context)
        }
    }

    private fun performLogout() {
        Timber.i("User initiated dashboard lock/logout")
        viewModelScope.launch {
            authRepository.logout()
            sendEffect(DashboardEffect.NavigateToLogin)
        }
    }
}