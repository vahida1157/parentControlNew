package com.vahak.parentcontroll.uiv2.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.core.util.LogExporter
import com.vahak.parentcontroll.presentation.setting.AppSettingsEffect
import com.vahak.parentcontroll.presentation.setting.AppSettingsEvent
import com.vahak.parentcontroll.presentation.setting.AppSettingsState
import com.vahak.parentcontroll.presentation.setting.ApplicationSettingsViewModel
import com.vahak.parentcontroll.uiv2.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme
import kotlinx.coroutines.launch

// --- 1. STATEFUL WRAPPER ---
@Composable
fun ApplicationSettingsScreen(
    viewModel: ApplicationSettingsViewModel = hiltViewModel(),
    onNavigateToPasswordManagement: () -> Unit,
    onLogoutComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is AppSettingsEffect.NavigateToLogin) {
                onLogoutComplete()
            }
        }
    }

    ApplicationSettingsContent(
        state = state,
        onNavigateToPasswordManagement = onNavigateToPasswordManagement,
        onLogoutClick = { viewModel.onEvent(AppSettingsEvent.LogoutClicked) },
        onExportLogsClick = {
            coroutineScope.launch {
                LogExporter.exportLogsAndShare(context)
            }
        }
    )
}

// --- 2. STATELESS CONTENT ---
@Composable
fun ApplicationSettingsContent(
    state: AppSettingsState,
    onNavigateToPasswordManagement: () -> Unit,
    onLogoutClick: () -> Unit,
    onExportLogsClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    // Gradients from HTML
    val headerGradient = Brush.linearGradient(
        colors = listOf(colors.primary, colors.primaryVariant)
    )
    val avatarGradient = Brush.linearGradient(
        colors = listOf(colors.yellow, colors.orange)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // --- 1. PROFILE HEADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = headerGradient,
                        shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                    )
                    .padding(top = 40.dp, bottom = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(avatarGradient, CircleShape)
                            .border(4.dp, Color.White.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = AppIcons.Profile, // Use a user/profile icon
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "والد گرامی", // Or fetch from state if you add it
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.parentPhoneNumber,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 1.sp
                    )
                }
            }

            // --- 2. MENU CONTENT ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    // Negative offset to pull the menu up slightly over the header curve like HTML
                    .padding(top = 16.dp, bottom = 100.dp)
            ) {

                // Theme Switcher Card
                ThemeSwitcherCard()

                Spacer(modifier = Modifier.height(8.dp))

                // Settings Menu
                ProfileMenuItemV2(
                    icon = AppIcons.LockBadge, // Use settings/lock icon
                    iconBgColor = colors.blue.copy(alpha = if (isDark) 0.15f else 0.1f),
                    iconTintColor = colors.blue,
                    title = "تنظیمات رمز عبور",
                    onClick = onNavigateToPasswordManagement
                )

                ProfileMenuItemV2(
                    icon = AppIcons.Notification, // Use bell icon
                    iconBgColor = colors.green.copy(alpha = if (isDark) 0.15f else 0.1f),
                    iconTintColor = colors.green,
                    title = "اعلان‌ها",
                    onClick = { /* TODO */ }
                )

                ProfileMenuItemV2(
                    icon = AppIcons.ShieldCheck, // Use shield icon
                    iconBgColor = colors.primary.copy(alpha = if (isDark) 0.15f else 0.1f),
                    iconTintColor = colors.primary,
                    title = "حریم خصوصی",
                    onClick = { /* TODO */ }
                )

                ProfileMenuItemV2(
                    icon = AppIcons.Info, // Use question mark/info icon
                    iconBgColor = colors.orange.copy(alpha = if (isDark) 0.15f else 0.1f),
                    iconTintColor = colors.orange,
                    title = "راهنما و پشتیبانی",
                    onClick = { /* TODO */ }
                )

                // Preserved Debug/Log Export functionality
                ProfileMenuItemV2(
                    icon = AppIcons.ChartBar, // Use bug/chart icon
                    iconBgColor = colors.textSecondary.copy(alpha = if (isDark) 0.2f else 0.1f),
                    iconTintColor = colors.textSecondary,
                    title = "ارسال گزارش سیستم (Debug)",
                    onClick = onExportLogsClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Logout Button
                ProfileMenuItemV2(
                    icon = AppIcons.Logout, // Use door/logout icon
                    iconBgColor = colors.redLight,
                    iconTintColor = colors.red,
                    title = "خروج از حساب کاربری",
                    titleColor = colors.red,
                    modifier = Modifier.border(2.dp, colors.redLight, RoundedCornerShape(12.dp)),
                    onClick = onLogoutClick
                )
            }
        }
    }
}

@Composable
fun ProfileMenuItemV2(
    icon: Painter,
    iconBgColor: Color,
    iconTintColor: Color,
    title: String,
    titleColor: Color = LocalCustomColors.current.textPrimary,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .shadow(
                elevation = if (isDark) 2.dp else 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .background(colors.surface, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBgColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconTintColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = titleColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = AppIcons.ChevronLeft,
            contentDescription = "Go",
            tint = colors.textHint,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun ThemeSwitcherCard() {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()

    // Note: Local UI state for visual demonstration.
    // To make this functional, state must be hoisted to MainActivity.
    var selectedTheme by remember { mutableStateOf("system") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 2.dp else 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = Color.Black.copy(alpha = 0.04f)
            )
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(colors.primary, CircleShape)
                    .border(2.dp, colors.primary.copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ظاهر برنامه",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cardInnerBG, RoundedCornerShape(12.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ThemeOptionButton(
                title = "روشن",
                icon = AppIcons.Sun, // Use Sun icon
                isSelected = selectedTheme == "light",
                onClick = { selectedTheme = "light" },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionButton(
                title = "تیره",
                icon = AppIcons.Moon, // Use Moon icon
                isSelected = selectedTheme == "dark",
                onClick = { selectedTheme = "dark" },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionButton(
                title = "سیستم",
                icon = AppIcons.Smartphone, // Use Phone/System icon
                isSelected = selectedTheme == "system",
                onClick = { selectedTheme = "system" },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun ThemeOptionButton(
    title: String,
    icon: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) colors.primary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (isSelected) colors.textOnPrimaryVariant else colors.textHint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                color = if (isSelected) colors.textOnPrimaryVariant else colors.textHint,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, locale = "fa", name = "Application Settings V2 (Light)")
@Composable
fun ApplicationSettingsScreenPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        ApplicationSettingsContent(
            state = AppSettingsState("9368630582"),
            onNavigateToPasswordManagement = {},
            onExportLogsClick = {},
            onLogoutClick = {})
    }
}

@Preview(showBackground = true, locale = "fa", name = "Application Settings V2 (Dark)")
@Composable
fun ApplicationSettingsScreenPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        ApplicationSettingsContent(
            state = AppSettingsState("9368630582"),
            onNavigateToPasswordManagement = {},
            onExportLogsClick = {},
            onLogoutClick = {})
    }
}