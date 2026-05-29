package com.vahak.mehrban.presentation.setting

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.GlobalSettingsEntity
import com.vahak.mehrban.core.util.PermissionChecker
import com.vahak.mehrban.core.util.PermissionType
import com.vahak.mehrban.domain.repository.AppRuleRepository
import com.vahak.mehrban.domain.repository.ChildRepository
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChildSettingsState(
    val activeChild: ChildEntity? = null,
    val allChildren: List<ChildEntity> = emptyList(),
    val settings: GlobalSettingsEntity? = null,
    val allowedAppsCount: Int = 0, // FIXED: Changed from blockedAppsCount
    val isChildSheetOpen: Boolean = false,
    val isLoading: Boolean = true,
)

sealed class ChildSettingsEvent {
    object BackClicked : ChildSettingsEvent()
    object HelpClicked : ChildSettingsEvent()
    object OpenChildSheet : ChildSettingsEvent()
    object CloseChildSheet : ChildSettingsEvent()
    data class SelectChild(val childId: String) : ChildSettingsEvent()
    data class GridItemClicked(val route: String, val context: Context) : ChildSettingsEvent()
}

sealed class ChildSettingsEffect {
    object NavigateBack : ChildSettingsEffect()
    data class NavigateToFeature(val route: String) : ChildSettingsEffect()
    data class ShowToast(val message: String) : ChildSettingsEffect()
    data class NavigateToPermissionSlider(val route: String, val missingPermissions: List<String>) :
        ChildSettingsEffect()
}

@HiltViewModel
class ChildSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val childRepository: ChildRepository,
    private val settingsRepository: SettingsRepository,
    private val appRuleRepository: AppRuleRepository,
    private val sessionManager: SessionManager,
) : BaseViewModel<ChildSettingsState, ChildSettingsEvent, ChildSettingsEffect>(ChildSettingsState()) {

    private val currentChildIdFlow =
        MutableStateFlow(checkNotNull(savedStateHandle.get<String>("childId")))

    private val featurePermissionsMap = mapOf(
        "time_limit" to listOf(PermissionType.USAGE_STATS, PermissionType.OVERLAY),
        "app_lock" to listOf(PermissionType.USAGE_STATS, PermissionType.OVERLAY),
        "site_management" to listOf(PermissionType.VPN),
        "location" to listOf(PermissionType.LOCATION),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            launch {
                childRepository.getAllChildren().collectLatest { allKids ->
                    updateState { copy(allChildren = allKids) }
                }
            }

            currentChildIdFlow.collectLatest { id ->
                updateState { copy(isLoading = true) }

                // 🚀 PURE OBSERVATION: All network calls are deleted.
                // We just listen to Room. If the DB updates, the UI recomposes automatically.

                launch {
                    childRepository.observeChildById(id).collectLatest { child ->
                        updateState { copy(activeChild = child) }
                        checkIfReady()
                    }
                }

                launch {
                    settingsRepository.getGlobalSettings(id).collectLatest { settings ->
                        val safeSettings = settings ?: GlobalSettingsEntity(childId = id)
                        updateState { copy(settings = safeSettings) }
                        checkIfReady()
                    }
                }

                launch {
                    appRuleRepository.observeAllRules(id).collectLatest { rules ->
                        val allowedCount = rules.count { it.isAllowed }
                        updateState { copy(allowedAppsCount = allowedCount) }
                    }
                }
            }
        }
    }

    private fun checkIfReady() {
        if (state.value.activeChild != null && state.value.settings != null) {
            updateState { copy(isLoading = false) }
        }
    }

    override fun onEvent(event: ChildSettingsEvent) {
        when (event) {
            is ChildSettingsEvent.BackClicked -> sendEffect(ChildSettingsEffect.NavigateBack)
            is ChildSettingsEvent.HelpClicked -> sendEffect(ChildSettingsEffect.ShowToast("This feature is under development."))
            is ChildSettingsEvent.OpenChildSheet -> updateState { copy(isChildSheetOpen = true) }
            is ChildSettingsEvent.CloseChildSheet -> updateState { copy(isChildSheetOpen = false) }
            is ChildSettingsEvent.SelectChild -> {
                updateState { copy(isChildSheetOpen = false, isLoading = true) }
                currentChildIdFlow.value = event.childId

                // 🚀 Tell the SessionManager we switched children.
                // The SessionSyncEngine will detect this and fetch the new child's rules.
                viewModelScope.launch {
                    sessionManager.setViewedChildId(event.childId)
                }
            }

            is ChildSettingsEvent.GridItemClicked -> {
                if (event.route in listOf(
                        "location",
                        "safe_search",
                        "prevent_delete",
                        "eye_protect",
                        "content_movies",
                        "site_management"
                    )
                ) {
                    sendEffect(ChildSettingsEffect.ShowToast("This feature will be available soon!"))
                    return
                }

                val requiredPermissions = featurePermissionsMap[event.route] ?: emptyList()
                val missingPermissions = requiredPermissions.filter {
                    !PermissionChecker.hasPermission(event.context, it)
                }

                if (missingPermissions.isEmpty()) {
                    sendEffect(ChildSettingsEffect.NavigateToFeature(event.route))
                } else {
                    sendEffect(
                        ChildSettingsEffect.NavigateToPermissionSlider(
                            event.route, missingPermissions.map { it.name })
                    )
                }
            }
        }
    }
}