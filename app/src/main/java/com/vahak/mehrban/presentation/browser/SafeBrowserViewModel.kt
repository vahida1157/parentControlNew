package com.vahak.mehrban.presentation.browser

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.*
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

data class BrowserState(
    val childId: String = "",
    val currentUrl: String = "",
    val inputText: String = "",
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,

    // Extracted from FullBrowserProfile
    val searchEngine: String = "kiddle",
    val isCartoonWorldEnabled: Boolean = true,
    val filterMode: FilterMode = FilterMode.WHITELIST_ONLY,
    val allowedSites: List<BrowserAllowedSiteEntity> = emptyList(),
    val blockedSites: List<BrowserBlockedSiteEntity> = emptyList(),
    val blockedKeywords: List<BrowserBlockedKeywordEntity> = emptyList()
)

sealed class BrowserEvent {
    data class InputChanged(val text: String) : BrowserEvent()
    object SubmitSearch : BrowserEvent()
    data class WebStateUpdated(val url: String?, val progress: Int, val canGoBack: Boolean, val canGoForward: Boolean) : BrowserEvent()
    data class LogHistory(val url: String, val title: String) : BrowserEvent()
    object GoHome : BrowserEvent()
}

sealed class BrowserEffect {
    data class ShowToast(val messageResId: Int) : BrowserEffect()
}

@HiltViewModel
class SafeBrowserViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val browserRepository: SafeBrowserRepository
) : BaseViewModel<BrowserState, BrowserEvent, BrowserEffect>(BrowserState()) {

    init {
        viewModelScope.launch {
            val childId = sessionManager.activeChildIdFlow.firstOrNull() ?: return@launch

            // 🚀 Observe the unified profile directly from the DB!
            browserRepository.observeFullProfile(childId).collectLatest { profile ->
                if (profile != null) {
                    updateState {
                        copy(
                            childId = childId,
                            searchEngine = profile.settings.searchEngine,
                            isCartoonWorldEnabled = profile.settings.isCartoonWorldEnabled,
                            filterMode = profile.settings.filterMode,
                            allowedSites = profile.allowedSites.filter { it.isActive },
                            blockedSites = profile.blockedSites.filter { it.isActive },
                            blockedKeywords = profile.blockedKeywords.filter { it.isActive }
                        )
                    }
                }
            }
        }
    }

    override fun onEvent(event: BrowserEvent) {
        when (event) {
            is BrowserEvent.GoHome -> updateState { copy(currentUrl = "", inputText = "") }
            is BrowserEvent.InputChanged -> updateState { copy(inputText = event.text) }

            is BrowserEvent.SubmitSearch -> {
                val input = state.value.inputText
                if (input.isBlank()) return

                val finalUrl = if (looksLikeUrl(input)) ensureScheme(input) else buildSearchUrl(input)

                if (!isUrlAllowed(finalUrl)) {
                    sendEffect(BrowserEffect.ShowToast(R.string.browser_url_not_allowed))
                    return
                }

                updateState { copy(currentUrl = finalUrl, inputText = displayUrl(finalUrl)) }
            }

            is BrowserEvent.WebStateUpdated -> {
                updateState {
                    copy(
                        currentUrl = event.url ?: currentUrl,
                        inputText = if (event.url != null) displayUrl(event.url) else inputText,
                        progress = event.progress / 100f,
                        isLoading = event.progress < 100,
                        canGoBack = event.canGoBack,
                        canGoForward = event.canGoForward
                    )
                }
            }

            is BrowserEvent.LogHistory -> {
                viewModelScope.launch {
                    if (state.value.childId.isNotEmpty() && event.url.startsWith("http")) {
                        browserRepository.logHistory(state.value.childId, event.url, event.title)
                    }
                }
            }
        }
    }

    // 🚀 FULLY UPDATED FILTERING LOGIC
    fun isUrlAllowed(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        if (state.value.filterMode == FilterMode.DISABLED) return true

        val normalized = normalizeUrl(url)

        // 1. ALWAYS block keywords (Overrides everything)
        if (state.value.blockedKeywords.any { normalized.contains(it.keyword.lowercase()) }) return false

        // 2. ALWAYS block blacklisted sites (Overrides everything)
        if (state.value.blockedSites.any { normalized.startsWith(normalizeUrl(it.url)) }) return false

        // 3. ALWAYS allow Safe Search Engine URLs
        if (isSearchPage(normalized)) return true

        // 4. ALWAYS allow Cartoon World if enabled
        if (state.value.isCartoonWorldEnabled && normalized.startsWith("telewebion.ir")) return true

        // 5. Evaluate Whitelist Mode
        if (state.value.filterMode == FilterMode.WHITELIST_ONLY) {
            if (state.value.allowedSites.isEmpty()) {
                return true
            }
            return state.value.allowedSites.any { normalized.startsWith(normalizeUrl(it.url)) }
        }

        // If it's BLACKLIST_ONLY, and we didn't hit a blacklist above, it's allowed.
        return true
    }

    private fun isSearchPage(normalized: String): Boolean {
        return normalized.startsWith("google.com/search") ||
                normalized.startsWith("duckduckgo.com") ||
                normalized.startsWith("shaadbin.ir") ||
                normalized.startsWith("kiddle.co")
    }

    private fun normalizeUrl(url: String) = url.trim().lowercase().replace(Regex("^https?://"), "").replace(Regex("^www\\."), "").removeSuffix("/")
    private fun looksLikeUrl(input: String): Boolean {
        val s = input.trim()
        if (s.contains(" ")) return false
        if (s.startsWith("http://") || s.startsWith("https://")) return true
        return s.contains(".") && !s.endsWith(".")
    }
    private fun ensureScheme(url: String) = if (!url.trim().startsWith("http://") && !url.trim().startsWith("https://")) "https://${url.trim()}" else url.trim()
    private fun displayUrl(url: String) = url.replace(Regex("^https?://"), "").replace(Regex("^www\\."), "")
    private fun buildSearchUrl(query: String): String {
        val q = try { URLEncoder.encode(query, "UTF-8") } catch (_: Exception) { query.replace(" ", "+") }
        return when (state.value.searchEngine) {
            "kiddle" -> "https://www.kiddle.co/s/?q=$q"
            // &kp=1 turns on strict safe search for DuckDuckGo
            "duckduckgo" -> "https://duckduckgo.com/?q=$q&kp=1"
            // &safe=active enforces strict filtering on Google (Web, Images, Video)
            "google" -> "https://www.google.com/search?q=$q&safe=active"
            else -> "https://shaadbin.ir/search?q=$q"
        }
    }
}