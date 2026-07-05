package com.vahak.mehrban.uiv2.components.browser.safebrowser

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.vahak.mehrban.presentation.browser.BrowserEvent

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserEngine(
    currentUrl: String,
    externalAppBlockedText: String,
    isUrlAllowed: (String) -> Boolean,
    onEvent: (BrowserEvent) -> Unit,
    onWebViewCreated: (WebView) -> Unit
) {
    val context = LocalContext.current

    AndroidView(modifier = Modifier.fillMaxSize(), factory = { androidContext ->
        WebView(androidContext).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                cacheMode = WebSettings.LOAD_DEFAULT
                setLayerType(View.LAYER_TYPE_HARDWARE, null)
            }

            webViewClient = object : WebViewClient() {

                override fun doUpdateVisitedHistory(
                    view: WebView?, url: String?, isReload: Boolean
                ) {
                    if (url != null && !isUrlAllowed(url)) {
                        // 🚀 The SPA tried to route to a bad page.
                        view?.stopLoading()
                        view?.goBack() // Silently undo the SPA routing!
                        onEvent(BrowserEvent.BlockUrlAttempt(url))
                        return
                    }
                    super.doUpdateVisitedHistory(view, url, isReload)
                    onEvent(
                        BrowserEvent.WebStateUpdated(
                            url, 0, view?.canGoBack() == true, view?.canGoForward() == true
                        )
                    )
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView?, request: WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return false

                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                        Toast.makeText(context, externalAppBlockedText, Toast.LENGTH_SHORT).show()
                        return true
                    }

                    if (!isUrlAllowed(url)) {
                        // 🚀 Standard link click to a bad page.
                        view?.stopLoading()
                        onEvent(BrowserEvent.BlockUrlAttempt(url))
                        return true // Prevent loading entirely
                    }
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    if (url != null && !isUrlAllowed(url)) {
                        // 🚀 Failsafe
                        view?.stopLoading()
                        onEvent(BrowserEvent.BlockUrlAttempt(url))
                        return
                    }
                    super.onPageStarted(view, url, favicon)
                    onEvent(
                        BrowserEvent.WebStateUpdated(
                            url, 0, view?.canGoBack() == true, view?.canGoForward() == true
                        )
                    )
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
                    if (url != null && title != null && !url.startsWith("blocked://")) {
                        onEvent(BrowserEvent.LogHistory(url, title))
                    }
                }
            }
        }.also { onWebViewCreated(it) }
    }, update = { view ->
        // Prevent infinite reload loops when syncing state
        if (view.url != currentUrl && currentUrl.isNotEmpty() && !currentUrl.startsWith("blocked://")) {
            view.loadUrl(currentUrl)
        }
    }, onRelease = { view ->
        view.stopLoading()
        view.webChromeClient = WebChromeClient()
        view.webViewClient = WebViewClient()
        view.clearHistory()
        view.destroy()
    })
}