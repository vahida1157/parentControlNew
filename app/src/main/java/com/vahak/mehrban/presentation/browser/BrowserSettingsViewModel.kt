package com.vahak.mehrban.presentation.browser

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserWhitelistEntity
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserSettingsState(
    val childId: String = "",
    val whitelist: List<BrowserWhitelistEntity> = emptyList(),
    val keywords: List<BrowserKeywordEntity> = emptyList(),
    val isLoading: Boolean = true
)

sealed class BrowserSettingsEvent {
    data class AddSite(val url: String, val label: String) : BrowserSettingsEvent()
    data class RemoveSite(val url: String, val label: String) : BrowserSettingsEvent()
    
    data class AddKeyword(val keyword: String) : BrowserSettingsEvent()
    data class RemoveKeyword(val keyword: String) : BrowserSettingsEvent()
}

sealed class BrowserSettingsEffect {
    data class ShowToast(val message: String) : BrowserSettingsEffect()
}

@HiltViewModel
class BrowserSettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: SafeBrowserRepository
) : BaseViewModel<BrowserSettingsState, BrowserSettingsEvent, BrowserSettingsEffect>(BrowserSettingsState()) {

    init {
        viewModelScope.launch {
            // 🚀 The parent is configuring the currently viewed child
            sessionManager.viewedChildIdFlow.distinctUntilChanged().collectLatest { childId ->
                if (childId != null) {
                    updateState { copy(childId = childId) }
                    observeData(childId)
                }
            }
        }
    }

    private fun observeData(childId: String) {
        viewModelScope.launch {
            repository.observeWhitelist(childId).collectLatest { sites ->
                updateState { copy(whitelist = sites, isLoading = false) }
            }
        }
        viewModelScope.launch {
            repository.observeKeywords(childId).collectLatest { words ->
                updateState { copy(keywords = words) }
            }
        }
    }

    override fun onEvent(event: BrowserSettingsEvent) {
        val childId = state.value.childId
        if (childId.isEmpty()) return

        viewModelScope.launch {
            when (event) {
                is BrowserSettingsEvent.AddSite -> {
                    val cleanUrl = event.url.lowercase().replace("https://", "").replace("http://", "")
                    repository.toggleWhitelistSite(childId, cleanUrl, event.label, isActive = true)
                    sendEffect(BrowserSettingsEffect.ShowToast("سایت اضافه شد"))
                }
                is BrowserSettingsEvent.RemoveSite -> {
                    // Soft delete by setting isActive = false
                    repository.toggleWhitelistSite(childId, event.url, event.label, isActive = false)
                }
                is BrowserSettingsEvent.AddKeyword -> {
                    repository.toggleKeyword(childId, event.keyword.lowercase(), isActive = true)
                    sendEffect(BrowserSettingsEffect.ShowToast("کلمه اضافه شد"))
                }
                is BrowserSettingsEvent.RemoveKeyword -> {
                    repository.toggleKeyword(childId, event.keyword, isActive = false)
                }
            }
        }
    }
}