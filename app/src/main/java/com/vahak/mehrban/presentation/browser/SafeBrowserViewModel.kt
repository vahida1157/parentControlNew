package com.vahak.mehrban.presentation.browser

import androidx.lifecycle.viewModelScope
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.entity.BrowserKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserWhitelistEntity
import com.vahak.mehrban.domain.repository.SafeBrowserRepository
import com.vahak.mehrban.presentation.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.net.URLEncoder
import javax.inject.Inject

data class BrowserState(
    val childId: String = "",
    val isProtectionEnabled: Boolean = true,
    val currentUrl: String = "",
    val inputText: String = "",
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val searchEngine: String = "kiddle",
    val isEngineMenuOpen: Boolean = false,
    val whitelist: List<BrowserWhitelistEntity> = emptyList(),
    val keywords: List<BrowserKeywordEntity> = emptyList()
)

sealed class BrowserEvent {
    data class InputChanged(val text: String) : BrowserEvent()
    object SubmitSearch : BrowserEvent()
    data class WebStateUpdated(val url: String?, val progress: Int, val canGoBack: Boolean, val canGoForward: Boolean) : BrowserEvent()
    data class SetEngineMenuOpen(val isOpen: Boolean) : BrowserEvent()
    data class ChangeSearchEngine(val engineId: String) : BrowserEvent()
    data class LogHistory(val url: String, val title: String) : BrowserEvent() // 🚀 History Event
}

sealed class BrowserEffect {
    data class ShowToast(val messageResId: Int) : BrowserEffect() // 🚀 Fixed to use String Resource ID
}

@HiltViewModel
class SafeBrowserViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val browserRepository: SafeBrowserRepository
) : BaseViewModel<BrowserState, BrowserEvent, BrowserEffect>(BrowserState()) {

    init {
        viewModelScope.launch {
            val language = sessionManager.appLanguageFlow.first()
            val savedEngine = sessionManager.searchEngineFlow.first()
            val activeEngine = savedEngine ?: if (language == "fa") "shaadbin" else "kiddle"

            val childId = sessionManager.activeChildIdFlow.firstOrNull() ?: return@launch

            updateState {
                copy(
                    childId = childId,
                    searchEngine = activeEngine,
                    currentUrl = getEngineHomeUrl(activeEngine),
                    inputText = displayUrl(getEngineHomeUrl(activeEngine))
                )
            }

            launch { browserRepository.observeWhitelist(childId).collectLatest { updateState { copy(whitelist = it) } } }
            launch { browserRepository.observeKeywords(childId).collectLatest { updateState { copy(keywords = it) } } }
        }
    }

    override fun onEvent(event: BrowserEvent) {
        when (event) {
            is BrowserEvent.InputChanged -> updateState { copy(inputText = event.text) }

            is BrowserEvent.SubmitSearch -> {
                val input = state.value.inputText
                if (input.isBlank()) return

                val finalUrl = if (looksLikeUrl(input)) ensureScheme(input) else buildSearchUrl(input)

                if (!isUrlAllowed(finalUrl)) {
                    sendEffect(BrowserEffect.ShowToast(R.string.browser_url_not_allowed)) // 🚀 Uses string resource!
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

            is BrowserEvent.SetEngineMenuOpen -> updateState { copy(isEngineMenuOpen = event.isOpen) }

            is BrowserEvent.ChangeSearchEngine -> {
                val newHomeUrl = getEngineHomeUrl(event.engineId)
                updateState {
                    copy(
                        searchEngine = event.engineId,
                        isEngineMenuOpen = false,
                        currentUrl = newHomeUrl,
                        inputText = displayUrl(newHomeUrl)
                    )
                }
                viewModelScope.launch { sessionManager.setSearchEngine(event.engineId) }
            }

            is BrowserEvent.LogHistory -> {
                viewModelScope.launch {
                    // Only log if we have a valid URL (ignore blank pages or internal data URIs)
                    if (state.value.childId.isNotEmpty() && event.url.startsWith("http")) {
                        browserRepository.logHistory(state.value.childId, event.url, event.title)
                    }
                }
            }
        }
    }

    fun isUrlAllowed(url: String?): Boolean {
        if (url == null || url.isEmpty()) return false
        if (!state.value.isProtectionEnabled) return true

        val normalized = normalizeUrl(url)
        val hasKeyword = state.value.keywords.any { normalized.contains(it.keyword.lowercase()) }
        if (hasKeyword) return false
        if (isSearchPage(normalized)) return true
        return state.value.whitelist.any { normalized.startsWith(normalizeUrl(it.urlPrefix)) }
    }

    private fun getEngineHomeUrl(engine: String) = when (engine) {
        "shaadbin" -> "https://shaadbin.ir"
        "duckduckgo" -> "https://duckduckgo.com"
        "google" -> "https://www.google.com"
        else -> "https://www.kiddle.co"
    }

    private fun isSearchPage(normalized: String): Boolean {
        return normalized.startsWith("google.com/search") ||
                normalized.startsWith("bing.com/search") ||
                normalized.startsWith("duckduckgo.com") ||
                normalized.startsWith("shaadbin.ir") ||
                normalized.startsWith("kiddle.co")
    }

    private fun normalizeUrl(url: String): String {
        return url.trim().lowercase().replace(Regex("^https?://"), "").replace(Regex("^www\\."), "").removeSuffix("/")
    }

    private fun looksLikeUrl(input: String): Boolean {
        val s = input.trim()
        if (s.contains(" ")) return false
        if (s.startsWith("http://") || s.startsWith("https://")) return true
        return s.contains(".") && !s.endsWith(".")
    }

    private fun ensureScheme(url: String): String {
        val s = url.trim()
        return if (!s.startsWith("http://") && !s.startsWith("https://")) "https://$s" else s
    }

    private fun displayUrl(url: String): String {
        return url.replace(Regex("^https?://"), "").replace(Regex("^www\\."), "")
    }

    private fun buildSearchUrl(query: String): String {
        val q = try { URLEncoder.encode(query, "UTF-8") } catch (e: Exception) { query.replace(" ", "+") }
        return when (state.value.searchEngine) {
            "shaadbin" -> "https://shaadbin.ir/search?q=$q"
            "duckduckgo" -> "https://duckduckgo.com/?q=$q&kp=1"
            "google" -> "https://www.google.com/search?q=$q&safe=active"
            else -> "https://www.kiddle.co/s/?q=$q"
        }
    }
}