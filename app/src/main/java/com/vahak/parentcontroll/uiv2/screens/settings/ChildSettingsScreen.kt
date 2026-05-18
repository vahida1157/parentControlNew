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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.Gender
import com.vahak.parentcontroll.core.data.local.entity.GlobalSettingsEntity
import com.vahak.parentcontroll.presentation.setting.ChildSettingsEffect
import com.vahak.parentcontroll.presentation.setting.ChildSettingsEvent
import com.vahak.parentcontroll.presentation.setting.ChildSettingsState
import com.vahak.parentcontroll.presentation.setting.ChildSettingsViewModel
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme
import java.time.LocalDate
import java.time.LocalTime

// Helper Functions
private fun formatTimeLimit(isActive: Boolean, mins: Int): String {
    if (!isActive) return "غیرفعال" // "Inactive"
    val h = mins / 60
    val m = mins % 60
    return buildString {
        if (h > 0) append("$h ساعت ")
        if (m > 0) append("$m دقیقه")
    }.trim().ifEmpty { "نامحدود" } // "Unlimited"
}

private fun formatSleepTime(isActive: Boolean, start: LocalTime, end: LocalTime): String {
    if (!isActive) return "غیرفعال" // "Inactive"
    return String.format("%02d:%02d - %02d:%02d", start.hour, start.minute, end.hour, end.minute)
}

