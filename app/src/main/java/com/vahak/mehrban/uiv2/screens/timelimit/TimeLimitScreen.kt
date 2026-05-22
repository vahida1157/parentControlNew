package com.vahak.mehrban.uiv2.screens.timelimit

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.presentation.timelimit.TimeLimitEffectV2
import com.vahak.mehrban.presentation.timelimit.TimeLimitEventV2
import com.vahak.mehrban.presentation.timelimit.TimeLimitStateV2
import com.vahak.mehrban.presentation.timelimit.TimeLimitViewModelV2
import com.vahak.mehrban.ui.component.PickerPresentationMode
import com.vahak.mehrban.uiv2.components.DynamicTimePickerV2
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun TimeLimitScreen(
    viewModel: TimeLimitViewModelV2 = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TimeLimitEffectV2.NavigateBack -> onBackClick()
                is TimeLimitEffectV2.ShowToast -> Toast.makeText(
                    context,
                    effect.message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    TimeLimitContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun TimeLimitContent(
    state: TimeLimitStateV2,
    onEvent: (TimeLimitEventV2) -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            ScreenHeaderV2(
                title = "مدیریت زمان استفاده",
                subtitle = "تنظیمات",
                iconEmoji = "⏰",
                onBackClick = { onEvent(TimeLimitEventV2.BackClicked) }
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-30).dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {

                    ToggleRowV2(
                        title = "فعال‌سازی محدودیت زمان",
                        desc = "اعمال سقف استفاده روزانه",
                        iconEmoji = "⏳",
                        iconBg = colors.orangeLight,
                        isActive = state.isTimeLimitActive,
                        onToggle = { onEvent(TimeLimitEventV2.ToggleActive(it)) }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isTimeLimitActive) {
                        Text(
                            text = "انتخاب سریع",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimePresetButton(
                                title = "۳۰ دقیقه",
                                isSelected = state.hours == 0 && state.minutes == 30,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(0, 30)) }
                            TimePresetButton(
                                title = "۱ ساعت",
                                isSelected = state.hours == 1 && state.minutes == 0,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(1, 0)) }
                            TimePresetButton(
                                title = "۲ ساعت",
                                isSelected = state.hours == 2 && state.minutes == 0,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(2, 0)) }
                            TimePresetButton(
                                title = "۳ ساعت",
                                isSelected = state.hours == 3 && state.minutes == 0,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(3, 0)) }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "یا زمان دلخواه را وارد کنید",
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // --- NEW: Clickable Time Display ---
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(colors.cardInnerBG, RoundedCornerShape(12.dp))
                                    .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onEvent(TimeLimitEventV2.OpenPicker) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                TimeDisplayBox(
                                    label = "ساعت",
                                    value = state.hours.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = ":",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                TimeDisplayBox(
                                    label = "دقیقه",
                                    value = state.minutes.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    colors.primary.copy(alpha = 0.08f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    colors.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏰", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "زمان مجاز روزانه:",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    state.previewText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.primary
                                )
                            }
                        }

                        // TODO future: add these features in future when it's ready
//                        Spacer(modifier = Modifier.height(24.dp))
//
//                        ToggleRowV2(
//                            title = "هشدار پایان زمان",
//                            desc = "هشدار ۵ دقیقه قبل از پایان",
//                            iconEmoji = "🔔",
//                            iconBg = colors.orangeLight,
//                            isActive = state.isWarningEnabled,
//                            onToggle = { onEvent(TimeLimitEventV2.ToggleWarning(it)) }
//                        )
//                        Spacer(modifier = Modifier.height(12.dp))
//                        ToggleRowV2(
//                            title = "تفاوت روزهای آخر هفته",
//                            desc = "زمان مجاز جداگانه برای تعطیلات",
//                            iconEmoji = "📅",
//                            iconBg = colors.greenLight,
//                            isActive = state.isWeekendSeparate,
//                            onToggle = { onEvent(TimeLimitEventV2.ToggleWeekend(it)) }
//                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onEvent(TimeLimitEventV2.SaveClicked) },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(
                                8.dp,
                                RoundedCornerShape(14.dp),
                                spotColor = colors.primary.copy(alpha = 0.4f)
                            ),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            disabledContainerColor = colors.backgroundButtonDisable
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                color = colors.textOnPrimaryVariant,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "ذخیره تنظیمات",
                                color = colors.textOnPrimaryVariant,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        // --- NEW: Dynamic Time Picker Integration ---
        if (state.isPickerVisible) {
            DynamicTimePickerV2(
                mode = PickerPresentationMode.BOTTOM_SHEET,
                title = "تعیین سقف مصرف روزانه",
                initialHours = state.hours,
                initialMinutes = state.minutes,
                hoursRange = 0..23,
                minutesRange = 0..59,
                onDismiss = { onEvent(TimeLimitEventV2.ClosePicker) },
                onConfirm = { h, m -> onEvent(TimeLimitEventV2.ConfirmTime(h, m)) }
            )
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
private fun TimeDisplayBox(label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalCustomColors.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = colors.primary
        )
    }
}

@Composable
fun ScreenHeaderV2(
    title: String,
    subtitle: String? = null,
    iconEmoji: String? = null,
    onBackClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    val headerGradient = Brush.linearGradient(listOf(colors.primary, colors.primaryVariant))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = headerGradient,
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            )
            .padding(top = 40.dp, bottom = 60.dp, start = 20.dp, end = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (iconEmoji != null) {
                    Text(iconEmoji, fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Column {
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp
                        )
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onBackClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.Back, contentDescription = "Back", tint = Color.White)
            }
        }
    }
}

@Composable
fun TimePresetButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface)
            .border(
                2.dp,
                if (isSelected) colors.primary else colors.divider,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            color = if (isSelected) colors.primary else colors.textPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ToggleRowV2(
    title: String,
    desc: String,
    iconEmoji: String,
    iconBg: Color,
    isActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val colors = LocalCustomColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(iconBg, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(iconEmoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = colors.textPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(desc, fontSize = 11.sp, color = colors.textSecondary)
        }
        Switch(
            checked = isActive, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.divider
            )
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Time Limit Light", locale = "fa")
@Composable
fun TimeLimitPreviewLight() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        TimeLimitContent(state = TimeLimitStateV2(), onEvent = {})
    }
}

@Preview(showBackground = true, name = "2. Time Limit Dark", locale = "fa")
@Composable
fun TimeLimitPreviewDark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        TimeLimitContent(state = TimeLimitStateV2(isTimeLimitActive = false), onEvent = {})
    }
}