package com.vahak.parentcontroll.presentation.appselection

import androidx.compose.ui.graphics.ImageBitmap

// A simple UI model for our apps
data class AppItemUi(
    val packageName: String,
    val appName: String,
    val isAllowed: Boolean,
    val iconBitmap: ImageBitmap? = null
)