@Composable
fun ChildSettingsScreen(
    viewModel: ChildSettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onNavigateToFeature: (String) -> Unit,
    onInterceptForPermissions: (String, List<String>) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChildSettingsEffect.NavigateBack -> onBackClick()
                is ChildSettingsEffect.NavigateToFeature -> onNavigateToFeature(effect.route)
                is ChildSettingsEffect.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()

                is ChildSettingsEffect.NavigateToPermissionSlider -> onInterceptForPermissions(
                    effect.route,
                    effect.missingPermissions
                )
            }
        }
    }

    ChildSettingsContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSettingsContent(
    state: ChildSettingsState,
    onEvent: (ChildSettingsEvent) -> Unit
) {
    val colors = LocalCustomColors.current
    val localContext = LocalContext.current

    // Coming soon badge colors
    val soonBg = Color(0xFFFCE4EC)
    val soonText = Color(0xFFC2185B)

    // Safeguard to prevent crashing if db is empty
    if (state.isLoading || state.activeChild == null || state.settings == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = colors.primary)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        ChildSettingsHeaderV2(
            child = state.activeChild,
            onBackClick = { onEvent(ChildSettingsEvent.BackClicked) },
            onChangeChildClick = { onEvent(ChildSettingsEvent.OpenChildSheet) }
        )

        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)) {

            // SECTION: Time Management
            SectionTitleV2("مدیریت زمان")

            val isTimeLimitActive = state.settings.isTimeLimitActive
            ChildSettingsRowItemV2(
                title = "قفل زمان روزانه",
                desc = "سقف زمان استفاده در شبانه‌روز",
                iconEmoji = "⏰", iconBg = Color(0xFFE3F2FD),
                valueBadge = formatTimeLimit(isTimeLimitActive, state.settings.dailyTimeLimitMins),
                isInactive = !isTimeLimitActive, // Passes inactive state for gray styling
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "time_limit",
                            localContext
                        )
                    )
                }
            )

            val isSleepTimeActive = state.settings.isSleepTimeActive
            ChildSettingsRowItemV2(
                title = "حالت خواب",
                desc = "قفل خودکار شبانه",
                iconEmoji = "🌙", iconBg = Color(0xFFFCE4EC),
                valueBadge = formatSleepTime(
                    isSleepTimeActive,
                    state.settings.sleepTimeStart,
                    state.settings.sleepTimeEnd
                ),
                isInactive = !isSleepTimeActive, // Passes inactive state for gray styling
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "sleep_time",
                            localContext
                        )
                    )
                }
            )

            // SECTION: Internet & Apps
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2("کنترل برنامه و اینترنت")

            val allowedCount = state.allowedAppsCount
            val appLockBadgeText = if (allowedCount == 0) "همه غیرمجاز" else "$allowedCount برنامه مجاز"
            val isAppLockInactive = allowedCount == 0
            ChildSettingsRowItemV2(
                title = "قفل برنامه‌ها",
                desc = "مدیریت دسترسی به اپلیکیشن‌ها",
                iconEmoji = "🔒", iconBg = Color(0xFFE8F5E9),
                valueBadge = appLockBadgeText,
                isInactive = isAppLockInactive,
                onClick = { onEvent(ChildSettingsEvent.GridItemClicked("app_lock", localContext)) }
            )

            ChildSettingsRowItemV2(
                title = "فیلتر سایت‌ها",
                desc = "مدیریت لیست سایت‌های مسدود/مجاز",
                iconEmoji = "🌐", iconBg = Color(0xFFE3F2FD),
                valueBadge = "به زودی", badgeBgColor = soonBg, badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "site_management",
                            localContext
                        )
                    )
                }
            )
            ChildSettingsRowItemV2(
                title = "جستجوی ایمن",
                desc = "فیلتر نتایج جستجوی نامناسب",
                iconEmoji = "🔍", iconBg = Color(0xFFE8F5E9),
                valueBadge = "به زودی", badgeBgColor = soonBg, badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "safe_search",
                            localContext
                        )
                    )
                }
            )
            ChildSettingsRowItemV2(
                title = "مسدود تبلیغات",
                desc = "حذف تبلیغات در وب و برنامه‌ها",
                iconEmoji = "🚫", iconBg = Color(0xFFFFF3E0),
                valueBadge = "به زودی", badgeBgColor = soonBg, badgeTextColor = soonText,
                onClick = { onEvent(ChildSettingsEvent.HelpClicked) }
            )

            // SECTION: Allowed Content
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2("محتوای مجاز")
            ChildSettingsRowItemV2(
                title = "فیلم و انیمیشن",
                desc = "کتابخانه محتوای امن متناسب با سن",
                iconEmoji = "🎬", iconBg = Color(0xFFFFF3E0),
                valueBadge = "به زودی", badgeBgColor = soonBg, badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "content_movies",
                            localContext
                        )
                    )
                }
            )

            // SECTION: Health & Safety
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2("سلامت و امنیت")
            ChildSettingsRowItemV2(
                title = "محافظ چشم",
                desc = "فیلتر نور آبی و یادآور استراحت",
                iconEmoji = "👁️", iconBg = Color(0xFFF3E5F5),
                valueBadge = "به زودی", badgeBgColor = soonBg, badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "eye_protect",
                            localContext
                        )
                    )
                }
            )
            ChildSettingsRowItemV2(
                title = "موقعیت مکانی",
                desc = "ردیابی لحظه‌ای و محدوده امن",
                iconEmoji = "📍", iconBg = Color(0xFFE0F7FA),
                valueBadge = "به زودی", badgeBgColor = soonBg, badgeTextColor = soonText,
                onClick = { onEvent(ChildSettingsEvent.GridItemClicked("location", localContext)) }
            )
            ChildSettingsRowItemV2(
                title = "جلوگیری از حذف",
                desc = "قفل امنیتی برای حذف برنامه‌",
                iconEmoji = "🛡️", iconBg = Color(0xFFFCE4EC),
                valueBadge = "به زودی", badgeBgColor = soonBg, badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "prevent_delete",
                            localContext
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- CHILD SELECTOR BOTTOM SHEET ---
    if (state.isChildSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(ChildSettingsEvent.CloseChildSheet) },
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "انتخاب فرزند",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                state.allChildren.forEach { child ->
                    val isSelected = child.id == state.activeChild.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onEvent(ChildSettingsEvent.SelectChild(child.id)) }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(colors.cardInnerBG, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (child.gender == Gender.BOY) "👦" else "👧", fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = child.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.primary else colors.textPrimary
                            )
                        }
                        if (isSelected) {
                            Icon(AppIcons.Check, contentDescription = null, tint = colors.primary)
                        }
                    }
                    if (!isSelected) {
                        HorizontalDivider(
                            color = colors.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

// ----------------------------------------------------------------------------
// EXTRACTED UI COMPONENTS
// ----------------------------------------------------------------------------

@Composable
fun ChildSettingsHeaderV2(
    child: ChildEntity,
    onBackClick: () -> Unit,
    onChangeChildClick: () -> Unit
) {
    val colors = LocalCustomColors.current
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
                    Text(
                        "پیکربندی محافظت",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    Text(
                        "⚙️ تنظیمات کامل",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
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
                    Icon(
                        AppIcons.Back,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .clickable { onChangeChildClick() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (child.gender == Gender.BOY) "👦" else "👧", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "تنظیمات اعمال می‌شود برای",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    )
                    Text(
                        child.name,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "تغییر فرزند",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
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
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(colors.primary, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = colors.textSecondary
        )
    }
}

@Composable
fun ChildSettingsRowItemV2(
    title: String,
    desc: String,
    iconEmoji: String,
    iconBg: Color,
    valueBadge: String,
    isInactive: Boolean = false,
    badgeBgColor: Color? = null,
    badgeTextColor: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val isDark = isSystemInDarkTheme()

    // Determine badge colors based on inactive state or provided overrides
    val finalBadgeBg = when {
        badgeBgColor != null -> badgeBgColor
        isInactive -> colors.divider.copy(alpha = 0.5f) // Gray background if inactive
        else -> colors.primary.copy(alpha = 0.08f)
    }

    val finalBadgeText = when {
        badgeTextColor != null -> badgeTextColor
        isInactive -> colors.textSecondary // Gray text if inactive
        else -> colors.primary
    }

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
                Text(iconEmoji, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(desc, fontSize = 11.sp, color = colors.textSecondary, maxLines = 1)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(finalBadgeBg, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    valueBadge,
                    color = finalBadgeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                AppIcons.ChevronLeft,
                contentDescription = null,
                tint = colors.textHint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private val mockChild = ChildEntity(
    id = "mock-123",
    name = "علی",
    dob = LocalDate.now().minusYears(10), // Makes the child 10 years old
    gender = Gender.BOY
)

private val mockChildrenList = listOf(
    mockChild,
    ChildEntity(
        id = "mock-456",
        name = "سارا",
        dob = LocalDate.now().minusYears(7),
        gender = Gender.GIRL
    )
)

@Preview(showBackground = true, name = "1. Child Settings Light", locale = "fa")
@Composable
fun ChildSettingsPreviewLightV2() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        ChildSettingsContent(
            state = ChildSettingsState(
                activeChild = mockChild,
                allChildren = mockChildrenList,
                settings = GlobalSettingsEntity(
                    childId = "mock-123",
                    isTimeLimitActive = true,
                    dailyTimeLimitMins = 180,
                    isSleepTimeActive = true
                ),
                allowedAppsCount = 5,
                isLoading = false,
                isChildSheetOpen = false
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Child Settings Dark", locale = "fa")
@Composable
fun ChildSettingsPreviewDarkV2() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        ChildSettingsContent(
            state = ChildSettingsState(
                activeChild = mockChild,
                allChildren = mockChildrenList,
                settings = GlobalSettingsEntity(
                    childId = "mock-123",
                    isTimeLimitActive = false,
                    isSleepTimeActive = false
                ),
                allowedAppsCount = 0,
                isLoading = false,
                isChildSheetOpen = false
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "3. Child Settings - Change Child Sheet", locale = "fa")
@Composable
fun ChildSettingsPreviewSheetOpenV2() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        ChildSettingsContent(
            state = ChildSettingsState(
                activeChild = mockChild,
                allChildren = mockChildrenList,
                settings = GlobalSettingsEntity(childId = "mock-123"),
                isLoading = false,
                isChildSheetOpen = true // Forces the bottom sheet to show in the preview
            ),
            onEvent = {}
        )
    }
}