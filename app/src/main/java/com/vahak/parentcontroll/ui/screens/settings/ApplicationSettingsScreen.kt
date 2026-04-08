package com.vahak.parentcontroll.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.setting.AppSettingsEffect
import com.vahak.parentcontroll.presentation.setting.AppSettingsEvent
import com.vahak.parentcontroll.presentation.setting.ApplicationSettingsViewModel
import com.vahak.parentcontroll.ui.component.SimpleFlatHeader
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ApplicationSettingsScreen(
    viewModel: ApplicationSettingsViewModel = hiltViewModel(),
    onNavigateToPasswordManagement: () -> Unit,
    onLogoutComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val colors = LocalCustomColors.current
    val scrollState = rememberScrollState()
    
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is AppSettingsEffect.NavigateToLogin) {
                onLogoutComplete()
            }
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        SimpleFlatHeader(
            title = "تنظیمات برنامه", onBackClick = { }) // No back click since it's a root tab

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState)
        ) {

            // --- 1. ACCOUNT INFO SECTION ---
            Text(
                text = "حساب کاربری",
                color = colors.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp, start = 5.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(15.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(colors.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(AppIcons.Profile, contentDescription = null, tint = colors.primary)
                    }
                    Spacer(modifier = Modifier.width(15.dp))
                    Column {
                        Text("شماره موبایل والدین", color = colors.textSecondary, fontSize = 12.sp)
                        Text(
                            state.parentPhoneNumber,
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- 2. SECURITY SECTION ---
            Text(
                text = "امنیت",
                color = colors.textSecondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp, start = 5.dp)
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(15.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    SettingsRowItem(
                        icon = AppIcons.LockBadge,
                        title = "رمز عبور والدین",
                        subtitle = "برای خروج از محیط امن و تنظیمات",
                        onClick = onNavigateToPasswordManagement
                    )
                    // You can add more security things here later (e.g. Fingerprint)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- 3. DANGER ZONE (LOGOUT) ---
            Button(
                onClick = { viewModel.onEvent(AppSettingsEvent.LogoutClicked) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.red.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(15.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
            ) {
                Icon(
                    AppIcons.LockBadge, contentDescription = "Logout", tint = colors.red
                ) // Use logout icon if you have one
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    "خروج از حساب کاربری",
                    color = colors.red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(100.dp)) // Padding for bottom nav bar
        }
    }
}

@Composable
fun SettingsRowItem(
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(15.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.background, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(15.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (subtitle != null) {
                Text(subtitle, color = colors.textSecondary, fontSize = 12.sp)
            }
        }
        Icon(
            AppIcons.ChevronLeft,
            contentDescription = "Go",
            tint = colors.textHint,
            modifier = Modifier.size(16.dp)
        )
    }
}

// ==========================================
// PREVIEW
// ==========================================
@Preview(showBackground = true, locale = "fa")
@Composable
fun ApplicationSettingsScreenPreview() {
    ParentControlTheme {
        ApplicationSettingsScreen(onNavigateToPasswordManagement = {}, onLogoutComplete = {})
    }
}