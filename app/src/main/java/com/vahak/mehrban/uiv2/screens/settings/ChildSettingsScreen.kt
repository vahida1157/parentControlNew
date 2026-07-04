package com.vahak.mehrban.uiv2.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.core.data.local.entity.GlobalSettingsEntity
import com.vahak.mehrban.presentation.setting.ChildSettingsEffect
import com.vahak.mehrban.presentation.setting.ChildSettingsEvent
import com.vahak.mehrban.presentation.setting.ChildSettingsState
import com.vahak.mehrban.presentation.setting.ChildSettingsViewModel
import com.vahak.mehrban.presentation.setting.FeatureToastType.*
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.uiv2.components.header.MehrbanChildSelectionHeader
import com.vahak.mehrban.uiv2.navigation.Screen
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import java.time.LocalDate
import java.time.LocalTime

// --- Helper composables for format strings ---

@Composable
private fun formatTimeLimit(isActive: Boolean, mins: Int): String {
    if (!isActive) return stringResource(R.string.child_settings_inactive)
    val h = mins / 60
    val m = mins % 60
    val hourLabel = stringResource(R.string.hour)   // "ساعت" or "h"
    val minLabel = stringResource(R.string.minute)  // "دقیقه" or "m"
    val text = buildString {
        if (h > 0) append("$h $hourLabel ")
        if (m > 0) append("$m $minLabel")
    }.trim()
    return text.ifEmpty { stringResource(R.string.unlimited) }
}

@Composable
private fun formatSleepTime(isActive: Boolean, start: LocalTime, end: LocalTime): String {
    if (!isActive) return stringResource(R.string.child_settings_inactive)
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

    val featureUnderDevelopmentMessage = stringResource(R.string.feature_under_development)
    val featureComingSoonMessage = stringResource(R.string.feature_coming_soon)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChildSettingsEffect.NavigateBack -> onBackClick()
                is ChildSettingsEffect.NavigateToFeature -> onNavigateToFeature(effect.route)
                is ChildSettingsEffect.ShowToast -> {
                    val message = when(effect.type) {
                        UNDER_DEVELOPMENT -> featureUnderDevelopmentMessage
                        COMING_SOON -> featureComingSoonMessage
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }

                is ChildSettingsEffect.NavigateToPermissionSlider -> onInterceptForPermissions(
                    effect.route, effect.missingPermissions
                )
            }
        }
    }

    ChildSettingsContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildSettingsContent(
    state: ChildSettingsState, onEvent: (ChildSettingsEvent) -> Unit
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
        MehrbanChildSelectionHeader(
            title = stringResource(R.string.child_settings_header_title),
            subtitle = stringResource(R.string.child_settings_header_subtitle),
            childName = state.activeChild.name,
            childGender = state.activeChild.gender,
            changeButtonText = stringResource(R.string.change),
            onBackClick = { onEvent(ChildSettingsEvent.BackClicked) },
            onChangeChildClick = { onEvent(ChildSettingsEvent.OpenChildSheet) })

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            // SECTION: Time Management
            SectionTitleV2(stringResource(R.string.child_settings_section_time))

            val isTimeLimitActive = state.settings.isTimeLimitActive
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_daily_time_lock),
                desc = stringResource(R.string.child_settings_daily_time_lock_desc),
                iconEmoji = "⏰",
                iconBg = Color(0xFFE3F2FD),
                valueBadge = formatTimeLimit(isTimeLimitActive, state.settings.dailyTimeLimitMins),
                isInactive = !isTimeLimitActive,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "time_limit", localContext
                        )
                    )
                })

            val isSleepTimeActive = state.settings.isSleepTimeActive
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_sleep_mode),
                desc = stringResource(R.string.child_settings_sleep_mode_desc),
                iconEmoji = "🌙",
                iconBg = Color(0xFFFCE4EC),
                valueBadge = formatSleepTime(
                    isSleepTimeActive, state.settings.sleepTimeStart, state.settings.sleepTimeEnd
                ),
                isInactive = !isSleepTimeActive,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "sleep_time", localContext
                        )
                    )
                })

            // SECTION: Internet & Apps
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2(stringResource(R.string.child_settings_section_apps))

            val allowedCount = state.allowedAppsCount
            val appLockBadgeText =
                if (allowedCount == 0) stringResource(R.string.child_settings_all_blocked)
                else stringResource(R.string.child_settings_apps_allowed, allowedCount)
            val isAppLockInactive = allowedCount == 0
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_app_lock),
                desc = stringResource(R.string.child_settings_app_lock_desc),
                iconEmoji = "🔒",
                iconBg = Color(0xFFE8F5E9),
                valueBadge = appLockBadgeText,
                isInactive = isAppLockInactive,
                onClick = { onEvent(ChildSettingsEvent.GridItemClicked("app_lock", localContext)) })

            ChildSettingsRowItemV2(
                title = stringResource(R.string.browser_settings_title),
                desc = stringResource(R.string.browser_settings_subtitle),
                iconEmoji = "🌐",
                iconBg = Color(0xFFE3F2FD),
                valueBadge = stringResource(R.string.active), // "فعال"
                isInactive = false, // It's fully functional now!
                onClick = {
                    // This route name exactly matches what we wrote in Screen.kt
                    onEvent(ChildSettingsEvent.GridItemClicked(Screen.BrowserSettingMenu.route, localContext))
                }
            )

