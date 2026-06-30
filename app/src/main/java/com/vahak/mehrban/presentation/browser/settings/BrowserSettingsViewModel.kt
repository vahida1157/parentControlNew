package com.vahak.mehrban.presentation.browser.settings

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.*
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class BrowserSettingsPage { MENU, ALLOWED_SITES, BLOCKED_SITES, KEYWORDS, HISTORY }

data class BrowserSettingsState(
    val childId: String = "",
    val activePage: BrowserSettingsPage = BrowserSettingsPage.MENU,

    val originalProfile: FullBrowserProfile? = null,

    val draftSettings: BrowserSettingsEntity? = null,
    val draftAllowedSites: List<BrowserAllowedSiteEntity> = emptyList(),
    val draftBlockedSites: List<BrowserBlockedSiteEntity> = emptyList(),
    val draftKeywords: List<BrowserBlockedKeywordEntity> = emptyList(),

    val history: List<BrowserHistoryEntity> = emptyList(),
    val selectedDateMillis: Long = System.currentTimeMillis(), // 🚀 Tracks current viewing date
    val isEngineMenuOpen: Boolean = false,
    val isLoading: Boolean = true,
    val showUnsavedDialog: Boolean = false
) {
    val hasUnsavedChanges: Boolean
        get() = originalProfile?.settings != draftSettings ||
                originalProfile?.allowedSites != draftAllowedSites ||
                originalProfile.blockedSites != draftBlockedSites ||
                originalProfile.blockedKeywords != draftKeywords
}

sealed class BrowserSettingsEvent {
    data class NavigateTo(val page: BrowserSettingsPage) : BrowserSettingsEvent()
    object OnBackPress : BrowserSettingsEvent()
    object SaveAndExit : BrowserSettingsEvent()
    object DiscardAndExit : BrowserSettingsEvent()
    object DismissUnsavedDialog : BrowserSettingsEvent()

    // 🚀 NEW: Date Navigation
    data class ChangeHistoryDate(val offsetDays: Int) : BrowserSettingsEvent()

    data class ChangeFilterMode(val mode: FilterMode) : BrowserSettingsEvent()
    data class SetEngineMenuOpen(val isOpen: Boolean) : BrowserSettingsEvent()
    data class ChangeSearchEngine(val engineId: String) : BrowserSettingsEvent()
    data class ToggleCartoonWorld(val isEnabled: Boolean) : BrowserSettingsEvent()

    data class AddAllowedSite(val url: String, val label: String) : BrowserSettingsEvent()
    data class RemoveAllowedSite(val url: String) : BrowserSettingsEvent()
    data class AddBlockedSite(val url: String) : BrowserSettingsEvent()
    data class RemoveBlockedSite(val url: String) : BrowserSettingsEvent()
    data class AddBlockedKeyword(val keyword: String) : BrowserSettingsEvent()
    data class RemoveBlockedKeyword(val keyword: String) : BrowserSettingsEvent()
}

sealed class BrowserSettingsEffect {
    data class ShowToast(val messageResId: Int) : BrowserSettingsEffect()
    object ExitScreen : BrowserSettingsEffect()
}

