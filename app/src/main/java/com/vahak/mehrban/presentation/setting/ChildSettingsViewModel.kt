package com.vahak.mehrban.presentation.setting

import android.content.Context
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
import com.vahak.mehrban.presentation.setting.ChildSettingsEffect.*
import com.vahak.mehrban.uiv2.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

enum class FeatureToastType { UNDER_DEVELOPMENT, COMING_SOON }

data class ChildSettingsState(
    val activeChild: ChildEntity? = null,
    val allChildren: List<ChildEntity> = emptyList(),
    val settings: GlobalSettingsEntity? = null,
    val allowedAppsCount: Int = 0,
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
    object AddChildClicked : ChildSettingsEvent()
}

sealed class ChildSettingsEffect {
    object NavigateBack : ChildSettingsEffect()
    data class NavigateToFeature(val route: String) : ChildSettingsEffect()
    data class ShowToast(val type: FeatureToastType) : ChildSettingsEffect()
    data class NavigateToPermissionSlider(val route: String, val missingPermissions: List<String>) :
        ChildSettingsEffect()
}

@HiltViewModel
class ChildSettingsViewModel @Inject constructor(
    private val childRepository: ChildRepository,
    private val settingsRepository: SettingsRepository,
    private val appRuleRepository: AppRuleRepository,
    private val sessionManager: SessionManager,
) : BaseViewModel<ChildSettingsState, ChildSettingsEvent, ChildSettingsEffect>(ChildSettingsState()) {

    private val currentChildIdFlow = MutableStateFlow<String?>(null)

    private val featurePermissionsMap = mapOf(
        "time_limit" to listOf(PermissionType.USAGE_STATS, PermissionType.OVERLAY),
        "app_lock" to listOf(PermissionType.USAGE_STATS, PermissionType.OVERLAY),
        "site_management" to listOf(PermissionType.VPN),
        "location" to listOf(PermissionType.LOCATION),
    )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val activeId = sessionManager.activeChildIdFlow.firstOrNull()
            if (activeId != null) {
                currentChildIdFlow.value = activeId
            } else {
                val firstChild = childRepository.getAllChildren().firstOrNull()?.firstOrNull()
                currentChildIdFlow.value = firstChild?.id
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            launch {
                childRepository.getAllChildren().collectLatest { allKids ->
                    updateState { copy(allChildren = allKids) }
                }
            }

            currentChildIdFlow.collectLatest { id ->
                if (id == null) return@collectLatest // 🚀 Safely skip until ID is resolved

                Timber.d("Loading comprehensive child configuration state")
                updateState { copy(isLoading = true) }

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
        if ((state.value.activeChild != null && state.value.settings != null) || state.value.allChildren.isEmpty()) {
            updateState { copy(isLoading = false) }
        }
    }

    override fun onEvent(event: ChildSettingsEvent) {
        when (event) {
            is ChildSettingsEvent.BackClicked -> sendEffect(NavigateBack)
            is ChildSettingsEvent.HelpClicked -> sendEffect(
                ShowToast(
                    FeatureToastType.UNDER_DEVELOPMENT
                )
            )

            is ChildSettingsEvent.OpenChildSheet -> updateState { copy(isChildSheetOpen = true) }
            is ChildSettingsEvent.CloseChildSheet -> updateState { copy(isChildSheetOpen = false) }
            is ChildSettingsEvent.SelectChild -> {
                Timber.d("Dashboard context switched to new child profile")
                updateState { copy(isChildSheetOpen = false, isLoading = true) }
                currentChildIdFlow.value = event.childId
                viewModelScope.launch { sessionManager.setViewedChildId(event.childId) }
            }

            is ChildSettingsEvent.GridItemClicked -> {
                if (event.route in listOf(
                        "location",
                        "prevent_delete",
                        "eye_protect",
                        "content_movies"
                    )
                ) {
                    sendEffect(ShowToast(FeatureToastType.COMING_SOON))
                    return
                }

                val requiredPermissions = featurePermissionsMap[event.route] ?: emptyList()
                val missingPermissions = requiredPermissions.filter {
                    !PermissionChecker.hasPermission(event.context, it)
                }

                if (missingPermissions.isEmpty()) {
                    Timber.d("OS permissions verified, launching feature: %s", event.route)
                    sendEffect(NavigateToFeature(event.route))
                } else {
                    Timber.w(
                        "Feature launch blocked due to missing OS permissions, missingCount: %d, feature: %s",
                        missingPermissions.size,
                        event.route
                    )
                    sendEffect(
                        NavigateToPermissionSlider(
                            event.route, missingPermissions.map { it.name })
                    )
                }
            }

            ChildSettingsEvent.AddChildClicked -> sendEffect(NavigateToFeature(Screen.AddChild.route))
        }
    }
}