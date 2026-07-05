package com.vahak.mehrban.uiv2.components.browser.safebrowser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit
) {
    val colors = LocalCustomColors.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, enabled = canGoBack) {
                Icon(
                    painter = AppIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = if (canGoBack) colors.textPrimary else colors.textHint,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onHome) {
                Icon(
                    painter = AppIcons.Home,
                    contentDescription = "Home",
                    tint = colors.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = onForward, enabled = canGoForward) {
                Icon(
                    painter = AppIcons.ChevronRight,
                    contentDescription = "Forward",
                    tint = if (canGoForward) colors.textPrimary else colors.textHint,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewBrowserBottomBar() {
    ParentControlTheme {
        BrowserBottomBar(
            canGoBack = true,
            canGoForward = false,
            onBack = {},
            onForward = {},
            onHome = {}
        )
    }
}