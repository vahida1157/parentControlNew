package com.vahak.parentcontroll.presentation.sitemanagement

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.vahak.parentcontroll.core.data.local.dao.SettingsDao
import com.vahak.parentcontroll.core.data.local.dao.WebDao
import com.vahak.parentcontroll.core.data.local.entity.BlockedDomainEntity
import com.vahak.parentcontroll.presentation.BaseViewModel
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
    data class ToggleDomainStatus(val domain: BlockedDomainEntity, val isActive: Boolean) : SiteManagementEvent()
}

sealed class SiteManagementEffect {
    object NavigateBack : SiteManagementEffect()
    data class ShowToast(val message: String) : SiteManagementEffect()

}

@HiltViewModel
class SiteManagementViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsDao: SettingsDao,
    private val webDao: WebDao
) : BaseViewModel<SiteManagementState, SiteManagementEvent, SiteManagementEffect>(SiteManagementState()) {

    private val childId: String = checkNotNull(savedStateHandle["childId"])

    init {
        updateState { copy(childId = this@SiteManagementViewModel.childId) }

        // 1. Observe the Master Toggle
        viewModelScope.launch {
            settingsDao.getGlobalSettings(childId).collectLatest { settings ->
                if (settings != null) {
                    updateState { copy(isSiteManagementActive = settings.isSiteManagementActive) }
                }
            }
        }

        // 2. Observe the Blocked Domains List
        viewModelScope.launch {
            webDao.observeBlockedDomains(childId).collectLatest { domains ->
                updateState { copy(blockedDomains = domains) }
            }
        }
    }

    override fun onEvent(event: SiteManagementEvent) {
        when (event) {
            is SiteManagementEvent.BackClicked -> sendEffect(SiteManagementEffect.NavigateBack)
            
            is SiteManagementEvent.ToggleActive -> {
                viewModelScope.launch {
                    settingsDao.updateSiteManagementToggle(childId, event.isActive)
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
                    val newDomain = BlockedDomainEntity(childId = childId, domain = input)
                    webDao.insertDomain(newDomain)
                    updateState { copy(domainInput = "") } // Clear input after adding
                }
            }
            
            is SiteManagementEvent.RemoveDomainClicked -> {
                viewModelScope.launch { webDao.deleteDomain(event.domain) }
            }
            
            is SiteManagementEvent.ToggleDomainStatus -> {
                viewModelScope.launch {
                    webDao.setDomainActive(event.domain.id, event.isActive)
                }
            }
        }
    }
}