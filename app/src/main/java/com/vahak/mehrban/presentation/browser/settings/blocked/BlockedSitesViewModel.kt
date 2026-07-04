package com.vahak.mehrban.presentation.browser.settings.blocked

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedSiteEntity
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.domain.usecase.BrowserUrlValidationResult
import com.vahak.mehrban.domain.usecase.ValidateBrowserUrlUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedSitesState(
    val childId: String = "",
    val sites: List<BrowserBlockedSiteEntity> = emptyList()
)

sealed class BlockedSitesEvent {
    data class AddSite(val url: String) : BlockedSitesEvent()
    data class EditSite(val oldUrl: String, val newUrl: String) : BlockedSitesEvent()
    data class RemoveSite(val url: String) : BlockedSitesEvent()
}

sealed class BlockedSitesEffect {
    data class ShowToast(val messageResId: Int) : BlockedSitesEffect()
}

@HiltViewModel
class BlockedSitesViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: SafeBrowserRepository,
    private val validateUrlUseCase: ValidateBrowserUrlUseCase
) : BaseViewModel<BlockedSitesState, BlockedSitesEvent, BlockedSitesEffect>(BlockedSitesState()) {

    init {
        viewModelScope.launch {
            sessionManager.viewedChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    updateState { copy(childId = childId) }
                    repository.observeFullProfile(childId).collectLatest { profile ->
                        updateState { copy(sites = profile?.blockedSites?.filter { it.isActive } ?: emptyList()) }
                    }
                }
            }
        }
    }

    // 🚀 Pure pass-through: No Pairs, No Enums
    fun validateUrl(url: String): BrowserUrlValidationResult = validateUrlUseCase.execute(url)

    override fun onEvent(event: BlockedSitesEvent) {
        val childId = state.value.childId
        if (childId.isEmpty()) return

        viewModelScope.launch {
            when (event) {
                is BlockedSitesEvent.AddSite -> {
                    val validation = validateUrlUseCase.execute(event.url)
                    if (validation is BrowserUrlValidationResult.Error) {
                        sendEffect(BlockedSitesEffect.ShowToast(validation.messageRes))
                        return@launch
                    }
                    val cleanUrl = validateUrlUseCase.clean(event.url)
                    repository.addBlockedSite(childId, cleanUrl)
                }

                is BlockedSitesEvent.EditSite -> {
                    val validation = validateUrlUseCase.execute(event.newUrl)
                    if (validation is BrowserUrlValidationResult.Error) {
                        sendEffect(BlockedSitesEffect.ShowToast(validation.messageRes))
                        return@launch
                    }
                    val cleanUrl = validateUrlUseCase.clean(event.newUrl)
                    if (event.oldUrl != cleanUrl) repository.removeBlockedSite(childId, event.oldUrl)
                    repository.addBlockedSite(childId, cleanUrl)
                }

                is BlockedSitesEvent.RemoveSite -> repository.removeBlockedSite(childId, event.url)
            }
        }
    }
}