package com.vahak.mehrban.presentation.sitemanagement

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.BlockedDomainEntity
import com.vahak.mehrban.domain.repository.SettingsRepository
import com.vahak.mehrban.domain.repository.WebRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
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
    @ApplicationContext private val context: Context
) : BaseViewModel<SiteManagementState, SiteManagementEvent, SiteManagementEffect>(
    SiteManagementState()
) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        updateState { copy(childId = this@SiteManagementViewModel.childId) }

        viewModelScope.launch {
            Timber.d("Initiating background synchronization for web filtering domains")
            webRepository.syncDomainsFromServer(childId)
        }

        viewModelScope.launch {
            settingsRepository.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState { copy(isSiteManagementActive = settings.isSiteManagementActive) }
                }
            }
        }

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
                Timber.i("Site management master toggle modified, isActive: %b", event.isActive)
                viewModelScope.launch {
                    settingsRepository.updateSiteManagementToggle(childId, event.isActive)
                }
            }

            is SiteManagementEvent.DomainInputChanged -> updateState { copy(domainInput = event.input) }

            is SiteManagementEvent.AddDomainClicked -> {
                val input = state.value.domainInput.trim().lowercase()
                if (input.isBlank() || !input.contains(".")) {
                    Timber.w("Domain addition rejected due to malformed input")
                    sendEffect(SiteManagementEffect.ShowToast(context.getString(R.string.invalid_domain_format)))
                    return
                }

                Timber.i("Adding new domain definition to site blocklist: %s", input)
                viewModelScope.launch {
                    webRepository.addDomain(childId, input)
                    updateState { copy(domainInput = "") }
                }
            }

            is SiteManagementEvent.RemoveDomainClicked -> {
                Timber.i("Deleting domain definition from site blocklist")
                viewModelScope.launch {
                    webRepository.removeDomain(event.domain)
                }
            }

            is SiteManagementEvent.ToggleDomainStatus -> {
                Timber.d(
                    "Toggling domain definition restriction status, isActive: %b", event.isActive
                )
                viewModelScope.launch {
                    webRepository.toggleDomainStatus(event.domain.id, event.isActive)
                }
            }
        }
    }
}