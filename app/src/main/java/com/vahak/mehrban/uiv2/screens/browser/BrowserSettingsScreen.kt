package com.vahak.mehrban.uiv2.screens.browser

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedSiteEntity
import com.vahak.mehrban.core.data.local.entity.BrowserHistoryEntity
import com.vahak.mehrban.core.data.local.entity.BrowserSettingsEntity
import com.vahak.mehrban.core.data.local.entity.FilterMode
import com.vahak.mehrban.core.data.local.entity.FullBrowserProfile
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsEffect
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsEvent
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsPage
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsState
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsViewModel
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun BrowserSettingsScreen(
    viewModel: BrowserSettingsViewModel = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserSettingsEffect.ShowToast -> Toast.makeText(
                    context, effect.messageResId, Toast.LENGTH_SHORT
                ).show()

                is BrowserSettingsEffect.ExitScreen -> onBackClick()
            }
        }
    }

    BrowserSettingsContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun BrowserSettingsContent(
    state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    BackHandler { onEvent(BrowserSettingsEvent.OnBackPress) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            MehrbanHeader(
                title = stringResource(R.string.browser_settings_title),
                subtitle = if (state.activePage == BrowserSettingsPage.MENU) stringResource(R.string.browser_settings_subtitle) else stringResource(
                    R.string.browser_back_to_menu
                ),
                iconEmoji = "🌐",
                action = HeaderAction.Back(onClick = { onEvent(BrowserSettingsEvent.OnBackPress) })
            )

            when (state.activePage) {
                BrowserSettingsPage.MENU -> SettingsMenu(state, onEvent)
                BrowserSettingsPage.ALLOWED_SITES -> AllowedSitesSubScreen(state, onEvent)
                BrowserSettingsPage.BLOCKED_SITES -> BlockedSitesSubScreen(state, onEvent)
                BrowserSettingsPage.KEYWORDS -> KeywordsSubScreen(state, onEvent)
                BrowserSettingsPage.HISTORY -> HistorySubScreen(state, onEvent)
            }
        }

        // 🚀 SAVE LOCAL DRAFT FAB
        if (state.hasUnsavedChanges && state.activePage == BrowserSettingsPage.MENU) {
            ExtendedFloatingActionButton(
                onClick = { onEvent(BrowserSettingsEvent.SaveAndExit) },
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
                    .padding(bottom = 24.dp),
                containerColor = colors.primary,
                contentColor = Color.White
            ) {
                Icon(AppIcons.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
            }
        }
    }

    // 🚀 LOCAL UNSAVED CHANGES DIALOG
    if (state.showUnsavedDialog) {
        val unsavedChangesText = stringResource(R.string.browser_unsaved_changes)
        val unsavedDescText = stringResource(R.string.browser_unsaved_desc)
        val saveAndExitText = stringResource(R.string.browser_save_and_exit)
        val discardAndExitText = stringResource(R.string.browser_discard_and_exit)
        AlertDialog(
            onDismissRequest = { onEvent(BrowserSettingsEvent.DismissUnsavedDialog) },
            title = {
                Text(
                    unsavedChangesText, fontWeight = FontWeight.Bold, color = colors.textPrimary
                )
            },
            text = { Text(unsavedDescText, color = colors.textSecondary) },
            confirmButton = {
                Button(
                    onClick = { onEvent(BrowserSettingsEvent.SaveAndExit) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text(saveAndExitText)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(BrowserSettingsEvent.DiscardAndExit) }) {
                    Text(discardAndExitText, color = colors.red)
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun SettingsMenu(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    val colors = LocalCustomColors.current
    val settings = state.draftSettings // Read from DRAFT

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {

        if (settings?.filterMode == FilterMode.WHITELIST_ONLY && (state.draftAllowedSites.isEmpty())) {
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

        Text(
            stringResource(R.string.browser_filter_mode),
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterModeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.browser_whitelist),
                desc = stringResource(R.string.browser_whitelist_desc),
                isSelected = settings?.filterMode == FilterMode.WHITELIST_ONLY,
                selectedColor = colors.green,
                onClick = { onEvent(BrowserSettingsEvent.ChangeFilterMode(FilterMode.WHITELIST_ONLY)) })
            FilterModeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.browser_blacklist),
                desc = stringResource(R.string.browser_blacklist_desc),
                isSelected = settings?.filterMode == FilterMode.BLACKLIST_ONLY,
                selectedColor = colors.red,
                onClick = { onEvent(BrowserSettingsEvent.ChangeFilterMode(FilterMode.BLACKLIST_ONLY)) })
            FilterModeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.browser_filter_disabled),
                desc = stringResource(R.string.browser_filter_disabled_desc),
                isSelected = settings?.filterMode == FilterMode.DISABLED,
                selectedColor = colors.textHint,
                onClick = { onEvent(BrowserSettingsEvent.ChangeFilterMode(FilterMode.DISABLED)) })
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                .clickable { onEvent(BrowserSettingsEvent.SetEngineMenuOpen(true)) },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
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
                        "kiddle" -> stringResource(R.string.browser_engine_kiddle);
                        "duckduckgo" -> stringResource(R.string.browser_engine_duckduckgo);
                        "google" -> stringResource(R.string.browser_engine_google);
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
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface)
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp)
                        .background(colors.orange.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
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
                    onCheckedChange = { onEvent(BrowserSettingsEvent.ToggleCartoonWorld(it)) })
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.browser_access_management),
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SettingMenuRow(
            "✅",
            stringResource(R.string.browser_allowed_sites),
            stringResource(R.string.browser_sites_count, state.draftAllowedSites.size),
            onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.ALLOWED_SITES)) })
        SettingMenuRow(
            "⛔",
            stringResource(R.string.browser_blocked_sites),
            stringResource(R.string.browser_sites_count, state.draftBlockedSites.size),
            onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.BLOCKED_SITES)) })
        SettingMenuRow(
            "🚫",
            stringResource(R.string.browser_blocked_keywords),
            stringResource(R.string.browser_keywords_count, state.draftKeywords.size),
            onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.KEYWORDS)) })
        SettingMenuRow(
            "🕒",
            stringResource(R.string.browser_history),
            stringResource(R.string.browser_view_activity),
            onClick = { onEvent(BrowserSettingsEvent.NavigateTo(BrowserSettingsPage.HISTORY)) })

        Spacer(modifier = Modifier.height(80.dp))
    }

    if (state.isEngineMenuOpen) {
        AlertDialog(
            onDismissRequest = { onEvent(BrowserSettingsEvent.SetEngineMenuOpen(false)) },
            title = {
                Text(
                    stringResource(R.string.browser_select_search_engine),
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column {
                    listOf(
                        "kiddle" to stringResource(R.string.browser_engine_kiddle),
                        "shaadbin" to stringResource(R.string.browser_engine_shaadbin),
                        "duckduckgo" to stringResource(R.string.browser_engine_duckduckgo),
                        "google" to stringResource(R.string.browser_engine_google)
                    ).forEach { (id, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable { onEvent(BrowserSettingsEvent.ChangeSearchEngine(id)) }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = settings?.searchEngine == id,
                                onClick = { onEvent(BrowserSettingsEvent.ChangeSearchEngine(id)) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = colors.primary,
                                    unselectedColor = colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, color = colors.textPrimary)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { onEvent(BrowserSettingsEvent.SetEngineMenuOpen(false)) }) {
                    Text(stringResource(R.string.cancel), color = colors.textSecondary)
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun FilterModeCard(
    modifier: Modifier,
    title: String,
    desc: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = modifier.clickable { onClick() }.border(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) selectedColor else colors.divider,
            RoundedCornerShape(12.dp)
        ),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) selectedColor.copy(alpha = 0.1f) else colors.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) selectedColor else colors.textPrimary,
                fontSize = 14.sp
            )
            Text(desc, color = colors.textSecondary, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
fun SettingMenuRow(emoji: String, title: String, desc: String, onClick: () -> Unit) {
    val colors = LocalCustomColors.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text(desc, fontSize = 12.sp, color = colors.textSecondary)
            }
            Icon(AppIcons.ChevronLeft, contentDescription = null, tint = colors.textHint)
        }
    }
}

@Composable
fun AllowedSitesSubScreen(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    val colors = LocalCustomColors.current
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(state.draftAllowedSites) { site ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                site.label, fontWeight = FontWeight.Bold, color = colors.textPrimary
                            ); Text(site.url, fontSize = 12.sp, color = colors.textSecondary)
                        }
                        IconButton(onClick = { onEvent(BrowserSettingsEvent.RemoveAllowedSite(site.url)) }) {
                            Icon(
                                AppIcons.DeleteForever,
                                contentDescription = "Delete",
                                tint = colors.red
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = colors.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Text("+", color = Color.White, fontSize = 24.sp) }
    }

    if (showAddDialog) {
        var url by remember { mutableStateOf("") }
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.browser_add_site_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.browser_site_url_hint)) }); Spacer(
                    modifier = Modifier.height(8.dp)
                ); OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.browser_site_name_hint)) })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (url.isNotBlank() && label.isNotBlank()) {
                        onEvent(BrowserSettingsEvent.AddAllowedSite(url, label)); showAddDialog =
                            false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(
                        stringResource(
                            R.string.cancel
                        )
                    )
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun BlockedSitesSubScreen(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    val colors = LocalCustomColors.current
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(state.draftBlockedSites) { site ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(site.url, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        IconButton(onClick = { onEvent(BrowserSettingsEvent.RemoveBlockedSite(site.url)) }) {
                            Icon(
                                AppIcons.DeleteForever,
                                contentDescription = "Delete",
                                tint = colors.red
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = colors.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Text("+", color = Color.White, fontSize = 24.sp) }
    }

    if (showAddDialog) {
        var url by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.browser_add_blocked_site_title)) },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(stringResource(R.string.browser_site_address_hint)) })
            },
            confirmButton = {
                Button(onClick = {
                    if (url.isNotBlank()) {
                        onEvent(BrowserSettingsEvent.AddBlockedSite(url)); showAddDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(
                        stringResource(
                            R.string.cancel
                        )
                    )
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun KeywordsSubScreen(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    val colors = LocalCustomColors.current
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(state.draftKeywords) { kw ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(kw.keyword, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        IconButton(onClick = { onEvent(BrowserSettingsEvent.RemoveBlockedKeyword(kw.keyword)) }) {
                            Icon(
                                AppIcons.DeleteForever,
                                contentDescription = "Delete",
                                tint = colors.red
                            )
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = colors.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Text("+", color = Color.White, fontSize = 24.sp) }
    }

    if (showAddDialog) {
        var keyword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.browser_add_keyword_title)) },
            text = {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.browser_keyword_hint)) })
            },
            confirmButton = {
                Button(onClick = {
                    if (keyword.isNotBlank()) {
                        onEvent(BrowserSettingsEvent.AddBlockedKeyword(keyword)); showAddDialog =
                            false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(
                        stringResource(
                            R.string.cancel
                        )
                    )
                }
            },
            containerColor = colors.surface
        )
    }
}

