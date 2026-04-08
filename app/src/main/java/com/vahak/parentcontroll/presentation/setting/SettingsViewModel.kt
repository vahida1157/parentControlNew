package com.vahak.parentcontroll.presentation.setting

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import com.vahak.parentcontroll.core.util.PermissionChecker
import com.vahak.parentcontroll.core.util.PermissionType
import com.vahak.parentcontroll.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// 1. Contract
data class SettingsState(
    val childId: String = "",
    val settings: GlobalSettingsEntity? = null,
    val isLoading: Boolean = true
)

sealed class SettingsEvent {
    data class ToggleChildTheme(val isActive: Boolean) : SettingsEvent()
    data class GridItemClicked(val route: String, val context: Context) : SettingsEvent()
    object BackClicked : SettingsEvent()
    object HelpClicked : SettingsEvent()
}

sealed class SettingsEffect {
    object NavigateBack : SettingsEffect()
    data class NavigateToFeature(val route: String) : SettingsEffect()
    data class ShowToast(val message: String) : SettingsEffect()
    data class NavigateToPermissionSlider(val route: String, val missingPermissions: List<String>) :
        SettingsEffect()
}

// 2. ViewModel
@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle, private val settingsDao: SettingsDao
) : BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    // Automatically extracts the ID from the NavGraph route: "child_settings/{childId}"
    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        updateState { copy(childId = this@SettingsViewModel.childId) }

        // Reactively listen to the database for this specific child
        viewModelScope.launch {
            settingsDao.getGlobalSettings(childId).collectLatest { entity ->
                updateState { copy(settings = entity, isLoading = false) }
            }
        }
    }

    private val featurePermissionsMap = mapOf(
        "time_limit" to listOf(PermissionType.USAGE_STATS, PermissionType.OVERLAY),
        "app_lock" to listOf(PermissionType.USAGE_STATS, PermissionType.OVERLAY),
        "location" to listOf(PermissionType.LOCATION)
    )

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleChildTheme -> {
                viewModelScope.launch {
                    // Update database directly; UI will react automatically via Flow
                    val currentSettings = state.value.settings ?: return@launch
                    settingsDao.upsertGlobalSettings(currentSettings.copy(isChildThemeActive = event.isActive))
                }
            }

            is SettingsEvent.BackClicked -> sendEffect(SettingsEffect.NavigateBack)
            is SettingsEvent.HelpClicked -> sendEffect(SettingsEffect.ShowToast("بخش راهنما در حال توسعه است."))
            is SettingsEvent.GridItemClicked -> {

                if (event.route in listOf(
                        "location",
                        "sleep_time",
                        "site_management",
                        "safe_search",
                        "prevent_delete",
                        "eye_protect",
                        "content_movies"
                    )
                ) {
                    sendEffect(SettingsEffect.ShowToast("این قابلیت به زودی اضافه خواهد شد!"))
                    return // Stop executing the rest of the block
                }

                val requiredPermissions = featurePermissionsMap[event.route] ?: emptyList()

                // Filter down to only the permissions the user HASN'T granted yet
                val missingPermissions = requiredPermissions.filter { permission ->
                    !PermissionChecker.hasPermission(event.context, permission)
                }

                if (missingPermissions.isEmpty()) {
                    // All good! Go straight to the feature.
                    sendEffect(SettingsEffect.NavigateToFeature(event.route))
                } else {
                    // Intercept! Go to the slider screen first.
                    // We pass the names of the enums so the slider knows what to show.
                    val missingNames = missingPermissions.map { it.name }
                    sendEffect(SettingsEffect.NavigateToPermissionSlider(event.route, missingNames))
                }
            }
        }
    }
}