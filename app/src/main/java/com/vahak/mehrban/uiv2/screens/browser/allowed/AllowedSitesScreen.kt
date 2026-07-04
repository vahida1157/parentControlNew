package com.vahak.mehrban.uiv2.screens.browser.allowed

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.vahak.mehrban.core.data.local.entity.BrowserAllowedSiteEntity
import com.vahak.mehrban.domain.usecase.BrowserUrlValidationResult
import com.vahak.mehrban.presentation.browser.settings.allowed.*
import com.vahak.mehrban.uiv2.components.browser.SettingsListItemCard
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.screens.browser.MockBrowserData
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun AllowedSitesScreen(
    viewModel: AllowedSitesViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is AllowedSitesEffect.ShowToast) {
                Toast.makeText(context, effect.messageResId, Toast.LENGTH_SHORT).show()
            }
        }
    }

    AllowedSitesContent(
        state = state,
        onEvent = viewModel::onEvent,
        validateUrl = viewModel::validateUrl,
        onBackClick = onBackClick
    )
}

@Composable
fun AllowedSitesContent(
    state: AllowedSitesState,
    onEvent: (AllowedSitesEvent) -> Unit,
    validateUrl: (String) -> BrowserUrlValidationResult,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<BrowserAllowedSiteEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
            MehrbanHeader(
                title = stringResource(R.string.browser_allowed_sites),
                subtitle = stringResource(R.string.browser_back_to_menu),
                iconEmoji = "✅",
                action = HeaderAction.Back(onClick = onBackClick)
            )

            if (state.sites.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.browser_settings_empty_list_hint), color = colors.textHint)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    items(state.sites) { site ->
                        SettingsListItemCard(
                            title = site.label,
                            subtitle = site.url,
                            onEditClick = { editingItem = site; showDialog = true },
                            onDeleteClick = { onEvent(AllowedSitesEvent.RemoveSite(site.url)) }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { editingItem = null; showDialog = true },
            containerColor = colors.primary,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).navigationBarsPadding()
        ) {
            Text("+", color = Color.White, fontSize = 24.sp)
        }
    }

    if (showDialog) {
        AddAllowedSiteDialog(
            initialUrl = editingItem?.url ?: "",
            initialLabel = editingItem?.label ?: "",
            validateUrl = validateUrl,
            onDismiss = { showDialog = false; editingItem = null },
            onSave = { url, label ->
                if (editingItem != null) {
                    onEvent(AllowedSitesEvent.EditSite(editingItem!!.url, url, label))
                } else {
                    onEvent(AllowedSitesEvent.AddSite(url, label))
                }
                showDialog = false
                editingItem = null
            }
        )
    }
}

@Composable
fun AddAllowedSiteDialog(
    initialUrl: String,
    initialLabel: String,
    validateUrl: (String) -> BrowserUrlValidationResult,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val colors = LocalCustomColors.current
    var urlInput by remember { mutableStateOf(initialUrl) }
    var labelInput by remember { mutableStateOf(initialLabel) }

    // 🚀 Validation directly applied to state
    val validationResult = validateUrl(urlInput)
    val isValid = validationResult is BrowserUrlValidationResult.Success
    val errorResId = (validationResult as? BrowserUrlValidationResult.Error)?.messageRes
    val showError = !isValid && urlInput.isNotBlank()

    // 🚀 Extracted String Resources
    val titleText = stringResource(R.string.browser_add_site_title)
    val urlHintText = stringResource(R.string.browser_site_url_hint)
    val nameHintText = stringResource(R.string.browser_site_name_hint)
    val cancelText = stringResource(R.string.cancel)
    val saveText = stringResource(R.string.save)
    val errorText = errorResId?.let { stringResource(it) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text(titleText, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text(urlHintText, color = if (showError) colors.red else colors.textHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    isError = showError,
                    supportingText = {
                        if (showError && errorText != null) {
                            Text(errorText, color = colors.red)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary, unfocusedBorderColor = colors.divider,
                        errorBorderColor = colors.red, errorTextColor = colors.textPrimary, errorSupportingTextColor = colors.red,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = labelInput,
                    onValueChange = { labelInput = it },
                    label = { Text(nameHintText, color = colors.textHint) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary, unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.primary, unfocusedBorderColor = colors.divider,
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text(cancelText, color = colors.textSecondary) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = urlInput.isNotBlank() && isValid,
                        onClick = { onSave(urlInput, labelInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(saveText, color = Color.White)
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewAllowedSitesContent() {
    ParentControlTheme {
        AllowedSitesContent(
            state = AllowedSitesState(sites = MockBrowserData.mockProfile.allowedSites),
            onEvent = {},
            validateUrl = { BrowserUrlValidationResult.Success },
            onBackClick = {}
        )
    }
}