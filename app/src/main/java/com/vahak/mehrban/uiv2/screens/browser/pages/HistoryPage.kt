package com.vahak.mehrban.uiv2.screens.browser.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsEvent
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsState
import com.vahak.mehrban.uiv2.screens.browser.MockBrowserData
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HistoryPage(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

    val todayText = stringResource(R.string.today)
    val yesterdayText = stringResource(R.string.yesterday)

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // 🚀 Date Logic to determine "Today", "Yesterday", or formatted date
    val displayDate = remember(state.selectedDateMillis) {
        val selected = Calendar.getInstance().apply { timeInMillis = state.selectedDateMillis }
        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val fmt = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())

        when (fmt.format(selected.time)) {
            fmt.format(today.time) -> todayText
            fmt.format(yesterday.time) -> yesterdayText
            else -> fmt.format(selected.time)
        }
    }

    // 🚀 Disable "Next Day" if we are already viewing today
    val isToday = remember(state.selectedDateMillis) {
        val selected = Calendar.getInstance().apply { timeInMillis = state.selectedDateMillis }
        val today = Calendar.getInstance()
        selected.get(Calendar.YEAR) == today.get(Calendar.YEAR) && selected.get(Calendar.DAY_OF_YEAR) == today.get(
            Calendar.DAY_OF_YEAR
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 🚀 DATE NAVIGATION BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .border(width = 1.dp, color = colors.divider)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Day (Right arrow in RTL)
            IconButton(onClick = { onEvent(BrowserSettingsEvent.ChangeHistoryDate(-1)) }) {
                Text("▶", color = colors.primary, fontSize = 18.sp) // Standard Unicode arrow
            }

            Text(
                text = displayDate,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 16.sp
            )

            // Next Day (Left arrow in RTL)
            IconButton(
                onClick = { onEvent(BrowserSettingsEvent.ChangeHistoryDate(1)) }, enabled = !isToday
            ) {
                Text(
                    "◀", color = if (isToday) colors.textHint else colors.primary, fontSize = 18.sp
                )
            }
        }

        // --- 🚀 FLAT LAZY LIST (One Day Only) ---
        if (state.history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.browser_no_history), color = colors.textHint)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(state.history) { log ->
                    val date = Date(log.timestamp)
                    val browserLinkErrorText = stringResource(R.string.browser_link_error)

                    Row(modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // 🚀 Parent clicks to visit the URL
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, log.url.toUri())
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context, browserLinkErrorText, Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        // Time Column
                        Text(
                            text = timeFormatter.format(date),
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.width(48.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        // Title and Truncated URL
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = log.title.ifBlank { log.url },
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.url,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                            )
                        }

                        // Copy Button
                        val linkCopiedText = stringResource(R.string.browser_link_copied)
                        IconButton(
                            onClick = {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("URL", log.url)
                                )
                                Toast.makeText(context, linkCopiedText, Toast.LENGTH_SHORT).show()
                            }) {
                            Icon(
                                AppIcons.Copy,
                                contentDescription = "Copy URL",
                                tint = colors.textHint,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(
                        color = colors.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 72.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "History Page")
@Composable
fun PreviewHistoryPage() {
    ParentControlTheme {
        HistoryPage(
            state = BrowserSettingsState(
                history = MockBrowserData.mockHistory,
                selectedDateMillis = System.currentTimeMillis()
            ), onEvent = {})
    }
}