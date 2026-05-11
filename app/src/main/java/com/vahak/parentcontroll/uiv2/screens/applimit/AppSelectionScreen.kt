package com.vahak.parentcontroll.uiv2.screens.applimit

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.appselection.AppFilterTab
import com.vahak.parentcontroll.presentation.appselection.AppItemUi
import com.vahak.parentcontroll.presentation.appselection.AppSelectionEffectV2
import com.vahak.parentcontroll.presentation.appselection.AppSelectionEventV2
import com.vahak.parentcontroll.presentation.appselection.AppSelectionStateV2
import com.vahak.parentcontroll.presentation.appselection.AppSelectionViewModelV2
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme

@Composable
fun AppSelectionScreen(
    viewModel: AppSelectionViewModelV2 = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is AppSelectionEffectV2.NavigateBack) onBackClick()
        }
    }

    AppSelectionContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun AppSelectionContent(
    state: AppSelectionStateV2,
    onEvent: (AppSelectionEventV2) -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // --- Modal Style Header ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp, bottom = 20.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔒 قفل برنامه‌ها",
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
                fontWeight = FontWeight.Black
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.surface, CircleShape)
                    .shadow(4.dp, CircleShape)
                    .clickable { onEvent(AppSelectionEventV2.BackClicked) },
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.Close, contentDescription = "Close", tint = colors.textPrimary)
            }
        }

        // --- Search Bar ---
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onEvent(AppSelectionEventV2.UpdateSearchQuery(it)) },
                placeholder = {
                    Text(
                        "جستجوی برنامه...",
                        color = colors.textHint,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
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
                title = "مسدود شده",
                isSelected = state.selectedTab == AppFilterTab.BLOCKED,
                modifier = Modifier.weight(1f),
                onClick = { onEvent(AppSelectionEventV2.TabSelected(AppFilterTab.BLOCKED)) }
            )
            FilterTabItemV2(
                title = "مجاز",
                isSelected = state.selectedTab == AppFilterTab.ALLOWED,
                modifier = Modifier.weight(1f),
                onClick = { onEvent(AppSelectionEventV2.TabSelected(AppFilterTab.ALLOWED)) }
            )
            FilterTabItemV2(
                title = "همه برنامه‌ها",
                isSelected = state.selectedTab == AppFilterTab.ALL,
                modifier = Modifier.weight(1f),
                onClick = { onEvent(AppSelectionEventV2.TabSelected(AppFilterTab.ALL)) }
            )
        }

        // --- App List Container ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .background(colors.surface)
                .shadow(4.dp, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.primary
                )
            } else if (state.filteredApps.isEmpty()) {
                Text(
                    text = "برنامه‌ای یافت نشد.",
                    color = colors.textHint,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    val apps = state.filteredApps
                    itemsIndexed(apps, key = { _, it -> it.packageName }) { index, app ->
                        AppListItemV2(
                            app = app,
                            isLastItem = index == apps.lastIndex,
                            onToggle = { isAllowed ->
                                onEvent(AppSelectionEventV2.ToggleApp(app.packageName, isAllowed))
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
fun FilterTabItemV2(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isSelected) colors.primary else colors.surface)
            .border(
                2.dp,
                if (isSelected) colors.primary else colors.divider,
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AppListItemV2(
    app: AppItemUi,
    isLastItem: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalCustomColors.current

    // Simulate the alternating pastel colors from the HTML prototype
    val bgColors =
        listOf(Color(0xFFE3F2FD), Color(0xFFFFF3E0), Color(0xFFE8F5E9), Color(0xFFF3E5F5))
    val randomBg = remember(app.packageName) { bgColors.random() }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // App Icon Box
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
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        // Fallback emoji
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

            // V2 Toggle Switch (Matches Custom HTML CSS Toggle)
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
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
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
                isLoading = false,
                installedApps = listOf(
                    AppItemUi("com.whatsapp", "واتس‌اپ", true),
                    AppItemUi("com.instagram", "اینستاگرام", false),
                    AppItemUi("com.mojang.minecraftpe", "ماینکرافت", true)
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. App Lock Dark", locale = "fa")
@Composable
fun AppSelectionPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        AppSelectionContent(
            state = AppSelectionStateV2(
                isLoading = false,
                selectedTab = AppFilterTab.BLOCKED,
                installedApps = listOf(
                    AppItemUi("com.instagram", "اینستاگرام", false),
                    AppItemUi("com.tiktok", "تیک‌تاک", false)
                )
            ),
            onEvent = {}
        )
    }
}