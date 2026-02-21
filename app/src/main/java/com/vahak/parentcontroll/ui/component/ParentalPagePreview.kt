package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Preview(showBackground = true, locale = "fa")
@Composable
fun ParentalPagePreview() {
    ParentControlTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalCustomColors.current.background)
        ) {
            ParentalHeader(
                title = "تنظیمات نظارتی",
                subtitle = "مدیریت کامل دسترسی‌های فرزند",
                onBackClick = {},
                onHelpClick = {}
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                ThemeToggleCard(isActive = true, onToggle = {})

                SettingsSectionTitle(label = "دسترسی و محتوا", icon = AppIcons.ContentLayer)

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsGridItem(
                            label = "بازی‌ها",
                            icon = AppIcons.Games,
                            isLocked = true
                        ) {}
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsGridItem(label = "فیلم", icon = AppIcons.Movies) {}
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        SettingsGridItem(label = "موسیقی", icon = AppIcons.Music) {}
                    }
                }
            }
        }
    }
}