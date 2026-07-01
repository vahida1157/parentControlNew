// BrowserSettingsScreen.kt
package com.vahak.mehrban.uiv2.screens.browser

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsEffect
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsEvent
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsPage
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsState
import com.vahak.mehrban.presentation.browser.settings.BrowserSettingsViewModel
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.screens.browser.pages.AllowedSitesPage
import com.vahak.mehrban.uiv2.screens.browser.pages.BlockedSitesPage
import com.vahak.mehrban.uiv2.screens.browser.pages.HistoryPage
import com.vahak.mehrban.uiv2.screens.browser.pages.KeywordsPage
import com.vahak.mehrban.uiv2.screens.browser.pages.SettingsMenuPage
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

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
    state: BrowserSettingsState,
    onEvent: (BrowserSettingsEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    // Handle system back presses cleanly through the ViewModel
    BackHandler { onEvent(BrowserSettingsEvent.OnBackPress) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Shared dynamic header for all sub-screens
            val isRootMenu = state.activePage == BrowserSettingsPage.MENU
            MehrbanHeader(
                title = stringResource(R.string.browser_settings_title),
                subtitle = if (isRootMenu) stringResource(R.string.browser_settings_subtitle) else stringResource(
                    R.string.browser_back_to_menu
                ),
                iconEmoji = "🌐",
                action = HeaderAction.Back(onClick = { onEvent(BrowserSettingsEvent.OnBackPress) })
            )

            // Clean, centralized routing to individual stateless pages
            when (state.activePage) {
                BrowserSettingsPage.MENU -> SettingsMenuPage(state, onEvent)
                BrowserSettingsPage.ALLOWED_SITES -> AllowedSitesPage(state, onEvent)
                BrowserSettingsPage.BLOCKED_SITES -> BlockedSitesPage(state, onEvent)
                BrowserSettingsPage.KEYWORDS -> KeywordsPage(state, onEvent)
                BrowserSettingsPage.HISTORY -> HistoryPage(state, onEvent)
            }
        }

        // Global FAB for saving draft changes (Only visible on root menu if changes exist)
        if (state.hasUnsavedChanges && state.activePage == BrowserSettingsPage.MENU) {
            ExtendedFloatingActionButton(
                onClick = { onEvent(BrowserSettingsEvent.SaveAndExit) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
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

    // Custom Themed Unsaved Changes Dialog (Replacing standard Material AlertDialog)
    if (state.showUnsavedDialog) {
        UnsavedChangesCustomDialog(onEvent = onEvent)
    }
}

@Composable
private fun UnsavedChangesCustomDialog(
    onEvent: (BrowserSettingsEvent) -> Unit
) {
    val colors = LocalCustomColors.current
    val browserUnsavedChangesText = stringResource(R.string.browser_unsaved_changes)
    val browserUnsavedDescText = stringResource(R.string.browser_unsaved_desc)
    val browserDiscardAndExitText = stringResource(R.string.browser_discard_and_exit)
    val browserSaveAndExitText = stringResource(R.string.browser_save_and_exit)

    Dialog(onDismissRequest = { onEvent(BrowserSettingsEvent.DismissUnsavedDialog) }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = browserUnsavedChangesText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = browserUnsavedDescText,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { onEvent(BrowserSettingsEvent.DiscardAndExit) }
                    ) {
                        Text(
                            text = browserDiscardAndExitText,
                            color = colors.red,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onEvent(BrowserSettingsEvent.SaveAndExit) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = browserSaveAndExitText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------
@Preview(showBackground = true, locale = "fa", name = "Main Wrapper - Menu")
@Composable
fun PreviewBrowserSettingsContent() {
    ParentControlTheme {
        BrowserSettingsContent(
            state = BrowserSettingsState(
                originalProfile = MockBrowserData.mockProfile,
                draftSettings = MockBrowserData.mockProfile.settings,
                activePage = BrowserSettingsPage.MENU
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "Main Wrapper - Unsaved Dialog")
@Composable
fun PreviewBrowserSettingsUnsavedDialog() {
    ParentControlTheme {
        BrowserSettingsContent(
            state = BrowserSettingsState(
                originalProfile = MockBrowserData.mockProfile,
                draftSettings = MockBrowserData.mockProfile.settings.copy(searchEngine = "google"),
                activePage = BrowserSettingsPage.MENU,
                showUnsavedDialog = true
            ),
            onEvent = {}
        )
    }
}