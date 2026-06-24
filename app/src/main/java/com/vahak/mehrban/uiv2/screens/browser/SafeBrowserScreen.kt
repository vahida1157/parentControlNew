package com.vahak.mehrban.uiv2.screens.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.BrowserWhitelistEntity
import com.vahak.mehrban.core.util.BrowserUsageTracker
import com.vahak.mehrban.presentation.browser.BrowserEffect
import com.vahak.mehrban.presentation.browser.BrowserEvent
import com.vahak.mehrban.presentation.browser.BrowserState
import com.vahak.mehrban.presentation.browser.SafeBrowserViewModel
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

// ----------------------------------------------------------------------------
// STATEFUL COMPONENT
// ----------------------------------------------------------------------------
@Composable
fun SafeBrowserScreen(
    viewModel: SafeBrowserViewModel = hiltViewModel(),
    onCloseClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        BrowserUsageTracker.isBrowserForeground = true
        onDispose {
            BrowserUsageTracker.isBrowserForeground = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserEffect.ShowToast -> {
                    Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    SafeBrowserContent(
        state = state,
        onEvent = viewModel::onEvent,
        onCloseClick = onCloseClick
    )
}

// ----------------------------------------------------------------------------
// STATELESS COMPONENT
// ----------------------------------------------------------------------------
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafeBrowserContent(
    state: BrowserState,
    onEvent: (BrowserEvent) -> Unit,
    onCloseClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // Check if we are running inside Android Studio's @Preview
    val isPreview = LocalInspectionMode.current
    var isSearchBarFocused by remember { mutableStateOf(false) }

    BackHandler {
        if (state.canGoBack && webViewRef != null) {
            webViewRef?.goBack()
        } else {
            onCloseClick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {

        // --- TOP BAR: Address & Close ---
        Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseClick) {
                    Icon(
                        AppIcons.Close,
                        contentDescription = stringResource(R.string.browser_close_desc),
                        tint = colors.textSecondary
                    )
                }

                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { onEvent(BrowserEvent.InputChanged(it)) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .onFocusChanged { isSearchBarFocused = it.isFocused },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    placeholder = {
                        Text(stringResource(R.string.browser_search_hint), color = colors.textHint)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        unfocusedContainerColor = colors.cardInnerBG,
                        focusedContainerColor = colors.surface,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        cursorColor = colors.primary
                    ),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            onEvent(BrowserEvent.SubmitSearch)
                            focusManager.clearFocus()
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // --- ANIMATED SUGGESTIONS ---
            AnimatedVisibility(
                visible = (isSearchBarFocused || isPreview) && state.whitelist.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    // 🚀 THE FIX 2: Apply alignment to the entire row, not the item!
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        Text(
                            text = stringResource(R.string.browser_allowed_sites) + ":",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            // 🚀 Removed .align() from here
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    items(state.whitelist) { site ->
                        SuggestionChip(
                            onClick = {
                                onEvent(BrowserEvent.InputChanged(site.urlPrefix))
                                onEvent(BrowserEvent.SubmitSearch)
                                focusManager.clearFocus()
                            },
                            label = { Text(site.label, color = colors.textPrimary) },
                            icon = { Text("🌐", fontSize = 14.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = colors.cardInnerBG
                            ),
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = colors.divider
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        if (state.isLoading) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = colors.primary,
                trackColor = colors.surface
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        // --- MIDDLE: Web Engine ---
        if (isPreview) {
            // 🚀 THE FIX: Render a dummy box in Android Studio to prevent crashes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(colors.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌐 Web Engine Preview",
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        } else {
            // Render the actual WebView on the device
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                onEvent(BrowserEvent.WebStateUpdated(url, 0, view?.canGoBack() == true, view?.canGoForward() == true))
                            }

                            // 🚀 WE CANNOT USE viewModel.isUrlAllowed HERE DIRECTLY IN STATELESS.
                            // We need to implement the validation inside the view model and just observe state,
                            // OR we accept that URL override checking must happen locally in a helper.
                            // For simplicity, we just pass the raw event back.
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false

                                // Since we moved the logic to ViewModel, we must rely on it.
                                // If you want immediate blocking, it's safer to check the state.whitelist directly.
                                val normalized = url.trim().lowercase().replace(Regex("^https?://"), "").replace(Regex("^www\\."), "")
                                val hasKeyword = state.keywords.any { normalized.contains(it.keyword.lowercase()) }
                                val isSearch = normalized.startsWith("google.com/search") || normalized.startsWith("kiddle.co") || normalized.startsWith("shaadbin.ir")
                                val isAllowed = state.whitelist.any { normalized.startsWith(it.urlPrefix) }

                                if (!state.isProtectionEnabled) return super.shouldOverrideUrlLoading(view, request)

                                if (hasKeyword || (!isSearch && !isAllowed)) {
                                    Toast.makeText(context, context.getString(R.string.browser_url_not_allowed), Toast.LENGTH_SHORT).show()
                                    return true
                                }

                                return super.shouldOverrideUrlLoading(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                onEvent(BrowserEvent.WebStateUpdated(null, newProgress, view?.canGoBack() == true, view?.canGoForward() == true))
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                super.onReceivedTitle(view, title)
                                val url = view?.url
                                if (url != null && title != null) {
                                    onEvent(BrowserEvent.LogHistory(url, title))
                                }
                            }
                        }
                    }.also { webViewRef = it }
                },
                update = { view ->
                    if (view.url != state.currentUrl && state.currentUrl.isNotEmpty()) {
                        view.loadUrl(state.currentUrl)
                    }
                }
            )
        }

        // --- BOTTOM BAR: Navigation & Settings ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(
                    onClick = { webViewRef?.goBack() },
                    enabled = state.canGoBack
                ) {
                    Text("◀", fontSize = 20.sp, color = if (state.canGoBack) colors.textPrimary else colors.textHint)
                }
                IconButton(
                    onClick = { webViewRef?.goForward() },
                    enabled = state.canGoForward
                ) {
                    Text("▶", fontSize = 20.sp, color = if (state.canGoForward) colors.textPrimary else colors.textHint)
                }
                IconButton(
                    onClick = { webViewRef?.reload() },
                    enabled = !state.isLoading
                ) {
                    Icon(AppIcons.Refresh, contentDescription = stringResource(R.string.browser_reload_desc), tint = colors.textPrimary)
                }
            }

            Box {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.primary.copy(alpha = 0.1f))
                        .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .clickable { onEvent(BrowserEvent.SetEngineMenuOpen(true)) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔍", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))

                        val engineName = when (state.searchEngine) {
                            "shaadbin" -> stringResource(R.string.browser_engine_shaadbin)
                            "kiddle" -> stringResource(R.string.browser_engine_kiddle)
                            "duckduckgo" -> stringResource(R.string.browser_engine_duckduckgo)
                            else -> stringResource(R.string.browser_engine_google)
                        }
                        Text(
                            text = engineName,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                DropdownMenu(
                    expanded = state.isEngineMenuOpen,
                    onDismissRequest = { onEvent(BrowserEvent.SetEngineMenuOpen(false)) },
                    modifier = Modifier.background(colors.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browser_engine_shaadbin), color = colors.textPrimary) },
                        onClick = { onEvent(BrowserEvent.ChangeSearchEngine("shaadbin")) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browser_engine_kiddle), color = colors.textPrimary) },
                        onClick = { onEvent(BrowserEvent.ChangeSearchEngine("kiddle")) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browser_engine_duckduckgo), color = colors.textPrimary) },
                        onClick = { onEvent(BrowserEvent.ChangeSearchEngine("duckduckgo")) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.browser_engine_google), color = colors.textPrimary) },
                        onClick = { onEvent(BrowserEvent.ChangeSearchEngine("google")) }
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------

private val mockBrowserState = BrowserState(
    currentUrl = "https://www.kiddle.co",
    inputText = "kiddle.co",
    searchEngine = "kiddle",
    whitelist = listOf(
        BrowserWhitelistEntity(childId = "1", urlPrefix = "aparat.com", label = "آپارات"),
        BrowserWhitelistEntity(childId = "1", urlPrefix = "telewebion.ir", label = "تلوبیون کودک")
    )
)

@Preview(showBackground = true, locale = "fa", name = "1. Browser Light")
@Composable
fun SafeBrowserPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        SafeBrowserContent(
            state = mockBrowserState,
            onEvent = {},
            onCloseClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Browser Dark (Typing)")
@Composable
fun SafeBrowserPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        SafeBrowserContent(
            state = mockBrowserState.copy(inputText = "کارتون پوکویو", isEngineMenuOpen = true),
            onEvent = {},
            onCloseClick = {}
        )
    }
}