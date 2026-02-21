package com.vahak.parentcontroll.ui.component

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vahak.parentcontroll.presentation.setting.SettingsEffect
import com.vahak.parentcontroll.presentation.setting.SettingsEvent
import com.vahak.parentcontroll.presentation.setting.SettingsViewModel
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors

@Composable
fun ParentalSettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Handle One-Off Effects from ViewModel
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsEffect.NavigateBack -> onBackClick()
                is SettingsEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // 1. Header
        ParentalHeader(
            title = "تنظیمات نظارتی",
            subtitle = "مدیریت کامل دسترسی‌های فرزند",
            onBackClick = { viewModel.onEvent(SettingsEvent.BackClicked) },
            onHelpClick = { viewModel.onEvent(SettingsEvent.HelpClicked) }
        )

        // 2. Settings Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {

            // Theme Toggle
            ThemeToggleCard(
                isActive = state.isChildThemeActive,
                onToggle = { isActive -> viewModel.onEvent(SettingsEvent.ToggleChildTheme(isActive)) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Section Title
            SettingsSectionTitle(label = "دسترسی و محتوا", icon = AppIcons.ContentLayer)

            // Grid Items Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "بازی‌ها",
                        icon = AppIcons.Games,
                        isLocked = true,
                        onClick = { viewModel.onEvent(SettingsEvent.GridItemClicked("بازی‌ها")) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "فیلم",
                        icon = AppIcons.Movies,
                        onClick = { viewModel.onEvent(SettingsEvent.GridItemClicked("فیلم")) }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    SettingsGridItem(
                        label = "موسیقی",
                        icon = AppIcons.Music,
                        onClick = { viewModel.onEvent(SettingsEvent.GridItemClicked("موسیقی")) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp)) // Bottom padding
        }
    }
}