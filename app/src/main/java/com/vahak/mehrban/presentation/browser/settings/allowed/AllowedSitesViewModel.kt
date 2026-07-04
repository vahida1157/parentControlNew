package com.vahak.mehrban.presentation.browser.settings.allowed

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.domain.usecase.BrowserUrlValidationResult
import com.vahak.mehrban.domain.usecase.ValidateBrowserUrlUseCase
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AllowedSitesState(
    val childId: String = "",
    val sites: List<BrowserAllowedSiteEntity> = emptyList()
)

sealed class AllowedSitesEvent {
    data class AddSite(val url: String, val label: String) : AllowedSitesEvent()
    data class EditSite(val oldUrl: String, val newUrl: String, val newLabel: String) : AllowedSitesEvent()
    data class RemoveSite(val url: String) : AllowedSitesEvent()
}

sealed class AllowedSitesEffect {
    data class ShowToast(val messageResId: Int) : AllowedSitesEffect()
}

@HiltViewModel
class AllowedSitesViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: SafeBrowserRepository,
    private val validateUrlUseCase: ValidateBrowserUrlUseCase
) : BaseViewModel<AllowedSitesState, AllowedSitesEvent, AllowedSitesEffect>(AllowedSitesState()) {

    init {
        viewModelScope.launch {
            sessionManager.viewedChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    updateState { copy(childId = childId) }
                    repository.observeFullProfile(childId).collectLatest { profile ->
                        updateState { copy(sites = profile?.allowedSites?.filter { it.isActive } ?: emptyList()) }
                    }
                }
            }
        }
    }

    // 🚀 Pure pass-through: No Pairs, No Enums
    fun validateUrl(url: String): BrowserUrlValidationResult = validateUrlUseCase.execute(url)

    override fun onEvent(event: AllowedSitesEvent) {
        val childId = state.value.childId
        if (childId.isEmpty()) return

        viewModelScope.launch {
            when (event) {
                is AllowedSitesEvent.AddSite -> {
                    val validation = validateUrlUseCase.execute(event.url)
                    if (validation is BrowserUrlValidationResult.Error) {
                        sendEffect(AllowedSitesEffect.ShowToast(validation.messageRes))
                        return@launch
                    }
                    val cleanUrl = validateUrlUseCase.clean(event.url)
                    repository.addAllowedSite(childId, cleanUrl, event.label.trim())
                }

                is AllowedSitesEvent.EditSite -> {
                    val validation = validateUrlUseCase.execute(event.newUrl)
                    if (validation is BrowserUrlValidationResult.Error) {
                        sendEffect(AllowedSitesEffect.ShowToast(validation.messageRes))
                        return@launch
                    }
                    val cleanUrl = validateUrlUseCase.clean(event.newUrl)
                    if (event.oldUrl != cleanUrl) repository.removeAllowedSite(childId, event.oldUrl)
                    repository.addAllowedSite(childId, cleanUrl, event.newLabel.trim())
                }

                is AllowedSitesEvent.RemoveSite -> repository.removeAllowedSite(childId, event.url)
            }
        }
    }
}