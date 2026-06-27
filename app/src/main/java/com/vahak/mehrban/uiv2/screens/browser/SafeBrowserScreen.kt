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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.core.data.local.entity.FilterMode
import com.vahak.mehrban.core.util.BrowserUsageTracker
import com.vahak.mehrban.presentation.browser.BrowserEffect
import com.vahak.mehrban.presentation.browser.BrowserEvent
import com.vahak.mehrban.presentation.browser.BrowserState
import com.vahak.mehrban.presentation.browser.SafeBrowserViewModel
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun SafeBrowserScreen(
    viewModel: SafeBrowserViewModel = hiltViewModel(),
    onCloseClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        BrowserUsageTracker.isBrowserForeground = true
        onDispose { BrowserUsageTracker.isBrowserForeground = false }
    }

    // 🚀 THE FIX: Listen for the emergency close signal from Enforcer Service
    LaunchedEffect(Unit) {
        BrowserUsageTracker.forceCloseFlow.collect {
            onCloseClick() // Instantly kicks the child out of the Compose browser screen!
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserEffect.ShowToast -> Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
            }
        }
    }

    SafeBrowserContent(
        state = state,
        onEvent = viewModel::onEvent,
        onCloseClick = onCloseClick,
        isUrlAllowed = viewModel::isUrlAllowed // 🚀 Pass the validation function down
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
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val isPreview = LocalInspectionMode.current

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

        // --- TOP BAR: Address & Close ---
        Column(modifier = Modifier.fillMaxWidth().background(colors.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onCloseClick) { Icon(AppIcons.Close, contentDescription = "Close", tint = colors.textSecondary) }

                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = { onEvent(BrowserEvent.InputChanged(it)) },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.browser_search_hint), color = colors.textHint) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                        unfocusedContainerColor = colors.cardInnerBG, focusedContainerColor = colors.surface,
                        focusedBorderColor = colors.primary, unfocusedBorderColor = colors.divider, cursorColor = colors.primary
                    ),
                    textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { onEvent(BrowserEvent.SubmitSearch); focusManager.clearFocus() })
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }

        if (state.isLoading && state.currentUrl.isNotEmpty()) {
            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(3.dp), color = colors.primary, trackColor = colors.surface)
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        // --- MIDDLE: Home Screen OR Web Engine ---
        if (state.currentUrl.isEmpty()) {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp))

                // 🚀 CARTOON WORLD BUTTON
                if (state.isCartoonWorldEnabled) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(colors.orange, colors.red)))
                            .clickable { onEvent(BrowserEvent.InputChanged("telewebion.ir/kids")); onEvent(BrowserEvent.SubmitSearch) }.padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎬", fontSize = 48.sp)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("دنیای کارتون", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                Text("تماشای بهترین انیمیشن‌ها", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.divider))
                    Text(text = stringResource(R.string.browser_allowed_sites), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 12.dp))
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.divider))
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (state.allowedSites.isEmpty()) {
                    Text("سایتی ثبت نشده است", color = colors.textHint, modifier = Modifier.padding(top = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(state.allowedSites) { site ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onEvent(BrowserEvent.InputChanged(site.url)); onEvent(BrowserEvent.SubmitSearch) },
                                shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = colors.surface)
                            ) {
                                Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(40.dp).background(colors.cardInnerBG, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Text("🌍", fontSize = 20.sp) }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(site.label, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                        Text(site.url, fontSize = 12.sp, color = colors.textSecondary)
                                    }
                                    Icon(AppIcons.ChevronLeft, contentDescription = null, tint = colors.textHint)
                                }
                            }
                        }
                    }
                }
            }
        } else if (isPreview) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("🌐 Web Engine Preview", color = colors.textSecondary) }
        } else {
            val urlNotAllowedText = stringResource(R.string.browser_url_not_allowed)
            AndroidView(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                factory = { androidContext ->
                    WebView(androidContext).apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        settings.apply { javaScriptEnabled = true; domStorageEnabled = true; loadWithOverviewMode = true; useWideViewPort = true; setSupportZoom(true); builtInZoomControls = true; displayZoomControls = false }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                onEvent(BrowserEvent.WebStateUpdated(url, 0, view?.canGoBack() == true, view?.canGoForward() == true))
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false

                                // 🚀 CRITICAL FIX: Block external intents (market://, intent://, tel://) so the app doesn't crash
                                // and the child cannot escape to Google Play or other apps.
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    Toast.makeText(context, "باز کردن برنامه‌های خارجی مسدود است", Toast.LENGTH_SHORT).show()
                                    return true // Block navigation
                                }

                                // 🚀 Apply validation through the passed lambda!
                                if (!isUrlAllowed(url)) {
                                    Toast.makeText(context, urlNotAllowedText, Toast.LENGTH_SHORT).show()
                                    return true // Block navigation
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
                                if (url != null && title != null) { onEvent(BrowserEvent.LogHistory(url, title)) }
                            }
                        }
                    }.also { webViewRef = it }
                },
                update = { view -> if (view.url != state.currentUrl && state.currentUrl.isNotEmpty()) { view.loadUrl(state.currentUrl) } }
            )
        }

        // --- BOTTOM BAR: Navigation ---
        // 🚀 Engine Selector removed entirely, controlled by parent now.
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surface).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { webViewRef?.goBack() }, enabled = state.canGoBack) { Text("◀", fontSize = 20.sp, color = if (state.canGoBack) colors.textPrimary else colors.textHint) }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { webViewRef?.goForward() }, enabled = state.canGoForward) { Text("▶", fontSize = 20.sp, color = if (state.canGoForward) colors.textPrimary else colors.textHint) }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { webViewRef?.reload() }, enabled = !state.isLoading && state.currentUrl.isNotEmpty()) { Icon(AppIcons.Refresh, contentDescription = "Reload", tint = if (state.currentUrl.isNotEmpty()) colors.textPrimary else colors.textHint) }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onEvent(BrowserEvent.GoHome) }) { Icon(AppIcons.Home, contentDescription = "Home", tint = colors.textPrimary) }
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------

private val mockBrowserState = BrowserState(
    currentUrl = "",
    inputText = "",
    searchEngine = "kiddle",
    isCartoonWorldEnabled = true,
    filterMode = FilterMode.WHITELIST_ONLY,
    allowedSites = listOf(
        BrowserAllowedSiteEntity(childId = "1", url = "aparat.com", label = "آپارات"),
        BrowserAllowedSiteEntity(childId = "1", url = "telewebion.ir", label = "تلوبیون کودک")
    )
)

@Preview(showBackground = true, locale = "fa", name = "1. Browser Home (Light)")
@Composable
fun SafeBrowserPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        SafeBrowserContent(state = mockBrowserState, onEvent = {}, onCloseClick = {}, isUrlAllowed = { true })
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Browser Home (Dark)")
@Composable
fun SafeBrowserPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        SafeBrowserContent(state = mockBrowserState, onEvent = {}, onCloseClick = {}, isUrlAllowed = { true })
    }
}