package com.vahak.mehrban.uiv2.components.dashboard

import android.content.Intent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.vahak.mehrban.R
import com.vahak.mehrban.core.data.local.entity.ChildEntity
import com.vahak.mehrban.core.data.local.entity.Gender
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.Period
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomeChildSelectorV2(
    children: List<ChildEntity>,
    activeChild: ChildEntity?,
    onSelect: (ChildEntity) -> Unit,
    onAddClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(modifier = Modifier.padding(top = 16.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)) {
        Text(
            text = stringResource(R.string.select_child),
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 20.dp)
        ) {
            items(children) { child ->
                val isSelected = child.id == activeChild?.id
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface)
                        .border(
                            2.dp,
                            if (isSelected) colors.primary else Color.Transparent,
                            RoundedCornerShape(30.dp)
                        )
                        .clickable { onSelect(child) }
                        .padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.cardInnerBG, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (child.gender == Gender.BOY) "👦" else "👧", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = child.name,
                        color = if (isSelected) colors.primary else colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(colors.surface, CircleShape)
                        .border(2.dp, colors.primary.copy(alpha = 0.5f), CircleShape)
                        .clickable { onAddClick() }, contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+", color = colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDashboardStateV2(onAddClick: () -> Unit) {
    val colors = LocalCustomColors.current
    val strokeColor = colors.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.primary.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
                .drawBehind {
                    val stroke = Stroke(
                        width = 5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 20f), 0f)
                    )
                    drawRoundRect(
                        color = strokeColor.copy(alpha = 0.3f),
                        style = stroke,
                        cornerRadius = CornerRadius(24.dp.toPx())
                    )
                }
                .clip(RoundedCornerShape(24.dp))
                .clickable { onAddClick() }
                .padding(vertical = 48.dp, horizontal = 24.dp),
            contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(
                            Brush.linearGradient(listOf(colors.primary, colors.primaryVariant)),
                            CircleShape
                        )
                        .shadow(12.dp, CircleShape, spotColor = colors.primary.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        AppIcons.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.first_child_add_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = colors.primary
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.first_child_add_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onAddClick,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        stringResource(R.string.button_add_child),
                        color = colors.textOnPrimaryVariant,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveChildSummaryCardV2(
    child: ChildEntity,
    timeLimitMins: Int,
    isTimeLimitActive: Boolean,
    usageSeconds: Int,
    onSettingsClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    val age = Period.between(child.dob, LocalDate.now()).years
    val ageText = if (age > 0) {
        stringResource(R.string.age_years, age)
    } else {
        stringResource(R.string.age_less_than_one)
    }

    val usageHours = usageSeconds / 3600
    val usageMins = (usageSeconds % 3600) / 60
    val formattedUsage = String.format("%d:%02d", usageHours, usageMins)

    val limitHours = timeLimitMins / 60
    val limitMins = timeLimitMins % 60
    val formattedLimit = if (!isTimeLimitActive || timeLimitMins == 0) {
        stringResource(R.string.unlimited)
    } else {
        buildString {
            if (limitHours > 0) append("$limitHours ${stringResource(R.string.hour)} ")
            if (limitMins > 0) append("$limitMins ${stringResource(R.string.minute)}")
        }.trim().ifEmpty { stringResource(R.string.unlimited) }
    }

    val totalLimitSeconds = (timeLimitMins * 60).toFloat().coerceAtLeast(1f)

    val progress = if (!isTimeLimitActive || timeLimitMins == 0) {
        0f
    } else {
        (usageSeconds.toFloat() / totalLimitSeconds).coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(colors.cardInnerBG, RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (child.gender == Gender.BOY) "👦" else "👧", fontSize = 32.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        child.name,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        ageText, color = colors.textSecondary, fontSize = 12.sp
                    )
                }
                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(colors.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        AppIcons.Settings,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.usage_today_label),
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    if (isTimeLimitActive && timeLimitMins > 0) {
                        stringResource(
                            R.string.usage_today_value_with_limit, formattedUsage, formattedLimit
                        )
                    } else {
                        stringResource(R.string.usage_today_value_unlimited, formattedUsage)
                    }, fontSize = 12.sp, fontWeight = FontWeight.Black, color = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when {
                    !isTimeLimitActive || timeLimitMins == 0 -> colors.divider
                    progress > 0.9f -> colors.red
                    progress > 0.75f -> colors.yellow
                    else -> colors.primary
                },
                trackColor = colors.divider,
            )
        }
    }
}

