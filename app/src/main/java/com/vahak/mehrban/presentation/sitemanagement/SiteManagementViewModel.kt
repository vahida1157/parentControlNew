package com.vahak.mehrban.presentation.sitemanagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.entity.BlockedDomainEntity
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.domain.repository.WebRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- Contract ---
data class SiteManagementState(
    val childId: String = "",
    val isSiteManagementActive: Boolean = false,
    val domainInput: String = "",
    val blockedDomains: List<BlockedDomainEntity> = emptyList()
)

sealed class SiteManagementEvent {
    object BackClicked : SiteManagementEvent()
    data class ToggleActive(val isActive: Boolean) : SiteManagementEvent()
    data class DomainInputChanged(val input: String) : SiteManagementEvent()
    object AddDomainClicked : SiteManagementEvent()
    data class RemoveDomainClicked(val domain: BlockedDomainEntity) : SiteManagementEvent()
    data class ToggleDomainStatus(val domain: BlockedDomainEntity, val isActive: Boolean) :
        SiteManagementEvent()
}

sealed class SiteManagementEffect {
    object NavigateBack : SiteManagementEffect()
    data class ShowToast(val message: String) : SiteManagementEffect()

}

@HiltViewModel
class SiteManagementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val webRepository: WebRepository,
) : BaseViewModel<SiteManagementState, SiteManagementEvent, SiteManagementEffect>(
    SiteManagementState()
) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        updateState { copy(childId = this@SiteManagementViewModel.childId) }

        // Trigger a background sync when the screen opens
        viewModelScope.launch {
            webRepository.syncDomainsFromServer(childId)
        }

        // 1. Observe the Master Toggle (via Repository)
        viewModelScope.launch {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState { copy(isSiteManagementActive = settings.isSiteManagementActive) }
                }
            }
        }

        // 2. Observe the Blocked Domains List (via Repository)
        viewModelScope.launch {
            webRepository.observeBlockedDomains(childId).collectLatest { domains ->
                updateState { copy(blockedDomains = domains) }
            }
        }
    }

    override fun onEvent(event: SiteManagementEvent) {
        when (event) {
            is SiteManagementEvent.BackClicked -> sendEffect(SiteManagementEffect.NavigateBack)

            is SiteManagementEvent.ToggleActive -> {
                viewModelScope.launch {
                    // This now properly flags is_synced = 0 and pushes to the server!
                    settingsRepository.updateSiteManagementToggle(childId, event.isActive)
                }
            }

            is SiteManagementEvent.DomainInputChanged -> {
                updateState { copy(domainInput = event.input) }
            }

            is SiteManagementEvent.AddDomainClicked -> {
                val input = state.value.domainInput.trim().lowercase()
                if (input.isBlank() || !input.contains(".")) {
                    sendEffect(SiteManagementEffect.ShowToast("لطفاً یک آدرس سایت معتبر وارد کنید (مثال: example.com)"))
                    return
                }

                viewModelScope.launch {
                    webRepository.addDomain(childId, input)
                    updateState { copy(domainInput = "") }
                }
            }

            is SiteManagementEvent.RemoveDomainClicked -> {
                viewModelScope.launch {
                    webRepository.removeDomain(event.domain)
                }
            }

            is SiteManagementEvent.ToggleDomainStatus -> {
                viewModelScope.launch {
                    webRepository.toggleDomainStatus(event.domain.id, event.isActive)
                }
            }
        }
    }
}