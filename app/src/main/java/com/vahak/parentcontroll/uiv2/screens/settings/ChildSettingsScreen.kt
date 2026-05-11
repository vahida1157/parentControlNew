package com.vahak.parentcontroll.uiv2.screens.settings

import android.widget.Toast
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import com.vahak.parentcontroll.presentation.setting.SettingsEffect
import com.vahak.parentcontroll.presentation.setting.SettingsEvent
import com.vahak.parentcontroll.presentation.setting.SettingsState
import com.vahak.parentcontroll.presentation.setting.SettingsViewModel
import com.vahak.parentcontroll.uiv2.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme

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
                is SettingsEffect.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                is SettingsEffect.NavigateToPermissionSlider -> {
                    onInterceptForPermissions(effect.route, effect.missingPermissions)
                }
            }
        }
    }

    ChildSettingsContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun ChildSettingsContent(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit
) {
    val colors = LocalCustomColors.current
    val localContext = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- 1. Header & Target Child Card ---
        ChildSettingsHeaderV2(
            childName = "فرزند شما", // Defaulting as ViewModel currently only has childId
            onBackClick = { onEvent(SettingsEvent.BackClicked) },
            onChangeChildClick = { onEvent(SettingsEvent.BackClicked) } // Route back to family
        )

        // --- 2. Scrollable Settings List ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            // SECTION: Time Management
            SectionTitleV2("مدیریت زمان")
            SettingsRowItemV2(
                title = "قفل زمان روزانه",
                desc = "سقف زمان استفاده در شبانه‌روز",
                iconEmoji = "⏰",
                iconBg = Color(0xFFE3F2FD),
                valueBadge = "۳ ساعت",
                onClick = { onEvent(SettingsEvent.GridItemClicked("time_limit", localContext)) }
            )
            SettingsRowItemV2(
                title = "حالت خواب",
                desc = "قفل خودکار شبانه",
                iconEmoji = "🌙",
                iconBg = Color(0xFFFCE4EC),
                valueBadge = "۲۱:۳۰ - ۰۷:۰۰",
                onClick = { onEvent(SettingsEvent.GridItemClicked("sleep_time", localContext)) }
            )

            // SECTION: Internet & Apps
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2("کنترل برنامه و اینترنت")
            SettingsRowItemV2(
                title = "قفل برنامه‌ها",
                desc = "مدیریت دسترسی به اپلیکیشن‌ها",
                iconEmoji = "🔒",
                iconBg = Color(0xFFE8F5E9),
                valueBadge = "۵ مسدود",
                onClick = { onEvent(SettingsEvent.GridItemClicked("app_lock", localContext)) }
            )
            SettingsRowItemV2(
                title = "فیلتر سایت‌ها",
                desc = "مدیریت لیست سایت‌های مسدود/مجاز",
                iconEmoji = "🌐",
                iconBg = Color(0xFFE3F2FD),
                valueBadge = "۱۲ مورد",
                onClick = { onEvent(SettingsEvent.GridItemClicked("site_management", localContext)) }
            )
            SettingsRowItemV2(
                title = "جستجوی ایمن",
                desc = "فیلتر نتایج جستجوی نامناسب",
                iconEmoji = "🔍",
                iconBg = Color(0xFFE8F5E9),
                valueBadge = "فعال",
                onClick = { onEvent(SettingsEvent.GridItemClicked("safe_search", localContext)) }
            )
            SettingsRowItemV2(
                title = "مسدود تبلیغات",
                desc = "حذف تبلیغات در وب و برنامه‌ها",
                iconEmoji = "🚫",
                iconBg = Color(0xFFFFF3E0),
                valueBadge = "پیشرفته",
                onClick = { onEvent(SettingsEvent.HelpClicked) } // Hook up later
            )

            // SECTION: Allowed Content
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2("محتوای مجاز")
            SettingsRowItemV2(
                title = "فیلم و انیمیشن",
                desc = "کتابخانه محتوای امن متناسب با سن",
                iconEmoji = "🎬",
                iconBg = Color(0xFFFFF3E0),
                valueBadge = "+۵۰",
                onClick = { onEvent(SettingsEvent.GridItemClicked("content_movies", localContext)) }
            )

            // SECTION: Health & Safety
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2("سلامت و امنیت")
            SettingsRowItemV2(
                title = "محافظ چشم",
                desc = "فیلتر نور آبی و یادآور استراحت",
                iconEmoji = "👁️",
                iconBg = Color(0xFFF3E5F5),
                valueBadge = "۵۰٪",
                onClick = { onEvent(SettingsEvent.GridItemClicked("eye_protect", localContext)) }
            )
            SettingsRowItemV2(
                title = "موقعیت مکانی",
                desc = "ردیابی لحظه‌ای و محدوده امن",
                iconEmoji = "📍",
                iconBg = Color(0xFFE0F7FA),
                valueBadge = "فعال",
                onClick = { onEvent(SettingsEvent.GridItemClicked("location", localContext)) }
            )
            SettingsRowItemV2(
                title = "جلوگیری از حذف",
                desc = "قفل امنیتی برای حذف فرزندبان",
                iconEmoji = "🛡️",
                iconBg = Color(0xFFFCE4EC),
                valueBadge = "فعال",
                onClick = { onEvent(SettingsEvent.GridItemClicked("prevent_delete", localContext)) }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ----------------------------------------------------------------------------
// EXTRACTED UI COMPONENTS
// ----------------------------------------------------------------------------

@Composable
fun ChildSettingsHeaderV2(
    childName: String,
    onBackClick: () -> Unit,
    onChangeChildClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()
    val headerGradient = Brush.linearGradient(listOf(colors.primary, colors.primaryVariant))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(top = 40.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("پیکربندی محافظت", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text("⚙️ تنظیمات کامل", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(AppIcons.Back, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Target Child Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .clickable { onChangeChildClick() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Temporary Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👦", fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text("تنظیمات اعمال می‌شود برای", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text(childName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text("تغییر فرزند", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SectionTitleV2(title: String) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(colors.primary, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.textSecondary)
    }
}

@Composable
fun SettingsRowItemV2(
    title: String,
    desc: String,
    iconEmoji: String,
    iconBg: Color,
    valueBadge: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(if (isDark) 1.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconBg, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 18.sp) // Emojis used to exactly match HTML
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1)
            }

            if (valueBadge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(valueBadge, color = colors.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(AppIcons.ChevronLeft, contentDescription = null, tint = colors.textHint, modifier = Modifier.size(16.dp))
        }
    }
}

// ----------------------------------------------------------------------------
// PREVIEWS
// ----------------------------------------------------------------------------

@Preview(showBackground = true, name = "1. Child Settings Light", locale = "fa")
@Composable
fun ChildSettingsPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        ChildSettingsContent(
            state = SettingsState(
                childId = "mock-123",
                settings = GlobalSettingsEntity(childId = "mock-123", isChildThemeActive = true),
                isLoading = false
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Child Settings Dark", locale = "fa")
@Composable
fun ChildSettingsPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        ChildSettingsContent(
            state = SettingsState(
                childId = "mock-123",
                settings = GlobalSettingsEntity(childId = "mock-123", isChildThemeActive = true),
                isLoading = false
            ),
            onEvent = {}
        )
    }
}