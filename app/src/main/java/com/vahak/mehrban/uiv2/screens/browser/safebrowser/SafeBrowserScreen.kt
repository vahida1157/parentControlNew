package com.vahak.mehrban.uiv2.screens.browser.safebrowser

import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.core.util.BrowserUsageTracker
import com.vahak.mehrban.presentation.browser.BrowserEffect
import com.vahak.mehrban.presentation.browser.BrowserEvent
import com.vahak.mehrban.presentation.browser.BrowserState
import com.vahak.mehrban.presentation.browser.SafeBrowserViewModel
import com.vahak.mehrban.uiv2.components.browser.safebrowser.BrowserBlockedOverlay
import com.vahak.mehrban.uiv2.components.browser.safebrowser.BrowserBottomBar
import com.vahak.mehrban.uiv2.components.browser.safebrowser.BrowserEngine
import com.vahak.mehrban.uiv2.components.browser.safebrowser.BrowserHomeContent
import com.vahak.mehrban.uiv2.components.browser.safebrowser.BrowserSearchBar
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

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
        BrowserUsageTracker.forceCloseFlow.collect { onCloseClick() }
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

@Composable
fun SafeBrowserContent(
    state: BrowserState,
    onEvent: (BrowserEvent) -> Unit,
    onCloseClick: () -> Unit,
    isUrlAllowed: (String) -> Boolean
) {
    val colors = LocalCustomColors.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val isPreview = LocalInspectionMode.current

    // 🚀 FIX 1: Pause WebView media/JavaScript when the overlay is visible
    LaunchedEffect(state.blockedAttemptUrl) {
        if (state.blockedAttemptUrl != null) {
            webViewRef?.onPause()
        } else {
            webViewRef?.onResume()
        }
    }

    // Hardware Back Button
    BackHandler {
        if (state.blockedAttemptUrl != null) {
            onEvent(BrowserEvent.DismissBlockedOverlay)
        } else if (state.currentUrl.isNotEmpty() && webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else if (state.currentUrl.isNotEmpty()) {
            webViewRef?.stopLoading()
            onEvent(BrowserEvent.GoHome)
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

        BrowserSearchBar(
            inputText = state.inputText,
            currentUrl = state.currentUrl,
            isLoading = state.isLoading,
            onEvent = onEvent,
            onReload = { webViewRef?.reload() },
            onStop = { webViewRef?.stopLoading() })

        if (state.isLoading && state.currentUrl.isNotEmpty()) {
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = colors.primary,
                trackColor = colors.surface
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.currentUrl.isEmpty()) {
                BrowserHomeContent(
                    isCartoonWorldEnabled = state.isCartoonWorldEnabled,
                    allowedSites = state.allowedSites,
                    onEvent = onEvent
                )
            } else if (!isPreview) {
                BrowserEngine(
                    currentUrl = state.currentUrl,
                    externalAppBlockedText = stringResource(R.string.browser_external_app_blocked),
                    isUrlAllowed = isUrlAllowed,
                    onEvent = onEvent,
                    onWebViewCreated = { webViewRef = it })
            }

            // 🚀 THE NATIVE BLOCKED OVERLAY
            if (state.blockedAttemptUrl != null) {
                BrowserBlockedOverlay(
                    blockedUrl = state.blockedAttemptUrl,
                    onGoBackClick = { onEvent(BrowserEvent.DismissBlockedOverlay) })
            }
        }

        BrowserBottomBar(
            canGoBack = state.canGoBack || state.blockedAttemptUrl != null,
            canGoForward = state.canGoForward && state.blockedAttemptUrl == null,
            onBack = {
                if (state.blockedAttemptUrl != null) {
                    onEvent(BrowserEvent.DismissBlockedOverlay)
                } else if (webViewRef?.canGoBack() == true) {
                    webViewRef?.goBack()
                }
            },
            onForward = { webViewRef?.goForward() },
            onHome = {
                webViewRef?.stopLoading()
                onEvent(BrowserEvent.GoHome)
            })
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewSafeBrowserContentHome() {
    ParentControlTheme {
        SafeBrowserContent(
            state = BrowserState(
            currentUrl = "", allowedSites = listOf(
                BrowserAllowedSiteEntity(
                    childId = "1", url = "aparat.com", label = "آپارات"
                )
            )
        ), onEvent = {}, onCloseClick = {}, isUrlAllowed = { true })
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewSafeBrowserContentWeb() {
    ParentControlTheme {
        SafeBrowserContent(
            state = BrowserState(
            currentUrl = "https://aparat.com",
            inputText = "aparat.com",
            isLoading = true,
            progress = 0.4f,
            canGoBack = true
        ), onEvent = {}, onCloseClick = {}, isUrlAllowed = { true })
    }
}