@HiltViewModel
class BrowserSettingsViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: SafeBrowserRepository
) : BaseViewModel<BrowserSettingsState, BrowserSettingsEvent, BrowserSettingsEffect>(BrowserSettingsState()) {

    private var hasInitializedDraft = false
    private var historyJob: Job? = null // 🚀 Keep track of the DB query job

    init {
        viewModelScope.launch {
            sessionManager.viewedChildIdFlow.distinctUntilChanged().collectLatest { childId ->
                if (childId != null) {
                    updateState { copy(childId = childId) }
                    observeProfile(childId)
                    observeHistory(childId, state.value.selectedDateMillis) // Load today's history initially
                }
            }
        }
    }

    private fun observeProfile(childId: String) {
        viewModelScope.launch {
            repository.observeFullProfile(childId).collectLatest { profile ->
                // 🚀 THE FIX: If profile is null (fresh install), provide default settings so the UI doesn't freeze
                val activeProfile = profile ?: FullBrowserProfile(
                    settings = BrowserSettingsEntity(childId = childId, searchEngine = "kiddle", filterMode = FilterMode.WHITELIST_ONLY),
                    allowedSites = emptyList(),
                    blockedSites = emptyList(),
                    blockedKeywords = emptyList()
                )

                val filteredProfile = activeProfile.copy(
                    allowedSites = activeProfile.allowedSites.filter { it.isActive },
                    blockedSites = activeProfile.blockedSites.filter { it.isActive },
                    blockedKeywords = activeProfile.blockedKeywords.filter { it.isActive }
                )

                updateState { copy(originalProfile = filteredProfile, isLoading = false) }

                // Only overwrite the draft if we haven't started editing
                if (!hasInitializedDraft) {
                    updateState {
                        copy(
                            draftSettings = filteredProfile.settings,
                            draftAllowedSites = filteredProfile.allowedSites,
                            draftBlockedSites = filteredProfile.blockedSites,
                            draftKeywords = filteredProfile.blockedKeywords
                        )
                    }
                    hasInitializedDraft = true
                }
            }
        }
    }

    // 🚀 NEW: Loads history ONLY for the selected 24-hour window
    private fun observeHistory(childId: String, dateMillis: Long) {
        historyJob?.cancel() // Cancel previous day's query

        historyJob = viewModelScope.launch {
            val calendar = Calendar.getInstance().apply { timeInMillis = dateMillis }

            // Set to Start of Day (00:00:00)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            // Set to End of Day (23:59:59)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            calendar.set(Calendar.SECOND, 59)
            calendar.set(Calendar.MILLISECOND, 999)
            val endOfDay = calendar.timeInMillis

            repository.observeHistoryForDate(childId, startOfDay, endOfDay).collectLatest { historyList ->
                updateState { copy(history = historyList) }
            }
        }
    }

    override fun onEvent(event: BrowserSettingsEvent) {
        val childId = state.value.childId

        viewModelScope.launch {
            // 🚀 THE FIX: Process navigation and dialog events ALWAYS, regardless of database state
            when (event) {
                is BrowserSettingsEvent.NavigateTo -> {
                    updateState { copy(activePage = event.page) }
                    return@launch
                }
                is BrowserSettingsEvent.OnBackPress -> {
                    if (state.value.activePage == BrowserSettingsPage.MENU) {
                        if (state.value.hasUnsavedChanges) {
                            updateState { copy(showUnsavedDialog = true) }
                        } else {
                            sendEffect(BrowserSettingsEffect.ExitScreen)
                        }
                    } else {
                        updateState { copy(activePage = BrowserSettingsPage.MENU) }
                    }
                    return@launch
                }
                is BrowserSettingsEvent.DismissUnsavedDialog -> {
                    updateState { copy(showUnsavedDialog = false) }
                    return@launch
                }
                is BrowserSettingsEvent.DiscardAndExit -> {
                    updateState { copy(showUnsavedDialog = false) }
                    sendEffect(BrowserSettingsEffect.ExitScreen)
                    return@launch
                }
                else -> {} // Pass through to data modification events below
            }

            // 🚀 Now we block data-modifying events if the database isn't ready yet
            if (childId.isEmpty() || state.value.draftSettings == null) return@launch

            when (event) {
                is BrowserSettingsEvent.ChangeHistoryDate -> {
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = state.value.selectedDateMillis }
                    cal.add(java.util.Calendar.DAY_OF_YEAR, event.offsetDays)
                    val newDate = cal.timeInMillis

                    updateState { copy(selectedDateMillis = newDate) }
                    observeHistory(childId, newDate)
                }

                is BrowserSettingsEvent.SaveAndExit -> {
                    val orig = state.value.originalProfile ?: return@launch
                    val draftSet = state.value.draftSettings ?: return@launch

                    if (orig.settings.filterMode != draftSet.filterMode) repository.updateFilterMode(childId, draftSet.filterMode)
                    if (orig.settings.searchEngine != draftSet.searchEngine) repository.updateSearchEngine(childId, draftSet.searchEngine)
                    if (orig.settings.isCartoonWorldEnabled != draftSet.isCartoonWorldEnabled) repository.updateCartoonWorld(childId, draftSet.isCartoonWorldEnabled)

                    val draftAllowedUrls = state.value.draftAllowedSites.map { it.url }.toSet()
                    val origAllowedUrls = orig.allowedSites.map { it.url }.toSet()
                    state.value.draftAllowedSites.filter { it.url !in origAllowedUrls }.forEach { repository.addAllowedSite(childId, it.url, it.label) }
                    orig.allowedSites.filter { it.url !in draftAllowedUrls }.forEach { repository.removeAllowedSite(childId, it.url) }

                    val draftBlockedUrls = state.value.draftBlockedSites.map { it.url }.toSet()
                    val origBlockedUrls = orig.blockedSites.map { it.url }.toSet()
                    state.value.draftBlockedSites.filter { it.url !in origBlockedUrls }.forEach { repository.addBlockedSite(childId, it.url) }
                    orig.blockedSites.filter { it.url !in draftBlockedUrls }.forEach { repository.removeBlockedSite(childId, it.url) }

                    val draftKeywords = state.value.draftKeywords.map { it.keyword }.toSet()
                    val origKeywords = orig.blockedKeywords.map { it.keyword }.toSet()
                    state.value.draftKeywords.filter { it.keyword !in origKeywords }.forEach { repository.addBlockedKeyword(childId, it.keyword) }
                    orig.blockedKeywords.filter { it.keyword !in draftKeywords }.forEach { repository.removeBlockedKeyword(childId, it.keyword) }

                    updateState { copy(showUnsavedDialog = false) }
                    sendEffect(BrowserSettingsEffect.ShowToast(R.string.saved_successfully))
                    sendEffect(BrowserSettingsEffect.ExitScreen)
                }

                is BrowserSettingsEvent.ChangeFilterMode -> updateState { copy(draftSettings = draftSettings?.copy(filterMode = event.mode)) }
                is BrowserSettingsEvent.SetEngineMenuOpen -> updateState { copy(isEngineMenuOpen = event.isOpen) }
                is BrowserSettingsEvent.ChangeSearchEngine -> updateState { copy(draftSettings = draftSettings?.copy(searchEngine = event.engineId), isEngineMenuOpen = false) }
                is BrowserSettingsEvent.ToggleCartoonWorld -> updateState { copy(draftSettings = draftSettings?.copy(isCartoonWorldEnabled = event.isEnabled)) }

                is BrowserSettingsEvent.AddAllowedSite -> updateState { copy(draftAllowedSites = draftAllowedSites + BrowserAllowedSiteEntity(childId = childId, url = event.url.lowercase(), label = event.label)) }
                is BrowserSettingsEvent.RemoveAllowedSite -> updateState { copy(draftAllowedSites = draftAllowedSites.filter { it.url != event.url }) }
                is BrowserSettingsEvent.AddBlockedSite -> updateState { copy(draftBlockedSites = draftBlockedSites + BrowserBlockedSiteEntity(childId = childId, url = event.url.lowercase())) }
                is BrowserSettingsEvent.RemoveBlockedSite -> updateState { copy(draftBlockedSites = draftBlockedSites.filter { it.url != event.url }) }
                is BrowserSettingsEvent.AddBlockedKeyword -> updateState { copy(draftKeywords = draftKeywords + BrowserBlockedKeywordEntity(childId = childId, keyword = event.keyword.lowercase())) }
                is BrowserSettingsEvent.RemoveBlockedKeyword -> updateState { copy(draftKeywords = draftKeywords.filter { it.keyword != event.keyword }) }
                else -> {}
            }
        }
    }
}