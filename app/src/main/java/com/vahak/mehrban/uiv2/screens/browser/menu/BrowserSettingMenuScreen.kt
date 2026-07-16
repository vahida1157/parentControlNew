package com.vahak.mehrban.uiv2.screens.browser.menu

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.FilterMode
import com.vahak.mehrban.presentation.browser.settings.menu.BrowserSettingMenuEffect
import com.vahak.mehrban.presentation.browser.settings.menu.BrowserSettingMenuEvent
import com.vahak.mehrban.presentation.browser.settings.menu.BrowserSettingMenuState
import com.vahak.mehrban.presentation.browser.settings.menu.BrowserSettingMenuViewModel
import com.vahak.mehrban.uiv2.components.SimpleSelectionDialog
import com.vahak.mehrban.uiv2.components.browser.SettingMenuRow
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.screens.browser.MockBrowserData
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

// --- STATEFUL COMPOSABLE ---
@Composable
fun BrowserSettingMenuScreen(
    viewModel: BrowserSettingMenuViewModel = hiltViewModel(),
    onNavigateToAllowed: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToKeywords: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is BrowserSettingMenuEffect.ExitScreen) onBackClick()
        }
    }

    BackHandler { viewModel.onEvent(BrowserSettingMenuEvent.OnBackPress) }

    BrowserSettingMenuContent(
        state = state,
        onEvent = viewModel::onEvent,
        onNavigateToAllowed = onNavigateToAllowed,
        onNavigateToBlocked = onNavigateToBlocked,
        onNavigateToKeywords = onNavigateToKeywords,
        onNavigateToHistory = onNavigateToHistory
    )
}

// --- STATELESS COMPOSABLE ---
@Composable
fun BrowserSettingMenuContent(
    state: BrowserSettingMenuState,
    onEvent: (BrowserSettingMenuEvent) -> Unit,
    onNavigateToAllowed: () -> Unit,
    onNavigateToBlocked: () -> Unit,
    onNavigateToKeywords: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val colors = LocalCustomColors.current
    val settings = state.settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        MehrbanHeader(
            title = stringResource(R.string.browser_settings_title),
            subtitle = stringResource(R.string.browser_settings_subtitle),
            iconEmoji = "🌐",
            action = HeaderAction.Back(onClick = { onEvent(BrowserSettingMenuEvent.OnBackPress) })
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // --- EMPTY WHITELIST WARNING ---
            if (settings?.filterMode == FilterMode.WHITELIST_ONLY && state.allowedCount == 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
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

            // --- ACCESS MANAGEMENT ---
            Text(
                stringResource(R.string.browser_access_management),
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                    onClick = { onEvent(BrowserSettingMenuEvent.SetFilterMenuOpen(true)) }
                )

                when (settings?.filterMode) {
                    FilterMode.WHITELIST_ONLY -> {
                        CompactMenuCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.browser_allowed_sites),
                            subtitle = stringResource(
                                R.string.browser_sites_count,
                                state.allowedCount
                            ),
                            iconEmoji = "✅",
                            iconBgColor = colors.primary.copy(alpha = 0.1f),
                            subtitleColor = colors.primary,
                            onClick = onNavigateToAllowed
                        )
                    }

                    FilterMode.BLACKLIST_ONLY -> {
                        CompactMenuCard(
                            modifier = Modifier.weight(1f),
                            title = stringResource(R.string.browser_blocked_sites),
                            subtitle = stringResource(
                                R.string.browser_sites_count,
                                state.blockedCount
                            ),
                            iconEmoji = "⛔",
                            iconBgColor = colors.red.copy(alpha = 0.1f),
                            subtitleColor = colors.red,
                            onClick = onNavigateToBlocked
                        )
                    }

                    else -> {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            SettingMenuRow(
                emoji = "🚫",
                title = stringResource(R.string.browser_blocked_keywords),
                desc = stringResource(R.string.browser_keywords_count, state.keywordsCount),
                onClick = onNavigateToKeywords
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- GENERAL SETTINGS ---
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
                    .clickable { onEvent(BrowserSettingMenuEvent.SetEngineMenuOpen(true)) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                colors.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
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
                        Text(
                            engineName,
                            fontSize = 12.sp,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(AppIcons.ChevronLeft, contentDescription = null, tint = colors.textHint)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                colors.orange.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text("🎬") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.browser_cartoon_world),
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            stringResource(R.string.browser_cartoon_world_desc),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    Switch(
                        checked = settings?.isCartoonWorldEnabled ?: true,
                        onCheckedChange = { onEvent(BrowserSettingMenuEvent.ToggleCartoonWorld(it)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- ACTIVITY SECTION ---
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
                onClick = onNavigateToHistory
            )

            Spacer(modifier = Modifier.height(80.dp))
        }

        // --- DIALOGS ---
        if (state.isFilterMenuOpen) {
            SimpleSelectionDialog(
                title = stringResource(R.string.browser_filter_mode),
                options = listOf(
                    FilterMode.WHITELIST_ONLY to stringResource(R.string.browser_whitelist),
                    FilterMode.BLACKLIST_ONLY to stringResource(R.string.browser_blacklist),
                    FilterMode.DISABLED to stringResource(R.string.browser_filter_disabled)
                ),
                selectedOption = settings?.filterMode,
                onSelect = { onEvent(BrowserSettingMenuEvent.ChangeFilterMode(it)) },
                onDismiss = { onEvent(BrowserSettingMenuEvent.SetFilterMenuOpen(false)) }
            )
        }

        if (state.isEngineMenuOpen) {
            SimpleSelectionDialog(
                title = stringResource(R.string.browser_select_search_engine),
                options = listOf(
                    "shaadbin" to stringResource(R.string.browser_engine_shaadbin),
                    "kiddle" to stringResource(R.string.browser_engine_kiddle),
                    "duckduckgo" to stringResource(R.string.browser_engine_duckduckgo),
                    "google" to stringResource(R.string.browser_engine_google)
                ),
                selectedOption = settings?.searchEngine,
                onSelect = { onEvent(BrowserSettingMenuEvent.ChangeSearchEngine(it)) },
                onDismiss = { onEvent(BrowserSettingMenuEvent.SetEngineMenuOpen(false)) }
            )
        }
    }
}

// --- REUSABLE COMPONENTS ---
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

// --- PREVIEW ---
@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewBrowserSettingMenuContent() {
    ParentControlTheme {
        BrowserSettingMenuContent(
            state = BrowserSettingMenuState(
                settings = MockBrowserData.mockProfile.settings,
                allowedCount = MockBrowserData.mockProfile.allowedSites.size,
                blockedCount = MockBrowserData.mockProfile.blockedSites.size,
                keywordsCount = MockBrowserData.mockProfile.blockedKeywords.size,
            ),
            onEvent = {},
            onNavigateToAllowed = {},
            onNavigateToBlocked = {},
            onNavigateToKeywords = {},
            onNavigateToHistory = {}
        )
    }
}