package com.vahak.mehrban.presentation.browser

import android.os.Bundle
import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val title: String = "برگه جدید",
    val webViewState: Bundle? = null
)