package com.vahak.mehrban.uiv2.screens.browser.keywords

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.BrowserBlockedKeywordEntity
import com.vahak.mehrban.presentation.browser.settings.keywords.BrowserBlockedKeywordsEvent
import com.vahak.mehrban.presentation.browser.settings.keywords.BrowserBlockedKeywordsState
import com.vahak.mehrban.presentation.browser.settings.keywords.BrowserBlockedKeywordsViewModel
import com.vahak.mehrban.uiv2.components.browser.SettingsListItemCard
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.screens.browser.MockBrowserData
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

// --- STATEFUL WRAPPER ---
@Composable
fun BrowserBlockedKeywordsScreen(
    viewModel: BrowserBlockedKeywordsViewModel = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    BrowserBlockedKeywordsContent(
        state = state, onEvent = viewModel::onEvent, onBackClick = onBackClick
    )
}

// --- STATELESS CONTENT ---
@Composable
fun BrowserBlockedKeywordsContent(
    state: BrowserBlockedKeywordsState,
    onEvent: (BrowserBlockedKeywordsEvent) -> Unit,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BrowserBlockedKeywordEntity?>(null) }

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
            MehrbanHeader(
                title = stringResource(R.string.browser_blocked_keywords),
                subtitle = stringResource(R.string.browser_back_to_menu),
                iconEmoji = "🚫",
                action = HeaderAction.Back(onClick = onBackClick)
            )

            if (state.keywords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.browser_settings_empty_list_hint),
                        color = colors.textHint
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.keywords) { item ->
                        SettingsListItemCard(
                            title = item.keyword,
                            onEditClick = { editingItem = item; showDialog = true },
                            onDeleteClick = { onEvent(BrowserBlockedKeywordsEvent.RemoveKeyword(item.keyword)) })
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingItem = null; showDialog = true },
            containerColor = colors.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }

    if (showDialog) {
        AddKeywordDialog(
            initialKeyword = editingItem?.keyword ?: "",
            onDismiss = { showDialog = false; editingItem = null },
            onSave = { keyword ->
                if (editingItem != null) {
                    onEvent(BrowserBlockedKeywordsEvent.EditKeyword(editingItem!!.keyword, keyword))
                } else {
                    onEvent(BrowserBlockedKeywordsEvent.AddKeyword(keyword))
                }
                showDialog = false
                editingItem = null
            })
    }
}

// --- STATELESS DIALOG ---
@Composable
fun AddKeywordDialog(
    initialKeyword: String, onDismiss: () -> Unit, onSave: (String) -> Unit
) {
    val colors = LocalCustomColors.current
    var keywordInput by remember { mutableStateOf(initialKeyword) }

    Dialog(onDismissRequest = onDismiss) {
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
                    stringResource(R.string.browser_add_keyword_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = keywordInput,
                    onValueChange = { keywordInput = it },
                    label = {
                        Text(
                            stringResource(R.string.browser_keyword_hint), color = colors.textHint
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.divider,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            stringResource(R.string.cancel), color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = keywordInput.isNotBlank(),
                        onClick = { onSave(keywordInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.save), color = Color.White)
                    }
                }
            }
        }
    }
}

// --- PREVIEWS ---
@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewKeywordsContent() {
    ParentControlTheme {
        BrowserBlockedKeywordsContent(
            state = BrowserBlockedKeywordsState(keywords = MockBrowserData.mockProfile.blockedKeywords),
            onEvent = {},
            onBackClick = {})
    }
}