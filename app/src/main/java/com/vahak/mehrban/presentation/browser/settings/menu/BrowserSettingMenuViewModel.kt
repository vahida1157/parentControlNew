package com.vahak.mehrban.presentation.browser.settings.menu

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserSettingsEntity
import com.vahak.mehrban.core.data.local.entity.FilterMode
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserSettingMenuState(
    val childId: String = "",
    val settings: BrowserSettingsEntity? = null,
    val allowedCount: Int = 0,
    val blockedCount: Int = 0,
    val keywordsCount: Int = 0,
    val isFilterMenuOpen: Boolean = false,
    val isEngineMenuOpen: Boolean = false,
    val isLoading: Boolean = true
)

sealed class BrowserSettingMenuEvent {
    object OnBackPress : BrowserSettingMenuEvent()
    data class SetFilterMenuOpen(val isOpen: Boolean) : BrowserSettingMenuEvent()
    data class SetEngineMenuOpen(val isOpen: Boolean) : BrowserSettingMenuEvent()
    data class ChangeFilterMode(val mode: FilterMode) : BrowserSettingMenuEvent()
    data class ChangeSearchEngine(val engineId: String) : BrowserSettingMenuEvent()
    data class ToggleCartoonWorld(val isEnabled: Boolean) : BrowserSettingMenuEvent()
}

sealed class BrowserSettingMenuEffect {
    object ExitScreen : BrowserSettingMenuEffect()
}

@HiltViewModel
class BrowserSettingMenuViewModel @Inject constructor(
    private val sessionManager: SessionManager, private val repository: SafeBrowserRepository
) : BaseViewModel<BrowserSettingMenuState, BrowserSettingMenuEvent, BrowserSettingMenuEffect>(
    BrowserSettingMenuState()
) {

    init {
        viewModelScope.launch {
            sessionManager.viewedChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    updateState { copy(childId = childId) }
                    repository.observeFullProfile(childId).collectLatest { profile ->
                        if (profile != null) {
                            updateState {
                                copy(
                                    settings = profile.settings,
                                    allowedCount = profile.allowedSites.count { it.isActive },
                                    blockedCount = profile.blockedSites.count { it.isActive },
                                    keywordsCount = profile.blockedKeywords.count { it.isActive },
                                    isLoading = false
                                )
                            }
                        } else {
                            updateState { copy(isLoading = false) }
                        }
                    }
                }
            }
        }
    }

    override fun onEvent(event: BrowserSettingMenuEvent) {
        val childId = state.value.childId
        if (childId.isEmpty()) return

        viewModelScope.launch {
            when (event) {
                is BrowserSettingMenuEvent.OnBackPress -> {
                    sendEffect(BrowserSettingMenuEffect.ExitScreen)

                    CoroutineScope(Dispatchers.IO).launch {
                        repository.pushBrowserDataToServer(childId)
                    }
                }

                is BrowserSettingMenuEvent.SetFilterMenuOpen -> updateState { copy(isFilterMenuOpen = event.isOpen) }
                is BrowserSettingMenuEvent.SetEngineMenuOpen -> updateState { copy(isEngineMenuOpen = event.isOpen) }
                is BrowserSettingMenuEvent.ChangeFilterMode -> repository.updateFilterMode(
                    childId, event.mode
                )

                is BrowserSettingMenuEvent.ChangeSearchEngine -> {
                    repository.updateSearchEngine(childId, event.engineId)
                    updateState { copy(isEngineMenuOpen = false) }
                }

                is BrowserSettingMenuEvent.ToggleCartoonWorld -> repository.updateCartoonWorld(
                    childId, event.isEnabled
                )
            }
        }
    }
}