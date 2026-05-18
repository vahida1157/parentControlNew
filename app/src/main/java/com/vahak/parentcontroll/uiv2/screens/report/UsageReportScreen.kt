package com.vahak.parentcontroll.uiv2.screens.report

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.core.data.local.entity.ChildEntity
import com.vahak.parentcontroll.core.data.local.entity.Gender
import com.vahak.parentcontroll.presentation.report.AppUsageUi
import com.vahak.parentcontroll.presentation.report.UsageReportEffect
import com.vahak.parentcontroll.presentation.report.UsageReportEvent
import com.vahak.parentcontroll.presentation.report.UsageReportState
import com.vahak.parentcontroll.presentation.report.UsageReportViewModel
import com.vahak.parentcontroll.uiv2.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme
import java.time.LocalDate

@Composable
fun UsageReportScreen(
    viewModel: UsageReportViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is UsageReportEffect.NavigateBack) {
                onBackClick()
            }
        }
    }

    UsageReportContent(state = state, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageReportContent(state: UsageReportState, onEvent: (UsageReportEvent) -> Unit) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
    ) {
        ReportHeader(
            child = state.activeChild,
            onBackClick = { onEvent(UsageReportEvent.BackClicked) },
            onChangeChildClick = { onEvent(UsageReportEvent.OpenChildSheet) } // FIXED: Wired up!
        )

        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {

            val hours = state.totalSecondsToday / 3600
            val mins = (state.totalSecondsToday % 3600) / 60

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                colors.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏱️", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = String.format("%d:%02d", hours, mins),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textPrimary
                        )
                        Text("زمان استفاده امروز", fontSize = 12.sp, color = colors.textSecondary)
                    }
                }
            }

            WeeklyUsageChart(state.weeklyUsageSeconds, state.averageSeconds)

            // --- THE RESTORED BLUE LINK SECTION ---
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(14.dp)
                            .background(colors.primary, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "پرکاربردترین برنامه‌ها",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.textSecondary
                    )
                }

                if (state.appUsages.size > 3) {
                    TextButton(
                        onClick = { onEvent(UsageReportEvent.ToggleShowAllApps) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text(
                            if (state.showAllApps) "بستن ↑" else "جزئیات ←",
                            color = colors.primary, // The Blue/Primary Link from HTML
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.animateContentSize()) {
                val appsToShow = if (state.showAllApps) state.appUsages else state.appUsages.take(3)

                if (appsToShow.isEmpty() && !state.isLoading) {
                    Text(
                        "هیچ برنامه‌ای امروز استفاده نشده است.",
                        color = colors.textHint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    val maxUsage =
                        state.appUsages.maxOfOrNull { it.usedSeconds }?.toFloat()?.coerceAtLeast(1f)
                            ?: 1f
                    appsToShow.forEach { app -> AppUsageRow(app, maxUsage) }
                }
            }

            // --- THE RESTORED INSIGHT CARD ---
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primary.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .border(1.dp, colors.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("💡", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "نکته این هفته",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = colors.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        "زمان استفاده روزانه در حد تعادل است. این روند به ترشح طبیعی دوپامین و حفظ تمرکز کمک می‌کند.",
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = colors.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- RESTORED CHILD SELECTOR BOTTOM SHEET ---
    if (state.isChildSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { onEvent(UsageReportEvent.CloseChildSheet) },
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
                    val isSelected = child.id == state.activeChild?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { onEvent(UsageReportEvent.SelectChild(child.id)) }
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
                                child.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.primary else colors.textPrimary
                            )
                        }
                        if (isSelected) Icon(
                            AppIcons.Check,
                            contentDescription = null,
                            tint = colors.primary
                        )
                    }
                    if (!isSelected) HorizontalDivider(
                        color = colors.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun ReportHeader(child: ChildEntity?, onBackClick: () -> Unit, onChangeChildClick: () -> Unit) {
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
                    Text("گزارش عملکرد", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                    Text(
                        "📊 آمار استفاده",
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

            // FIXED: Added clickable and Restored the "تغییر فرزند" badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
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
                    Text(if (child?.gender == Gender.BOY) "👦" else "👧", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("گزارش برای", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text(
                        child?.name ?: "...",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
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
fun WeeklyUsageChart(weeklySeconds: List<Int>, averageSeconds: Int) {
    val colors = LocalCustomColors.current
    val labels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    val maxSeconds = weeklySeconds.maxOrNull()?.toFloat()?.coerceAtLeast(1f) ?: 1f

    val avgHours = averageSeconds / 3600
    val avgMins = (averageSeconds % 3600) / 60

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "زمان استفاده هفته (ساعت)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    "میانگین: ${String.format("%d:%02d", avgHours, avgMins)}",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(145.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklySeconds.forEachIndexed { index, seconds ->
                        val hours = seconds / 3600
                        val mins = (seconds % 3600) / 60
                        val heightFraction = (seconds / maxSeconds)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f)
                        ) {
                            if (seconds > 0) {
                                Box(
                                    modifier = Modifier
                                        .background(colors.cardInnerBG, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        String.format("%d:%02d", hours, mins),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = colors.textPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.55f)
                                    .height(100.dp)
                                    .align(Alignment.CenterHorizontally),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(heightFraction.coerceAtLeast(0.06f))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    colors.primaryVariant,
                                                    colors.primary
                                                )
                                            ), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                labels[index],
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageRow(app: AppUsageUi, maxSeconds: Float) {
    val colors = LocalCustomColors.current
    val progress = (app.usedSeconds / maxSeconds).coerceIn(0f, 1f)

    val hours = app.usedSeconds / 3600
    val mins = (app.usedSeconds % 3600) / 60

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(colors.surface, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // --- TOP HALF: The original App Row ---
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(colors.cardInnerBG, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                val safeBitmap = remember(app.icon) {
                    try {
                        app.icon?.toBitmap(144, 144)?.asImageBitmap()
                    } catch (_: Exception) {
                        null
                    }
                }
                if (safeBitmap != null) {
                    Image(
                        bitmap = safeBitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                    )
                } else {
                    Text("📱", fontSize = 20.sp)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.appName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .background(colors.divider, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            colors.primaryVariant,
                                            colors.primary
                                        )
                                    ), RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                String.format("%d:%02d", hours, mins),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = colors.primary
            )
        }

        // 🚀 BOTTOM HALF: The Drill-Down Breakdown (Only shows if there are multiple devices or network data)
        if (app.devices.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = colors.divider.copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Column(modifier = Modifier.padding(start = 52.dp)) { // Aligns text under the app name
                app.devices.forEach { device ->
                    val dHours = device.seconds / 3600
                    val dMins = (device.seconds % 3600) / 60

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("▪", color = colors.textHint, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(device.name, fontSize = 11.sp, color = colors.textSecondary)
                        }
                        Text(
                            String.format("%d:%02d", dHours, dMins),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Usage Report Light", locale = "fa")
@Composable
fun UsageReportPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        UsageReportContent(
            state = UsageReportState(
                activeChild = ChildEntity(
                    id = "1",
                    name = "علی",
                    dob = LocalDate.now(),
                    gender = Gender.BOY
                ),
                totalSecondsToday = 6300, // 1 hour 45 mins
                weeklyUsageSeconds = listOf(
                    3600,
                    7200,
                    5400,
                    9000,
                    1800,
                    0,
                    0
                ), // Realistic week spread
                averageSeconds = 5400, // 1 hour 30 mins average
                appUsages = listOf(
                    AppUsageUi("com.whatsapp", "واتس‌اپ", null, 3600),
                    AppUsageUi("com.instagram", "اینستاگرام", null, 1800),
                    AppUsageUi("com.mojang.minecraftpe", "ماینکرافت", null, 900)
                ),
                isLoading = false,
                showAllApps = false
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Usage Report Dark", locale = "fa")
@Composable
fun UsageReportPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        UsageReportContent(
            state = UsageReportState(
                activeChild = ChildEntity(
                    id = "2",
                    name = "سارا",
                    dob = LocalDate.now(),
                    gender = Gender.GIRL
                ),
                totalSecondsToday = 0,
                weeklyUsageSeconds = listOf(0, 0, 0, 0, 0, 0, 0),
                averageSeconds = 0,
                appUsages = emptyList(),
                isLoading = false,
                isChildSheetOpen = false
            ),
            onEvent = {}
        )
    }
}