package com.vahak.parentcontroll.presentation.setting

import com.vahak.parentcontroll.presentation.BaseViewModel

// 1. Contract Definition
data class SettingsState(
    val isChildThemeActive: Boolean = true,
    // You can add more states here later, like:
    // val isGamesLocked: Boolean = true
)

sealed class SettingsEvent {
    data class ToggleChildTheme(val isActive: Boolean) : SettingsEvent()
    data class GridItemClicked(val itemName: String) : SettingsEvent()
    object BackClicked : SettingsEvent()
    object HelpClicked : SettingsEvent()
}

sealed class SettingsEffect {
    object NavigateBack : SettingsEffect()
    data class ShowToast(val message: String) : SettingsEffect()
}

// 2. ViewModel Implementation
class SettingsViewModel :
    BaseViewModel<SettingsState, SettingsEvent, SettingsEffect>(SettingsState()) {

    override fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.ToggleChildTheme -> {
                updateState { copy(isChildThemeActive = event.isActive) }
                // In the future: Call repository to sync this setting with Spring backend
                val status = if (event.isActive) "فعال" else "غیرفعال"
                sendEffect(SettingsEffect.ShowToast("پوسته کودکانه $status شد."))
            }

            is SettingsEvent.GridItemClicked -> {
                sendEffect(SettingsEffect.ShowToast("تنظیمات ${event.itemName} به زودی..."))
            }

            is SettingsEvent.BackClicked -> {
                sendEffect(SettingsEffect.NavigateBack)
            }

            is SettingsEvent.HelpClicked -> {
                sendEffect(SettingsEffect.ShowToast("بخش راهنما در حال توسعه است."))
            }
        }
    }
}