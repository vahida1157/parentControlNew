package com.vahak.parentcontroll.presentation.setting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
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
    data class GridItemClicked(val route: String) : SettingsEvent()
    object BackClicked : SettingsEvent()
    object HelpClicked : SettingsEvent()
}

sealed class SettingsEffect {
    object NavigateBack : SettingsEffect()
    data class NavigateToFeature(val route: String) : SettingsEffect()
    data class ShowToast(val message: String) : SettingsEffect()
}

// 2. ViewModel
@HiltViewModel
class SettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsDao: SettingsDao
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

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleChildTheme -> {
                viewModelScope.launch {
                    // Update database directly; UI will react automatically via Flow
                    val currentSettings = state.value.settings ?: return@launch
                    settingsDao.upsertGlobalSettings(currentSettings.copy(isChildThemeActive = event.isActive))
                }
            }
            is SettingsEvent.GridItemClicked -> sendEffect(SettingsEffect.NavigateToFeature(event.route))
            is SettingsEvent.BackClicked -> sendEffect(SettingsEffect.NavigateBack)
            is SettingsEvent.HelpClicked -> sendEffect(SettingsEffect.ShowToast("بخش راهنما در حال توسعه است."))
        }
    }
}