@Composable
fun HistorySubScreen(state: BrowserSettingsState, onEvent: (BrowserSettingsEvent) -> Unit) {
    val colors = LocalCustomColors.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val todayText = stringResource(R.string.today)
    val yesterdayText = stringResource(R.string.yesterday)

    val timeFormatter =
        remember { java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()) }

    // 🚀 Date Logic to determine "Today", "Yesterday", or formatted date
    val displayDate = remember(state.selectedDateMillis) {
        val selected =
            java.util.Calendar.getInstance().apply { timeInMillis = state.selectedDateMillis }
        val today = java.util.Calendar.getInstance()
        val yesterday =
            java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val fmt = java.text.SimpleDateFormat("yyyy/MM/dd", java.util.Locale.getDefault())

        when (fmt.format(selected.time)) {
            fmt.format(today.time) -> todayText
            fmt.format(yesterday.time) -> yesterdayText
            else -> fmt.format(selected.time)
        }
    }

    // 🚀 Disable "Next Day" if we are already viewing today
    val isToday = remember(state.selectedDateMillis) {
        val selected =
            java.util.Calendar.getInstance().apply { timeInMillis = state.selectedDateMillis }
        val today = java.util.Calendar.getInstance()
        selected.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) && selected.get(
            java.util.Calendar.DAY_OF_YEAR
        ) == today.get(java.util.Calendar.DAY_OF_YEAR)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // --- 🚀 DATE NAVIGATION BAR ---
        Row(
            modifier = Modifier.fillMaxWidth().background(colors.surface)
                .border(width = 1.dp, color = colors.divider)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Day (Right arrow in RTL)
            IconButton(onClick = { onEvent(BrowserSettingsEvent.ChangeHistoryDate(-1)) }) {
                Text("▶", color = colors.primary, fontSize = 18.sp) // Standard unicode arrow
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
                    val date = java.util.Date(log.timestamp)
                    val browserLinkErrorText = stringResource(R.string.browser_link_error)

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            // 🚀 Parent clicks to visit the URL
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    log.url.toUri()
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context, browserLinkErrorText, Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.url,
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = LocalTextStyle.current.copy(textDirection = androidx.compose.ui.text.style.TextDirection.Ltr)
                            )
                        }

                        // Copy Button
                        val linkCopiedText = stringResource(R.string.browser_link_copied)
                        IconButton(
                            onClick = {
                                clipboardManager.setText(
                                    androidx.compose.ui.text.AnnotatedString(
                                        log.url
                                    )
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

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------
private val mockProfile = FullBrowserProfile(
    settings = BrowserSettingsEntity(
        childId = "1", searchEngine = "kiddle", filterMode = FilterMode.WHITELIST_ONLY
    ),
    allowedSites = listOf(
        BrowserAllowedSiteEntity(
            childId = "1", url = "aparat.com", label = "آپارات"
        )
    ),
    blockedSites = listOf(BrowserBlockedSiteEntity(childId = "1", url = "badsite.com")),
    blockedKeywords = listOf(BrowserBlockedKeywordEntity(childId = "1", keyword = "ترسناک"))
)

private val mockHistory = listOf(
    BrowserHistoryEntity(id = 1, childId = "1", url = "https://aparat.com", title = "آپارات"),
    BrowserHistoryEntity(id = 2, childId = "1", url = "https://kiddle.co", title = "Kiddle Search")
)

@Preview(showBackground = true, locale = "fa", name = "Settings Menu")
@Composable
fun PreviewSettingsMenu() {
    ParentControlTheme {
        BrowserSettingsContent(
            state = BrowserSettingsState(
                originalProfile = mockProfile,
                draftSettings = mockProfile.settings,
                draftAllowedSites = mockProfile.allowedSites,
                draftBlockedSites = mockProfile.blockedSites,
                draftKeywords = mockProfile.blockedKeywords,
                history = mockHistory,
                isLoading = false,
                activePage = BrowserSettingsPage.MENU
            ), onEvent = {})
    }
}

@Preview(showBackground = true, locale = "fa", name = "History Page")
@Composable
fun PreviewHistoryPage() {
    ParentControlTheme {
        BrowserSettingsContent(
            state = BrowserSettingsState(
                originalProfile = mockProfile,
                history = mockHistory,
                isLoading = false,
                activePage = BrowserSettingsPage.HISTORY
            ), onEvent = {})
    }
}