//            ChildSettingsRowItemV2(
//                title = stringResource(R.string.child_settings_site_filter),
//                desc = stringResource(R.string.child_settings_site_filter_desc),
//                iconEmoji = "🌐",
//                iconBg = Color(0xFFE3F2FD),
//                valueBadge = stringResource(R.string.coming_soon),
//                badgeBgColor = soonBg,
//                badgeTextColor = soonText,
//                onClick = {
//                    onEvent(
//                        ChildSettingsEvent.GridItemClicked(
//                            "site_management", localContext
//                        )
//                    )
//                })
//            ChildSettingsRowItemV2(
//                title = stringResource(R.string.child_settings_safe_search),
//                desc = stringResource(R.string.child_settings_safe_search_desc),
//                iconEmoji = "🔍",
//                iconBg = Color(0xFFE8F5E9),
//                valueBadge = stringResource(R.string.coming_soon),
//                badgeBgColor = soonBg,
//                badgeTextColor = soonText,
//                onClick = {
//                    onEvent(
//                        ChildSettingsEvent.GridItemClicked(
//                            "safe_search", localContext
//                        )
//                    )
//                })
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_block_ads),
                desc = stringResource(R.string.child_settings_block_ads_desc),
                iconEmoji = "🚫",
                iconBg = Color(0xFFFFF3E0),
                valueBadge = stringResource(R.string.coming_soon),
                badgeBgColor = soonBg,
                badgeTextColor = soonText,
                onClick = { onEvent(ChildSettingsEvent.HelpClicked) })

            // SECTION: Allowed Content
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2(stringResource(R.string.child_settings_section_content))
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_movies),
                desc = stringResource(R.string.child_settings_movies_desc),
                iconEmoji = "🎬",
                iconBg = Color(0xFFFFF3E0),
                valueBadge = stringResource(R.string.coming_soon),
                badgeBgColor = soonBg,
                badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "content_movies", localContext
                        )
                    )
                })

            // SECTION: Health & Safety
            Spacer(modifier = Modifier.height(16.dp))
            SectionTitleV2(stringResource(R.string.child_settings_section_health))
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_eye_protection),
                desc = stringResource(R.string.child_settings_eye_protection_desc),
                iconEmoji = "👁️",
                iconBg = Color(0xFFF3E5F5),
                valueBadge = stringResource(R.string.coming_soon),
                badgeBgColor = soonBg,
                badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "eye_protect", localContext
                        )
                    )
                })
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_location),
                desc = stringResource(R.string.child_settings_location_desc),
                iconEmoji = "📍",
                iconBg = Color(0xFFE0F7FA),
                valueBadge = stringResource(R.string.coming_soon),
                badgeBgColor = soonBg,
                badgeTextColor = soonText,
                onClick = { onEvent(ChildSettingsEvent.GridItemClicked("location", localContext)) })
            ChildSettingsRowItemV2(
                title = stringResource(R.string.child_settings_prevent_delete),
                desc = stringResource(R.string.child_settings_prevent_delete_desc),
                iconEmoji = "🛡️",
                iconBg = Color(0xFFFCE4EC),
                valueBadge = stringResource(R.string.coming_soon),
                badgeBgColor = soonBg,
                badgeTextColor = soonText,
                onClick = {
                    onEvent(
                        ChildSettingsEvent.GridItemClicked(
                            "prevent_delete", localContext
                        )
                    )
                })
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
                    stringResource(R.string.select_child),
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
                        verticalAlignment = Alignment.CenterVertically) {
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

    val finalBadgeBg = when {
        badgeBgColor != null -> badgeBgColor
        isInactive -> colors.divider.copy(alpha = 0.5f)
        else -> colors.primary.copy(alpha = 0.08f)
    }

    val finalBadgeText = when {
        badgeTextColor != null -> badgeTextColor
        isInactive -> colors.textSecondary
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
    id = "mock-123", name = "علی", dob = LocalDate.now().minusYears(10), gender = Gender.BOY
)

private val mockChildrenList = listOf(
    mockChild, ChildEntity(
        id = "mock-456", name = "سارا", dob = LocalDate.now().minusYears(7), gender = Gender.GIRL
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
            ), onEvent = {})
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
                    childId = "mock-123", isTimeLimitActive = false, isSleepTimeActive = false
                ),
                allowedAppsCount = 0,
                isLoading = false,
                isChildSheetOpen = false
            ), onEvent = {})
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
                isChildSheetOpen = true
            ), onEvent = {})
    }
}