package com.vahak.mehrban.uiv2.screens.browser

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.BrowserKeywordEntity
import com.vahak.mehrban.core.data.local.entity.BrowserWhitelistEntity
import com.vahak.mehrban.presentation.browser.BrowserSettingsEffect
import com.vahak.mehrban.presentation.browser.BrowserSettingsEvent
import com.vahak.mehrban.presentation.browser.BrowserSettingsState
import com.vahak.mehrban.presentation.browser.BrowserSettingsViewModel
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

// ----------------------------------------------------------------------------
// STATEFUL COMPONENT
// ----------------------------------------------------------------------------
@Composable
fun BrowserSettingsScreen(
    viewModel: BrowserSettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BrowserSettingsEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    BrowserSettingsContent(
        state = state,
        onEvent = viewModel::onEvent,
        onBackClick = onBackClick
    )
}

// ----------------------------------------------------------------------------
// STATELESS COMPONENT
// ----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserSettingsContent(
    state: BrowserSettingsState,
    onEvent: (BrowserSettingsEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sites, 1 = Keywords

    // Local UI states for Dialogs
    var showAddSiteDialog by remember { mutableStateOf(false) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding().background(colors.background)) {

        // 🚀 Using your correctly structured MehrbanHeader
        MehrbanHeader(
            title = stringResource(R.string.browser_settings_title),
            subtitle = stringResource(R.string.browser_settings_subtitle),
            iconEmoji = "🌐",
            action = HeaderAction.Back(onClick = onBackClick)
        )

        // 🚀 THE FIX: Replaced TabRow with PrimaryTabRow
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = colors.surface,
            contentColor = colors.primary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(
                    text = stringResource(R.string.browser_tab_sites),
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(
                    text = stringResource(R.string.browser_tab_keywords),
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (selectedTab == 0) {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.whitelist) { site ->
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
                                        site.label,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        site.urlPrefix,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        onEvent(
                                            BrowserSettingsEvent.RemoveSite(
                                                site.urlPrefix,
                                                site.label
                                            )
                                        )
                                    }
                                ) {
                                    Icon(
                                        AppIcons.DeleteForever,
                                        contentDescription = stringResource(R.string.remove_desc),
                                        tint = colors.red
                                    )
                                }
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showAddSiteDialog = true },
                    containerColor = colors.primary,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Text("+", color = Color.White, fontSize = 24.sp)
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.keywords) { kw ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    kw.keyword,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                IconButton(
                                    onClick = { onEvent(BrowserSettingsEvent.RemoveKeyword(kw.keyword)) }
                                ) {
                                    Icon(
                                        AppIcons.DeleteForever,
                                        contentDescription = stringResource(R.string.remove_desc),
                                        tint = colors.red
                                    )
                                }
                            }
                        }
                    }
                }
                FloatingActionButton(
                    onClick = { showAddKeywordDialog = true },
                    containerColor = colors.primary,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Text("+", color = Color.White, fontSize = 24.sp)
                }
            }
        }
    }

    // --- Dialogs ---
    if (showAddSiteDialog) {
        var url by remember { mutableStateOf("") }
        var label by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSiteDialog = false },
            title = { Text(stringResource(R.string.browser_add_site_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = { Text(stringResource(R.string.browser_site_url_hint)) },
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text(stringResource(R.string.browser_site_name_hint)) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (url.isNotBlank() && label.isNotBlank()) {
                        onEvent(BrowserSettingsEvent.AddSite(url, label))
                        showAddSiteDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddSiteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddKeywordDialog) {
        var keyword by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddKeywordDialog = false },
            title = { Text(stringResource(R.string.browser_add_keyword_title)) },
            text = {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.browser_keyword_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (keyword.isNotBlank()) {
                        onEvent(BrowserSettingsEvent.AddKeyword(keyword))
                        showAddKeywordDialog = false
                    }
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddKeywordDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------
private val mockWhitelist = listOf(
    BrowserWhitelistEntity(childId = "1", urlPrefix = "aparat.com/kids", label = "آپارات کودک"),
    BrowserWhitelistEntity(childId = "1", urlPrefix = "telewebion.com", label = "تلوبیون")
)

private val mockKeywords = listOf(
    BrowserKeywordEntity(childId = "1", keyword = "بازی ترسناک"),
    BrowserKeywordEntity(childId = "1", keyword = "فیلترشکن")
)

@Preview(showBackground = true, locale = "fa", name = "1. Browser Settings (Light)")
@Composable
fun BrowserSettingsPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        BrowserSettingsContent(
            state = BrowserSettingsState(
                whitelist = mockWhitelist,
                keywords = mockKeywords,
                isLoading = false
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Browser Settings (Dark)")
@Composable
fun BrowserSettingsPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        BrowserSettingsContent(
            state = BrowserSettingsState(
                whitelist = mockWhitelist,
                keywords = mockKeywords,
                isLoading = false
            ),
            onEvent = {},
            onBackClick = {}
        )
    }
}