// SettingsMenuPage.kt
package com.vahak.mehrban.uiv2.screens.browser.pages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.FilterMode
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsEvent
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsPage
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsState
import com.vahak.mehrban.uiv2.components.browser.SettingMenuRow
import com.vahak.mehrban.uiv2.screens.browser.MockBrowserData
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun SettingsMenuPage(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    val colors = LocalCustomColors.current
    val settings = state.draftSettings

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 🚀 ACTIVITY SECTION ---
        Text(
            stringResource(R.string.browser_history),
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        SettingMenuRow(
            emoji = "🕒",
            title = stringResource(R.string.browser_history),
            desc = stringResource(R.string.browser_view_activity),
            onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.HISTORY)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 🚀 GENERAL SETTINGS ---
        Text(
            stringResource(R.string.browser_general_settings),
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .clickable { onEvent(BrowserSettingsEvent.SetEngineMenuOpen(true)) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🔍") }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.browser_search_engine),
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    val engineName = when (settings?.searchEngine) {
                        "kiddle" -> stringResource(R.string.browser_engine_kiddle)
                        "duckduckgo" -> stringResource(R.string.browser_engine_duckduckgo)
                        "google" -> stringResource(R.string.browser_engine_google)
                        else -> stringResource(R.string.browser_engine_shaadbin)
                    }
                    Text(engineName, fontSize = 12.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                }
                Icon(AppIcons.ChevronLeft, contentDescription = null, tint = colors.textHint)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.orange.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("🎬") }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.browser_cartoon_world), fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(stringResource(R.string.browser_cartoon_world_desc), fontSize = 12.sp, color = colors.textSecondary)
                }
                Switch(
                    checked = settings?.isCartoonWorldEnabled ?: true,
                    onCheckedChange = { onEvent(BrowserSettingsEvent.ToggleCartoonWorld(it)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 🚀 EMPTY WHITELIST WARNING (Moved right above Access Management) ---
        if (settings?.filterMode == FilterMode.WHITELIST_ONLY && state.draftAllowedSites.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.orange.copy(alpha = 0.1f)),
                border = BorderStroke(1.dp, colors.orange)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(AppIcons.Warning, contentDescription = null, tint = colors.orange)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.browser_empty_whitelist_warning),
                        color = colors.orange,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // --- 🚀 ACCESS MANAGEMENT (Dynamic Layout) ---
        Text(
            stringResource(R.string.browser_access_management),
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Single Row for Mode Selector and Conditionally Rendered Access List
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left Half: Filter Mode Selector
            CompactMenuCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.browser_filter_mode),
                subtitle = when (settings?.filterMode) {
                    FilterMode.WHITELIST_ONLY -> stringResource(R.string.browser_whitelist)
                    FilterMode.BLACKLIST_ONLY -> stringResource(R.string.browser_blacklist)
                    else -> stringResource(R.string.browser_filter_disabled)
                },
                iconEmoji = "🛡️",
                iconBgColor = colors.green.copy(alpha = 0.1f),
                subtitleColor = colors.green,
                onClick = { onEvent(BrowserSettingsEvent.SetFilterMenuOpen(true)) }
            )

            // Right Half: Dependent Access List
            if (settings?.filterMode == FilterMode.WHITELIST_ONLY) {
                CompactMenuCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.browser_allowed_sites),
                    subtitle = stringResource(R.string.browser_sites_count, state.draftAllowedSites.size),
                    iconEmoji = "✅",
                    iconBgColor = colors.primary.copy(alpha = 0.1f),
                    subtitleColor = colors.primary,
                    onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.ALLOWED_SITES)) }
                )
            } else if (settings?.filterMode == FilterMode.BLACKLIST_ONLY) {
                CompactMenuCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.browser_blocked_sites),
                    subtitle = stringResource(R.string.browser_sites_count, state.draftBlockedSites.size),
                    iconEmoji = "⛔",
                    iconBgColor = colors.red.copy(alpha = 0.1f),
                    subtitleColor = colors.red,
                    onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.BLOCKED_SITES)) }
                )
            } else {
                // If Disabled, keep layout balanced with empty spacer
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Keywords Row: Always visible regardless of the selected filter mode
        SettingMenuRow(
            emoji = "🚫",
            title = stringResource(R.string.browser_blocked_keywords),
            desc = stringResource(R.string.browser_keywords_count, state.draftKeywords.size),
            onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.KEYWORDS)) }
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    // --- 🚀 SIMPLE THEMED DIALOGS ---

    // Filter Mode Selection Dialog
    if (state.isFilterMenuOpen) {
        SimpleSelectionDialog(
            title = stringResource(R.string.browser_filter_mode),
            options = listOf(
                FilterMode.WHITELIST_ONLY to stringResource(R.string.browser_whitelist),
                FilterMode.BLACKLIST_ONLY to stringResource(R.string.browser_blacklist),
                FilterMode.DISABLED to stringResource(R.string.browser_filter_disabled)
            ),
            selectedOption = settings?.filterMode,
            onSelect = { onEvent(BrowserSettingsEvent.ChangeFilterMode(it)) },
            onDismiss = { onEvent(BrowserSettingsEvent.SetFilterMenuOpen(false)) }
        )
    }

    // Search Engine Selection Dialog
    if (state.isEngineMenuOpen) {
        SimpleSelectionDialog(
            title = stringResource(R.string.browser_select_search_engine),
            options = listOf(
                "kiddle" to stringResource(R.string.browser_engine_kiddle),
                "shaadbin" to stringResource(R.string.browser_engine_shaadbin),
                "duckduckgo" to stringResource(R.string.browser_engine_duckduckgo),
                "google" to stringResource(R.string.browser_engine_google)
            ),
            selectedOption = settings?.searchEngine,
            onSelect = { onEvent(BrowserSettingsEvent.ChangeSearchEngine(it)) },
            onDismiss = { onEvent(BrowserSettingsEvent.SetEngineMenuOpen(false)) }
        )
    }
}

// --- 🚀 NEW REUSABLE UI COMPONENTS ---

@Composable
private fun CompactMenuCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    iconEmoji: String,
    iconBgColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(iconEmoji, fontSize = 20.sp) }

            Spacer(modifier = Modifier.height(12.dp))

            Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = subtitleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun <T> SimpleSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCustomColors.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == value,
                            onClick = { onSelect(value); onDismiss() },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = colors.primary,
                                unselectedColor = colors.textSecondary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel), color = colors.textSecondary)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewSettingsMenuPage() {
    ParentControlTheme {
        SettingsMenuPage(
            state = BrowserSettingsState(
                originalProfile = MockBrowserData.mockProfile,
                draftSettings = MockBrowserData.mockProfile.settings,
                activePage = BrowserSettingsPage.MENU
            ),
            onEvent = {}
        )
    }
}