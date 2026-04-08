package com.vahak.parentcontroll.ui.screens.report

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vahak.parentcontroll.ui.component.SimpleFlatHeader
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

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
            .systemBarsPadding()
    ) {
        // Show the child's name dynamically in the header
        SimpleFlatHeader(
            title = "گزارش فعالیت ${state.childName}",
            onBackClick = { onEvent(UsageReportEvent.BackClicked) }
        )

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Total Time Summary Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 25.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(25.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "مجموع زمان استفاده امروز",
                                color = colors.surface.copy(alpha = 0.8f),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = formatPreciseTime(state.totalSecondsToday),
                                color = colors.surface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                    }

                    Text(
                        text = "جزئیات برنامه‌ها",
                        fontWeight = FontWeight.Bold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = 15.dp, start = 5.dp)
                    )
                }

                // App List
                if (state.appUsages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "هیچ فعالیتی برای امروز ثبت نشده است.",
                                color = colors.textHint,
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    items(state.appUsages) { appUsage ->
                        AppUsageListItem(appUsage)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// --- 3. STYLED APP LIST ITEM ---
@Composable
fun AppUsageListItem(appUsage: AppUsageUi) {
    val colors = LocalCustomColors.current

    // Explicit sizing for icons to prevent crash
    val imageBitmap = remember(appUsage.icon) {
        appUsage.icon?.toBitmap(width = 144, height = 144)?.asImageBitmap()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon Container (Matches Dashboard Style)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(colors.background, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap,
                        contentDescription = appUsage.appName,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    // Fallback
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.divider, RoundedCornerShape(8.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appUsage.appName,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                // Beautiful Time Badge
                Box(
                    modifier = Modifier
                        .background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = formatPreciseTime(appUsage.usedSeconds),
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// --- 4. PRECISE TIME FORMATTER (WITH SECONDS) ---
fun formatPreciseTime(totalSeconds: Int): String {
    if (totalSeconds == 0) return "۰ ثانیه"

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return buildString {
        if (hours > 0) append("$hours ساعت ")
        if (hours > 0 && (minutes > 0 || seconds > 0)) append("و ")
        if (minutes > 0) append("$minutes دقیقه ")
        if (minutes > 0 && seconds > 0) append("و ")
        if (seconds > 0 || (hours == 0 && minutes == 0)) append("$seconds ثانیه")
    }.trim()
}

// ==========================================
// PREVIEWS
// ==========================================
@Preview(showBackground = true, locale = "fa", name = "1. Populated Report")
@Composable
fun UsageReportPopulatedPreview() {
    ParentControlTheme {
        // Dummy ColorDrawable for Preview
        val dummyDrawable = android.graphics.Color.BLUE.toDrawable()

        UsageReportContent(
            state = UsageReportState(
                childName = "محمدمهدی",
                totalSecondsToday = 7450, // 2h 4m 10s
                isLoading = false,
                appUsages = listOf(
                    AppUsageUi("com.youtube", "یوتیوب", dummyDrawable, 3650), // 1h 0m 50s
                    AppUsageUi("com.minecraft", "ماینکرفت", dummyDrawable, 1805), // 30m 5s
                    AppUsageUi("com.chrome", "گوگل کروم", dummyDrawable, 45) // 45s
                )
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Empty Report")
@Composable
fun UsageReportEmptyPreview() {
    ParentControlTheme {
        UsageReportContent(
            state = UsageReportState(
                childName = "سارا",
                totalSecondsToday = 0,
                isLoading = false,
                appUsages = emptyList()
            ),
            onEvent = {}
        )
    }
}