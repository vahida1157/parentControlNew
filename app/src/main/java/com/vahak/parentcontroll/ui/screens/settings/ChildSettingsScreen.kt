package com.vahak.parentcontroll.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import com.vahak.parentcontroll.presentation.setting.SettingsEffect
import com.vahak.parentcontroll.presentation.setting.SettingsEvent
import com.vahak.parentcontroll.presentation.setting.SettingsState
import com.vahak.parentcontroll.presentation.setting.SettingsViewModel
import com.vahak.parentcontroll.ui.component.ParentalHeader
import com.vahak.parentcontroll.ui.component.SettingsGridItem
import com.vahak.parentcontroll.ui.component.SettingsSectionTitle
import com.vahak.parentcontroll.ui.component.ThemeToggleCard
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

// 1. STATEFUL WRAPPER
@Composable
fun ChildSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToFeature: (String) -> Unit,
    onInterceptForPermissions: (String, List<String>) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.NavigateBack -> onBackClick()
                is SettingsEffect.NavigateToFeature -> onNavigateToFeature(effect.route)
                is SettingsEffect.ShowToast -> Toast.makeText(
                    context, effect.message, Toast.LENGTH_SHORT
                ).show()

                is SettingsEffect.NavigateToPermissionSlider -> {
                    onInterceptForPermissions(effect.route, effect.missingPermissions)
                }
            }
        }
    }

    ChildSettingsContent(
        state = state, onEvent = viewModel::onEvent
    )
}

// 2. STATELESS CONTENT (Matches the HTML Grid perfectly)
@Composable
fun ChildSettingsContent(
    state: SettingsState, onEvent: (SettingsEvent) -> Unit
) {
    val colors = LocalCustomColors.current
    val isThemeActive = state.settings?.isChildThemeActive ?: true
    val localContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .systemBarsPadding()
    ) {
        ParentalHeader(
            title = "تنظیمات نظارتی",
            subtitle = "مدیریت کامل دسترسی‌های فرزند",
            onBackClick = { onEvent(SettingsEvent.BackClicked) },
            onHelpClick = { onEvent(SettingsEvent.HelpClicked) })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {

            ThemeToggleCard(
                isActive = isThemeActive,
                onToggle = { isActive -> onEvent(SettingsEvent.ToggleChildTheme(isActive)) })

            // --- SECTION 1: Time Management ---
            SettingsSectionTitle(
                label = "مدیریت زمان", icon = AppIcons.ChartPie
            ) // Use Clock icon if you have it
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "قفل زمان", icon = AppIcons.ChartPie, // Use Hourglass
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "time_limit", localContext
                                )
                            )
                        })
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "زمان خواب", icon = AppIcons.ChartPie, // Use Bed
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "sleep_time", localContext
                                )
                            )
                        })
                }
                Spacer(modifier = Modifier.weight(1f)) // Empty box to keep grid alignment
            }

            // --- SECTION 2: Internet & Apps ---
            SettingsSectionTitle(
                label = "مدیریت اینترنت و برنامه‌ها", icon = AppIcons.LockBadge
            ) // Use Shield if you have it
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "قفل برنامه‌ها", icon = AppIcons.Games, // Use Apps icon
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "app_lock", localContext
                                )
                            )
                        })
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "مدیریت سایت‌ها", icon = AppIcons.ContentLayer, // Use Globe
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "site_management", localContext
                                )
                            )
                        })
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "جستجوی ایمن", icon = AppIcons.Help, // Use Search
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "safe_search", localContext
                                )
                            )
                        })
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "جلوگیری از حذف", icon = AppIcons.LockBadge, // Use Trash Slash
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "prevent_delete", localContext
                                )
                            )
                        })
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "جلوگیری از تبلیغ",
                        icon = AppIcons.LockBadge, // Use Ban
                        onClick = { onEvent(SettingsEvent.HelpClicked) } // Not implemented yet
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            // --- SECTION 3: Content & Other ---
            SettingsSectionTitle(label = "سایر تنظیمات و محتوا", icon = AppIcons.Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "موقعیت مکانی", icon = AppIcons.ChartBar, // Use Location
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "location", localContext
                                )
                            )
                        })
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "محافظ چشم", icon = AppIcons.Profile, // Use Eye
                        onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "eye_protect", localContext
                                )
                            )
                        })
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "فیلم و انیمیشن", icon = AppIcons.Movies, onClick = {
                            onEvent(
                                SettingsEvent.GridItemClicked(
                                    "content_movies", localContext
                                )
                            )
                        })
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// 3. SAFE PREVIEW
@Preview(showBackground = true, name = "Child Settings Screen", locale = "fa")
@Composable
fun SettingsPreview() {
    ParentControlTheme {
        ChildSettingsContent(
            state = SettingsState(
                childId = "mock-123",
                settings = GlobalSettingsEntity(childId = "mock-123", isChildThemeActive = true),
                isLoading = false
            ), onEvent = {})
    }
}