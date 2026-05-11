package com.vahak.parentcontroll.uiv2.screens.report

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.report.AppUsageUi
import com.vahak.parentcontroll.presentation.report.UsageReportEffect
import com.vahak.parentcontroll.presentation.report.UsageReportEvent
import com.vahak.parentcontroll.presentation.report.UsageReportState
import com.vahak.parentcontroll.presentation.report.UsageReportViewModel
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme

// --- 1. STATEFUL WRAPPER ---
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

    UsageReportContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

// --- 2. STATELESS CONTENT ---
@Composable
fun UsageReportContent(
    state: UsageReportState,
    onEvent: (UsageReportEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // --- 1. Emerald Gradient Header ---
        ReportHeaderV2(
            childName = state.childName,
            onBackClick = { onEvent(UsageReportEvent.BackClicked) }
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.green)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                // --- 2. Grid Stats ---
                item {
                    ReportStatGridV2(totalSeconds = state.totalSecondsToday)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // --- 3. Weekly Chart (Placeholder logic for now) ---
                item {
                    WeeklyChartV2()
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- 4. Top Apps Section ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "پرکاربردترین برنامه‌ها",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "جزئیات ←",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            modifier = Modifier.clickable { /* Show full list */ }
                        )
                    }
                }

                if (state.appUsages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = colors.surface),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("هیچ فعالیتی برای امروز ثبت نشده است.", color = colors.textHint, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    val maxUsage = state.appUsages.maxOfOrNull { it.usedSeconds }?.toFloat() ?: 1f
                    
                    itemsIndexed(state.appUsages) { index, app ->
                        AppUsageItemV2(
                            appUsage = app,
                            maxUsageSeconds = maxUsage
                        )
                        if (index < state.appUsages.lastIndex) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // --- 5. Insights & Export ---
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    InsightCardV2()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { /* Implement PDF Export */ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .border(2.dp, colors.primary, RoundedCornerShape(14.dp)),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("📥 دریافت گزارش کامل (PDF)", color = colors.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
fun ReportHeaderV2(
    childName: String,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val headerGradient = Brush.linearGradient(listOf(colors.green, Color(0xFF0D9488))) // Emerald to Teal

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
                    Text("📊 آمار استفاده", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(42.dp).background(colors.cardInnerBG, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👦", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("گزارش برای", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                    Text(childName, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
                Box(
                    modifier = Modifier.background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 5.dp).clickable { /* Change Child */ }
                ) {
                    Text("تغییر", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReportStatGridV2(totalSeconds: Int) {
    val colors = LocalCustomColors.current
    
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Real Data: Today's Usage
            StatCardV2(
                modifier = Modifier.weight(1f),
                iconEmoji = "⏱️",
                iconBg = colors.primary.copy(alpha = 0.12f),
                value = formatCompactTime(totalSeconds),
                label = "زمان امروز"
            )
            // Mock Data: Alerts
            StatCardV2(
                modifier = Modifier.weight(1f),
                iconEmoji = "⚠️",
                iconBg = colors.red.copy(alpha = 0.12f),
                value = "3",
                label = "هشدار این هفته"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Mock Data: Blocked Sites
            StatCardV2(
                modifier = Modifier.weight(1f),
                iconEmoji = "🚫",
                iconBg = colors.yellow.copy(alpha = 0.15f),
                value = "12",
                label = "سایت مسدود شده"
            )
            // Mock Data: Educational Apps
            StatCardV2(
                modifier = Modifier.weight(1f),
                iconEmoji = "📚",
                iconBg = colors.blue.copy(alpha = 0.12f),
                value = "48%",
                label = "برنامه آموزشی"
            )
        }
    }
}

@Composable
fun StatCardV2(modifier: Modifier = Modifier, iconEmoji: String, iconBg: Color, value: String, label: String) {
    val colors = LocalCustomColors.current
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier.size(32.dp).background(iconBg, RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(iconEmoji, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = colors.textSecondary)
        }
    }
}

@Composable
fun WeeklyChartV2() {
    val colors = LocalCustomColors.current
    // Mock Data representing hours of usage for 7 days
    val weeklyData = listOf(1.5f, 2.0f, 2.5f, 2.2f, 1.8f, 2.5f, 2.16f)
    val maxUsage = weeklyData.maxOrNull() ?: 1f
    val labels = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

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
                Text("زمان استفاده هفتگی (ساعت)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text("میانگین: 2:15", fontSize = 11.sp, color = colors.textSecondary)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth().height(120.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyData.forEachIndexed { index, value ->
                    val heightPercent = (value / maxUsage).coerceAtLeast(0.1f)
                    val isToday = index == 6 // Mocking Friday as 'Today'
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = String.format("%.1f", value),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight(heightPercent)
                                .background(
                                    if (isToday) colors.yellow else colors.primary,
                                    RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = labels[index],
                            fontSize = 11.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) colors.yellow else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageItemV2(appUsage: AppUsageUi, maxUsageSeconds: Float) {
    val colors = LocalCustomColors.current

    val imageBitmap = remember(appUsage.icon) {
        appUsage.icon?.toBitmap(width = 144, height = 144)?.asImageBitmap()
    }
    
    // Simulate background for apps missing icons
    val bgColors = listOf(Color(0xFFE3F2FD), Color(0xFFFFF3E0), Color(0xFFE8F5E9), Color(0xFFF3E5F5))
    val randomBg = remember(appUsage.packageName) { bgColors.random() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier.size(38.dp).background(if (imageBitmap == null) randomBg else Color.Transparent, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(bitmap = imageBitmap, contentDescription = appUsage.appName, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
            } else {
                Text("📱", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name & Progress
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appUsage.appName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            
            // Progress Bar
            val progress = (appUsage.usedSeconds / maxUsageSeconds).coerceIn(0f, 1f)
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp).background(colors.divider, RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(progress).height(5.dp).background(colors.primary, RoundedCornerShape(3.dp))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Time Value
        Text(
            text = formatCompactTime(appUsage.usedSeconds),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = colors.primary
        )
    }
}

@Composable
fun InsightCardV2() {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .border(1.dp, colors.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("💡", fontSize = 24.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text("نکته این هفته", fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "این هفته 32% بیشتر از هفته قبل از برنامه‌های آموزشی استفاده شده. آفرین! 👏",
                fontSize = 12.sp,
                color = colors.textPrimary,
                lineHeight = 18.sp
            )
        }
    }
}

// Helper to format time compactly (e.g. "1:45" or "45m") without converting digits to Persian
fun formatCompactTime(totalSeconds: Int): String {
    if (totalSeconds == 0) return "0m"
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    
    return if (hours > 0) {
        val minString = minutes.toString().padStart(2, '0')
        "$hours:$minString"
    } else {
        "${minutes}m"
    }
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, locale = "fa", name = "1. Populated Report V2 (Light)")
@Composable
fun UsageReportPopulatedPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        val dummyDrawable = android.graphics.Color.BLUE.toDrawable()
        UsageReportContent(
            state = UsageReportState(
                childName = "محمدمهدی",
                totalSecondsToday = 7450, // 2h 4m
                isLoading = false,
                appUsages = listOf(
                    AppUsageUi("com.youtube", "یوتیوب", dummyDrawable, 3650),
                    AppUsageUi("com.minecraft", "ماینکرفت", dummyDrawable, 1805),
                    AppUsageUi("com.chrome", "گوگل کروم", dummyDrawable, 900) 
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Populated Report V2 (Dark)")
@Composable
fun UsageReportPopulatedPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        val dummyDrawable = android.graphics.Color.BLUE.toDrawable()
        UsageReportContent(
            state = UsageReportState(
                childName = "محمدمهدی",
                totalSecondsToday = 7450,
                isLoading = false,
                appUsages = listOf(
                    AppUsageUi("com.youtube", "یوتیوب", dummyDrawable, 3650),
                    AppUsageUi("com.minecraft", "ماینکرفت", dummyDrawable, 1805)
                )
            ),
            onEvent = {}
        )
    }
}