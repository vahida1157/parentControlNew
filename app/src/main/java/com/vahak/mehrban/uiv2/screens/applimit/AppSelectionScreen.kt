package com.vahak.mehrban.uiv2.screens.applimit

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.appselection.AppFilterTab
import com.vahak.mehrban.presentation.appselection.AppItemUi
import com.vahak.mehrban.presentation.appselection.AppSelectionEffectV2
import com.vahak.mehrban.presentation.appselection.AppSelectionEventV2
import com.vahak.mehrban.presentation.appselection.AppSelectionStateV2
import com.vahak.mehrban.presentation.appselection.AppSelectionViewModelV2
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun AppSelectionScreen(
    viewModel: AppSelectionViewModelV2 = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is AppSelectionEffectV2.NavigateBack -> onBackClick()
                is AppSelectionEffectV2.ShowToast -> Toast.makeText(
                    context, effect.message, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    AppSelectionContent(
        state = state, onEvent = viewModel::onEvent
    )
}

@Composable
fun AppSelectionContent(
    state: AppSelectionStateV2, onEvent: (AppSelectionEventV2) -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HARMONIZED HEADER ---
            MehrbanHeader(
                title = stringResource(R.string.app_lock_title),
                subtitle = stringResource(R.string.app_lock_subtitle),
                iconEmoji = "🔒",
                action = HeaderAction.Back { onEvent(AppSelectionEventV2.BackClicked) },
            )

            // --- HARMONIZED CARD ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp)
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 20.dp, bottom = 20.dp)
                ) {
                    // --- Search Bar ---
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = { onEvent(AppSelectionEventV2.UpdateSearchQuery(it)) },
                            placeholder = {
                                Text(
                                    stringResource(R.string.search_hint),
                                    color = colors.textHint,
                                    fontSize = 13.sp
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colors.textPrimary,
                                unfocusedTextColor = colors.textPrimary,
                                focusedContainerColor = colors.background,
                                unfocusedContainerColor = colors.background,
                                focusedBorderColor = colors.primary,
                                unfocusedBorderColor = colors.divider,
                            ),
                            singleLine = true
                        )
                    }

                    // --- Tabs ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterTabItemV2(
                            title = stringResource(R.string.tab_blocked),
                            isSelected = state.selectedTab == AppFilterTab.BLOCKED,
                            modifier = Modifier.weight(1f),
                            onClick = { onEvent(AppSelectionEventV2.TabSelected(AppFilterTab.BLOCKED)) })
                        FilterTabItemV2(
                            title = stringResource(R.string.tab_allowed),
                            isSelected = state.selectedTab == AppFilterTab.ALLOWED,
                            modifier = Modifier.weight(1f),
                            onClick = { onEvent(AppSelectionEventV2.TabSelected(AppFilterTab.ALLOWED)) })
                        FilterTabItemV2(
                            title = stringResource(R.string.tab_all),
                            isSelected = state.selectedTab == AppFilterTab.ALL,
                            modifier = Modifier.weight(1f),
                            onClick = { onEvent(AppSelectionEventV2.TabSelected(AppFilterTab.ALL)) })
                    }

                    // --- Scrollable App List ---
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center), color = colors.primary
                            )
                        } else if (state.filteredApps.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_apps_found),
                                color = colors.textHint,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(vertical = 8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val apps = state.filteredApps
                                itemsIndexed(
                                    apps, key = { _, it -> it.packageName }) { index, app ->
                                    AppListItemV2(
                                        app = app,
                                        isLastItem = index == apps.lastIndex,
                                        onToggle = { isAllowed ->
                                            onEvent(
                                                AppSelectionEventV2.ToggleApp(
                                                    app.packageName, isAllowed
                                                )
                                            )
                                        })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onEvent(AppSelectionEventV2.SaveClicked) },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(56.dp)
                            .shadow(
                                8.dp,
                                RoundedCornerShape(14.dp),
                                spotColor = colors.primary.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            disabledContainerColor = colors.backgroundButtonDisable
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                color = colors.textOnPrimaryVariant,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                stringResource(R.string.button_save_settings),
                                color = colors.textOnPrimaryVariant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
fun FilterTabItemV2(
    title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.background)
            .border(
                1.dp, if (isSelected) colors.primary else colors.divider, RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Text(
            text = title,
            color = if (isSelected) colors.primary else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppListItemV2(
    app: AppItemUi, isLastItem: Boolean, onToggle: (Boolean) -> Unit
) {
    val colors = LocalCustomColors.current

    val bgColors =
        listOf(Color(0xFFE3F2FD), Color(0xFFFFF3E0), Color(0xFFE8F5E9), Color(0xFFF3E5F5))
    val randomBg = remember(app.packageName) { bgColors.random() }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(randomBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (app.iconBitmap != null) {
                        Image(
                            bitmap = app.iconBitmap,
                            contentDescription = app.appName,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Text("📱", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = app.appName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = app.packageName.split(".").last()
                            .replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }

            Switch(
                checked = app.isAllowed,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.primary,
                    uncheckedThumbColor = colors.textSecondary,
                    uncheckedTrackColor = colors.divider
                )
            )
        }

        if (!isLastItem) {
            HorizontalDivider(
                color = colors.divider,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, name = "1. App Lock Light", locale = "fa")
@Composable
fun AppSelectionPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        AppSelectionContent(
            state = AppSelectionStateV2(
                isLoading = false, installedApps = listOf(
                    AppItemUi("com.whatsapp", "واتس‌اپ", true),
                    AppItemUi("com.instagram", "اینستاگرام", false),
                    AppItemUi("com.mojang.minecraftpe", "ماینکرافت", true)
                )
            ), onEvent = {})
    }
}

@Preview(showBackground = true, name = "2. App Lock Dark", locale = "fa")
@Composable
fun AppSelectionPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        AppSelectionContent(
            state = AppSelectionStateV2(
                isLoading = false, selectedTab = AppFilterTab.BLOCKED, installedApps = listOf(
                    AppItemUi("com.instagram", "اینستاگرام", false),
                    AppItemUi("com.tiktok", "تیک‌تاک", false)
                )
            ), onEvent = {})
    }
}