package com.vahak.mehrban.uiv2.screens.browser.safebrowser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.util.BrowserUsageTracker
import com.vahak.mehrban.presentation.browser.BrowserEffect
import com.vahak.mehrban.presentation.browser.BrowserEvent
import com.vahak.mehrban.presentation.browser.BrowserState
import com.vahak.mehrban.presentation.browser.SafeBrowserViewModel
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SafeBrowserScreen(
    viewModel: SafeBrowserViewModel = hiltViewModel(), onCloseClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        BrowserUsageTracker.isBrowserForeground = true
        onDispose { BrowserUsageTracker.isBrowserForeground = false }
    }

    LaunchedEffect(Unit) {
        BrowserUsageTracker.forceCloseFlow.collect {
            onCloseClick()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserEffect.ShowToast -> Toast.makeText(
                    context, effect.messageResId, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    SafeBrowserContent(
        state = state,
        onEvent = viewModel::onEvent,
        onCloseClick = onCloseClick,
        isUrlAllowed = viewModel::isUrlAllowed
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SafeBrowserContent(
    state: BrowserState,
    onEvent: (BrowserEvent) -> Unit,
    onCloseClick: () -> Unit,
    isUrlAllowed: (String) -> Boolean
) {
    val colors = LocalCustomColors.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current // Needed to pass RTL/LTR to HTML

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val isPreview = LocalInspectionMode.current
    var textFieldValue by remember { mutableStateOf(TextFieldValue(state.inputText)) }
    var isUrlFocused by remember { mutableStateOf(false) }
    // Synchronize programmatic state modifications from view model
    LaunchedEffect(isUrlFocused) {
        if (isUrlFocused && textFieldValue.text.isNotEmpty()) {
            delay(50.milliseconds)
            textFieldValue = textFieldValue.copy(
                selection = TextRange(0, textFieldValue.text.length)
            )
        }
    }

    LaunchedEffect(state.inputText) {
        if (!isUrlFocused && state.inputText != textFieldValue.text) {
            textFieldValue = TextFieldValue(text = state.inputText)
        }
    }

    BackHandler {
        if (state.currentUrl.isNotEmpty() && webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else if (state.currentUrl.isNotEmpty()) {
            onEvent(BrowserEvent.GoHome)
        } else {
            onCloseClick()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(colors.background).systemBarsPadding()) {

        // --- TOP BAR: Integrated Search Bar ---
        Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = {
                        textFieldValue = it
                        onEvent(BrowserEvent.InputChanged(it.text))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .onFocusChanged { focusState ->
                            isUrlFocused = focusState.isFocused
                        },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            stringResource(R.string.browser_search_hint),
                            color = colors.textHint
                        )
                    },

                    // 🚀 THE BROWSER LOGIC FIX: Clear vs Stop vs Refresh
                    trailingIcon = {
                        if (isUrlFocused) {
                            // State 1: User is editing -> Show CLEAR button
                            if (textFieldValue.text.isNotEmpty()) {
                                IconButton(onClick = {
                                    textFieldValue = TextFieldValue("")
                                    onEvent(BrowserEvent.InputChanged(""))
                                }) {
                                    Icon(
                                        AppIcons.Close,
                                        contentDescription = "Clear",
                                        tint = colors.textHint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        } else if (state.isLoading) {
                            // State 2: Page is actively loading -> Show STOP button
                            IconButton(onClick = { webViewRef?.stopLoading() }) {
                                Icon(
                                    AppIcons.Close,
                                    contentDescription = "Stop",
                                    tint = colors.red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } else if (state.currentUrl.isNotEmpty()) {
                            // State 3: Idle and viewing a page -> Show REFRESH button
                            IconButton(onClick = { webViewRef?.reload() }) {
                                Icon(
                                    AppIcons.Refresh,
                                    contentDescription = "Reload",
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
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
                    keyboardActions = KeyboardActions(onGo = {
                        onEvent(BrowserEvent.SubmitSearch)
                        focusManager.clearFocus()
                    })
                )
            }
        }

        if (state.isLoading && state.currentUrl.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = colors.primary,
                trackColor = colors.surface
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        // --- MIDDLE: Home Screen OR WebView Viewport ---
        if (state.currentUrl.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                if (state.isCartoonWorldEnabled) {
                    // 🚀 REMOVED HARDCODED PERSIAN
                    Box(
                        modifier = Modifier
                            // ... (keep modifiers)
                            .clickable {
                                onEvent(BrowserEvent.InputChanged("telewebion.ir/kids"))
                                onEvent(BrowserEvent.SubmitSearch)
                            }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎬", fontSize = 48.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    stringResource(R.string.browser_cartoon_world),
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    stringResource(R.string.browser_cartoon_world_desc),
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.divider))
                    Text(
                        text = stringResource(R.string.browser_allowed_sites),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.divider))
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (state.allowedSites.isEmpty()) {
                    // 🚀 REMOVED HARDCODED PERSIAN
                    Text(
                        stringResource(R.string.browser_empty_list_hint),
                        color = colors.textHint,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.allowedSites) { site ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onEvent(BrowserEvent.InputChanged(site.url)); onEvent(
                                    BrowserEvent.SubmitSearch
                                )
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = colors.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(40.dp).background(
                                            colors.cardInnerBG, RoundedCornerShape(12.dp)
                                        ), contentAlignment = Alignment.Center
                                    ) { Text("🌍", fontSize = 20.sp) }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            site.label,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            site.url, fontSize = 12.sp, color = colors.textSecondary
                                        )
                                    }
                                    Icon(
                                        AppIcons.ChevronLeft,
                                        contentDescription = null,
                                        tint = colors.textHint
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (isPreview) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) { Text("🌐 Web Engine Preview", color = colors.textSecondary) }
        } else {
            val urlNotAllowedText = stringResource(R.string.browser_url_not_allowed)
            val externalAppBlockedText = stringResource(R.string.browser_external_app_blocked)
            val blockedPageTitle = stringResource(R.string.browser_access_blocked)

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

                            // 🚀 NEW: Aggressive Caching
                            cacheMode = WebSettings.LOAD_DEFAULT
                            // LOAD_DEFAULT uses cache when available and network when expired.
                            // If you want it to load even faster (but risk stale content), use LOAD_CACHE_ELSE_NETWORK.

                            // 🚀 NEW: Hardware Acceleration & Performance
                            setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?, url: String?, favicon: Bitmap?
                            ) {
                                super.onPageStarted(view, url, favicon)
                                onEvent(
                                    BrowserEvent.WebStateUpdated(
                                        url,
                                        0,
                                        view?.canGoBack() == true,
                                        view?.canGoForward() == true
                                    )
                                )
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: return false

                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    Toast.makeText(
                                        context,
                                        externalAppBlockedText,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return true
                                }

                                if (!isUrlAllowed(url)) {
                                    // 🚀 DYNAMIC HTML ASSET LOADING & LOCALIZATION
                                    val lang =
                                        if (layoutDirection == LayoutDirection.Rtl) "fa" else "en"
                                    val dir =
                                        if (layoutDirection == LayoutDirection.Rtl) "rtl" else "ltr"

                                    val htmlContent = try {
                                        context.assets.open("blocked_page.html").bufferedReader()
                                            .use { it.readText() }
                                    } catch (e: Exception) {
                                        "<html><body style='text-align:center; padding:50px;'><h1>🛑 $blockedPageTitle</h1></body></html>"
                                    }

                                    // Replace the placeholders in the HTML with our localized strings
                                    // Replace the placeholders in the HTML with our localized strings
                                    val localizedHtml = htmlContent
                                        .replace("__TITLE__", blockedPageTitle)
                                        .replace("__LANG__", lang)
                                        .replace("__DIR__", dir)

                                    // 🚀 THE FIX: Use the blocked URL for both Base and History.
                                    // Bypasses the file:// security crash AND fixes the empty back-stack!
                                    view?.loadDataWithBaseURL(
                                        url,            // baseUrl
                                        localizedHtml,  // data
                                        "text/html",    // mimeType
                                        "UTF-8",        // encoding
                                        url             // historyUrl
                                    )
                                    onEvent(BrowserEvent.InputChanged(url))
                                    return true
                                }
                                return super.shouldOverrideUrlLoading(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                onEvent(
                                    BrowserEvent.WebStateUpdated(
                                        null,
                                        newProgress,
                                        view?.canGoBack() == true,
                                        view?.canGoForward() == true
                                    )
                                )
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
                }, update = { view ->
                    if (view.url != state.currentUrl && state.currentUrl.isNotEmpty()) {
                        view.loadUrl(state.currentUrl)
                    }
                })
        }

        // --- BOTTOM BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            // SpaceBetween pushes items to the edges and center beautifully
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRtl = layoutDirection == LayoutDirection.Rtl

            // ◀ BACK BUTTON
            IconButton(onClick = { webViewRef?.goBack() }, enabled = state.canGoBack) {
                Icon(
                    painter = AppIcons.ChevronLeft, // Naturally points Left
                    contentDescription = "Back",
                    tint = if (state.canGoBack) colors.textPrimary else colors.textHint,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            // In Persian (RTL), "Back" means pointing Right. So we flip it.
                            scaleX = if (isRtl) -1f else 1f
                        }
                )
            }

            // 🏠 HOME BUTTON (Moved to center)
            IconButton(onClick = { onEvent(BrowserEvent.GoHome) }) {
                Icon(
                    painter = AppIcons.Home,
                    contentDescription = "Home",
                    tint = colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            // ▶ FORWARD BUTTON
            IconButton(onClick = { webViewRef?.goForward() }, enabled = state.canGoForward) {
                Icon(
                    painter = AppIcons.ChevronLeft,
                    contentDescription = "Forward",
                    tint = if (state.canGoForward) colors.textPrimary else colors.textHint,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            // Forward is the exact opposite of Back
                            scaleX = if (isRtl) 1f else -1f
                        }
                )
            }
        }
    }
}