package com.vahak.mehrban.presentation.browser

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedSiteEntity
import com.vahak.mehrban.core.data.local.entity.FilterMode
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

    val searchEngine: String = "kiddle",
    val isCartoonWorldEnabled: Boolean = true,
    val filterMode: FilterMode = FilterMode.WHITELIST_ONLY,
    val allowedSites: List<BrowserAllowedSiteEntity> = emptyList(),
    val blockedSites: List<BrowserBlockedSiteEntity> = emptyList(),
    val blockedKeywords: List<BrowserBlockedKeywordEntity> = emptyList(),
    val blockedAttemptUrl: String? = null
)

sealed class BrowserEvent {
    data class InputChanged(val text: String) : BrowserEvent()
    object SubmitSearch : BrowserEvent()
    data class WebStateUpdated(
        val url: String?, val progress: Int, val canGoBack: Boolean, val canGoForward: Boolean
    ) : BrowserEvent()

    data class LogHistory(val url: String, val title: String) : BrowserEvent()
    object GoHome : BrowserEvent()
    data class BlockUrlAttempt(val url: String) : BrowserEvent()
    object DismissBlockedOverlay : BrowserEvent()
}

sealed class BrowserEffect {
    data class ShowToast(val messageResId: Int) : BrowserEffect()
}

@HiltViewModel
class SafeBrowserViewModel @Inject constructor(
    private val sessionManager: SessionManager, private val browserRepository: SafeBrowserRepository
) : BaseViewModel<BrowserState, BrowserEvent, BrowserEffect>(BrowserState()) {

    init {
        viewModelScope.launch {
            val childId = sessionManager.activeChildIdFlow.firstOrNull() ?: return@launch

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
                            blockedKeywords = profile.blockedKeywords.filter { it.isActive })
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

                if (containsBlockedKeyword(input, state.value.blockedKeywords)) {
                    updateState { copy(blockedAttemptUrl = input) }
                    return
                }

                val finalUrl = if (looksLikeUrl(input)) ensureScheme(input) else buildSearchUrl(input)

                if (!isUrlAllowed(finalUrl)) {
                    updateState { copy(blockedAttemptUrl = finalUrl) }
                    return
                }

                updateState { copy(currentUrl = finalUrl, inputText = displayUrl(finalUrl)) }
            }

            is BrowserEvent.WebStateUpdated -> {
                if (state.value.currentUrl.isEmpty()) return
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

            is BrowserEvent.BlockUrlAttempt -> updateState { copy(blockedAttemptUrl = event.url) }
            is BrowserEvent.DismissBlockedOverlay -> updateState { copy(blockedAttemptUrl = null) }
        }
    }

    // 🚀 FIXED AND UPGRADED PATH-LEVEL FILTERING LOGIC
    fun isUrlAllowed(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        if (state.value.filterMode == FilterMode.DISABLED) return true

        // 🚀 1. Decode the URL so %D8%AE%D8%B1 turns back into "خر"
        val decodedLowerUrl = try {
            java.net.URLDecoder.decode(url, "UTF-8").lowercase()
        } catch (_: Exception) {
            url.lowercase() // Fallback if decoding fails
        }

        val normalized = normalizeUrl(url)

        // 🚀 2. Use our Smart Regex Helper on the Decoded URL
        if (containsBlockedKeyword(decodedLowerUrl, state.value.blockedKeywords)) {
            return false
        }

        // ... (Keep the rest of your blacklists, cartoon world, and whitelist logic here exactly as before)
        if (state.value.blockedSites.any { normalized.startsWith(normalizeUrl(it.url)) }) return false
        if (isSearchPage(normalized)) return true
        if (state.value.isCartoonWorldEnabled && normalized.startsWith("telewebion.ir")) return true
        if (state.value.filterMode == FilterMode.WHITELIST_ONLY) {
            if (state.value.allowedSites.isEmpty()) return true
            return state.value.allowedSites.any { normalized.startsWith(normalizeUrl(it.url)) }
        }

        return true
    }

    private fun isSearchPage(normalized: String): Boolean {
        return normalized.startsWith("google.com/search") || normalized.startsWith("duckduckgo.com") || normalized.startsWith(
            "shaadbin.ir"
        ) || normalized.startsWith("kiddle.co")
    }

    // 🚀 Updated normalization to strip query params but keep structural routes and subpaths
    private fun normalizeUrl(url: String): String {
        var clean =
            url.trim().lowercase().replace(Regex("^https?://"), "").replace(Regex("^www\\."), "")
                .substringBefore("?") // Keep subpaths, drop query string args

        if (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        return clean
    }

    private fun looksLikeUrl(input: String): Boolean {
        val s = input.trim()
        if (s.contains(" ")) return false
        if (s.startsWith("http://") || s.startsWith("https://")) return true
        return s.contains(".") && !s.endsWith(".")
    }

    private fun ensureScheme(url: String) = if (!url.trim().startsWith("http://") && !url.trim()
            .startsWith("https://")
    ) "https://${url.trim()}" else url.trim()

    private fun displayUrl(url: String) =
        url.replace(Regex("^https?://"), "").replace(Regex("^www\\."), "")

    private fun buildSearchUrl(query: String): String {
        val q = try {
            URLEncoder.encode(query, "UTF-8")
        } catch (_: Exception) {
            query.replace(" ", "+")
        }
        return when (state.value.searchEngine) {
            "kiddle" -> "https://www.kiddle.co/s/?q=$q"
            "duckduckgo" -> "https://duckduckgo.com/?q=$q&kp=1"
            "google" -> "https://www.google.com/search?q=$q&safe=active"
            else -> "https://shaadbin.ir/search?q=$q"
        }
    }

    private fun containsBlockedKeyword(text: String, keywords: List<BrowserBlockedKeywordEntity>): Boolean {
        if (text.isBlank() || keywords.isEmpty()) return false

        val lowerText = text.lowercase()
        return keywords.any { entity ->
            val keyword = entity.keyword.trim().lowercase()
            if (keyword.isBlank()) return@any false

            // We look for the keyword surrounded by either the start/end of the string (^ or $),
            // OR surrounded by something that is NOT a letter or number (like spaces, /, ?, =)
            val regex = Regex("(^|[^\\p{L}\\p{N}])${Regex.escape(keyword)}([^\\p{L}\\p{N}]|$)")

            regex.containsMatchIn(lowerText)
        }
    }
}