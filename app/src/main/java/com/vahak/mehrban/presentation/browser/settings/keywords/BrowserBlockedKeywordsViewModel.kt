package com.vahak.mehrban.presentation.browser.settings.keywords

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedKeywordEntity
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BrowserBlockedKeywordsState(
    val childId: String = "",
    val keywords: List<BrowserBlockedKeywordEntity> = emptyList()
)

sealed class BrowserBlockedKeywordsEvent {
    data class AddKeyword(val keyword: String) : BrowserBlockedKeywordsEvent()
    data class EditKeyword(val oldKeyword: String, val newKeyword: String) : BrowserBlockedKeywordsEvent()
    data class RemoveKeyword(val keyword: String) : BrowserBlockedKeywordsEvent()
}

sealed class BrowserBlockedKeywordsEffect

@HiltViewModel
class BrowserBlockedKeywordsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: SafeBrowserRepository
) : BaseViewModel<BrowserBlockedKeywordsState, BrowserBlockedKeywordsEvent, BrowserBlockedKeywordsEffect>(BrowserBlockedKeywordsState()) {

    init {
        viewModelScope.launch {
            sessionManager.viewedChildIdFlow.collectLatest { childId ->
                if (childId != null) {
                    updateState { copy(childId = childId) }
                    repository.observeFullProfile(childId).collectLatest { profile ->
                        updateState { copy(keywords = profile?.blockedKeywords?.filter { it.isActive } ?: emptyList()) }
                    }
                }
            }
        }
    }

    override fun onEvent(event: BrowserBlockedKeywordsEvent) {
        val childId = state.value.childId
        if (childId.isEmpty()) return

        viewModelScope.launch {
            when (event) {
                is BrowserBlockedKeywordsEvent.AddKeyword -> {
                    val cleanKw = event.keyword.trim().lowercase()
                    if (cleanKw.isNotBlank()) repository.addBlockedKeyword(childId, cleanKw)
                }
                is BrowserBlockedKeywordsEvent.EditKeyword -> {
                    val cleanNew = event.newKeyword.trim().lowercase()
                    if (cleanNew.isNotBlank()) {
                        if (event.oldKeyword != cleanNew) repository.removeBlockedKeyword(childId, event.oldKeyword)
                        repository.addBlockedKeyword(childId, cleanNew)
                    }
                }
                is BrowserBlockedKeywordsEvent.RemoveKeyword -> repository.removeBlockedKeyword(childId, event.keyword)
            }
        }
    }
}