@Composable
fun ActionGridV2(
    onSettingsClick: () -> Unit,
    onReportClick: () -> Unit,
    onTimeLockClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionCardV2(
                stringResource(R.string.settings_full),
                stringResource(R.string.settings_full_desc),
                "⚙️",
                Color(0xFFE3F2FD),
                Color(0xFF1976D2),
                modifier = Modifier.weight(1f),
                onClick = onSettingsClick
            )
            ActionCardV2(
                stringResource(R.string.report_performance),
                stringResource(R.string.report_performance_desc),
                "📊",
                Color(0xFFE8F5E9),
                Color(0xFF27AE60),
                modifier = Modifier.weight(1f),
                onClick = onReportClick
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionCardV2(
                stringResource(R.string.instant_time_lock),
                stringResource(R.string.instant_time_lock_desc),
                "⏱️",
                Color(0xFFFFF3E0),
                Color(0xFFE65100),
                modifier = Modifier.weight(1f),
                onClick = onTimeLockClick
            )
            ActionCardV2(
                stringResource(R.string.live_location),
                stringResource(R.string.live_location_desc),
                "📍",
                Color(0xFFFCE4EC),
                Color(0xFFC2185B),
                modifier = Modifier.weight(1f),
                onClick = onLocationClick
            )
        }
    }
}

@Composable
fun ActionCardV2(
    title: String,
    desc: String,
    emoji: String,
    iconBg: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 18.sp, color = iconColor)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = colors.textPrimary
                )
                Text(desc, fontSize = 10.sp, color = colors.textSecondary, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerSliderV2() {
    val colors = LocalCustomColors.current
    val context = LocalContext.current

    val banners = listOf(
        Triple(
            "📢",
            stringResource(R.string.banner_official_channel),
            stringResource(R.string.banner_official_channel_desc)
        ),
        Triple(
            "💬",
            stringResource(R.string.banner_support),
            stringResource(R.string.banner_support_desc)
        ),
        /* Triple(
                    "🎁", stringResource(R.string.banner_offer), stringResource(R.string.banner_offer_desc)
                )*/
    )

    val pagerState = rememberPagerState(pageCount = { banners.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000L.milliseconds)

            if (!pagerState.isScrollInProgress) {
                try {
                    val nextPage = (pagerState.settledPage + 1) % banners.size
                    pagerState.animateScrollToPage(
                        page = nextPage, animationSpec = tween(
                            durationMillis = 800, easing = FastOutSlowInEasing
                        )
                    )
                } catch (_: CancellationException) {
                    // Ignored to keep auto-scroll alive
                }
            }
        }
    }

    val bannerColors = listOf(
        Brush.linearGradient(listOf(colors.primary, colors.primaryVariant)),
        Brush.linearGradient(listOf(colors.yellow, Color(0xFFC49530))),
//        Brush.linearGradient(listOf(colors.blue, Color(0xFF4C51BF))),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                pageSpacing = 16.dp,
                flingBehavior = PagerDefaults.flingBehavior(state = pagerState)
            ) { page ->
                val banner = banners[page]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bannerColors[page])
                        .clickable {
                            val url = when (page) {
                                0 -> "https://eitaa.com/mehrbanapp"
                                1 -> "https://eitaa.com/mehrbansupport"
                                else -> null
                            }
                            url?.let {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, it.toUri())
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                }
                            }
                        }
                        .padding(18.dp), contentAlignment = Alignment.CenterStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)
                                ), contentAlignment = Alignment.Center
                        ) {
                            Text(banner.first, fontSize = 30.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                banner.second,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                banner.third,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 11.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(banners.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(6.dp)
                        .width(if (isSelected) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) colors.primary else colors.divider)
                )
            }
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================


private val mockChildList = listOf(
    ChildEntity(id = "1", name = "علی", dob = LocalDate.of(2010, 1, 1), gender = Gender.BOY),
    ChildEntity(id = "2", name = "سارا", dob = LocalDate.of(2015, 6, 15), gender = Gender.GIRL)
)

@Preview(showBackground = true, locale = "fa", name = "1. Home Child Selector")
@Composable
fun HomeChildSelectorPreview() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        Box(modifier = Modifier.background(LocalCustomColors.current.background)) {
            HomeChildSelectorV2(
                children = mockChildList,
                activeChild = mockChildList[0],
                onSelect = {},
                onAddClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "2. Empty Dashboard State")
@Composable
fun EmptyDashboardStatePreview() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        Box(modifier = Modifier.background(LocalCustomColors.current.background)) {
            EmptyDashboardStateV2(onAddClick = {})
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "3. Active Child Summary (Dark)")
@Composable
fun ActiveChildSummaryPreview() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        Box(
            modifier = Modifier.background(LocalCustomColors.current.background)
                .padding(vertical = 16.dp)
        ) {
            ActiveChildSummaryCardV2(
                child = mockChildList[0],
                timeLimitMins = 120,
                isTimeLimitActive = true,
                usageSeconds = 3600, // 1 hour used
                onSettingsClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "4. Action Grid")
@Composable
fun ActionGridPreview() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        Box(
            modifier = Modifier.background(LocalCustomColors.current.background)
                .padding(vertical = 16.dp)
        ) {
            ActionGridV2(
                onSettingsClick = {},
                onReportClick = {},
                onTimeLockClick = {},
                onLocationClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "5. Banner Slider")
@Composable
fun BannerSliderPreview() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        Box(
            modifier = Modifier.background(LocalCustomColors.current.background)
                .padding(vertical = 16.dp)
        ) {
            BannerSliderV2()
        }
    }
}