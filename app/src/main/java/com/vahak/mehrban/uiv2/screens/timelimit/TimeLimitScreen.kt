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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.R
import com.vahak.mehrban.presentation.timelimit.TimeLimitEffectV2
import com.vahak.mehrban.presentation.timelimit.TimeLimitEventV2
import com.vahak.mehrban.presentation.timelimit.TimeLimitStateV2
import com.vahak.mehrban.presentation.timelimit.TimeLimitViewModelV2
import com.vahak.mehrban.uiv2.components.DynamicTimePickerV2
import com.vahak.mehrban.uiv2.components.PickerPresentationMode
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@Composable
fun TimeLimitScreen(
    viewModel: TimeLimitViewModelV2 = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val timeSettingsSavedMessage = stringResource(R.string.time_settings_saved)

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TimeLimitEffectV2.NavigateBack -> onBackClick()
                is TimeLimitEffectV2.ShowSavedToast -> Toast.makeText(
                    context, timeSettingsSavedMessage, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    TimeLimitContent(state = state, onEvent = viewModel::onEvent)
}

@Composable
fun TimeLimitContent(
    state: TimeLimitStateV2, onEvent: (TimeLimitEventV2) -> Unit
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
            MehrbanHeader(
                title = stringResource(R.string.timelimit_title),
                subtitle = stringResource(R.string.timelimit_subtitle),
                iconEmoji = "⏰",
                action = HeaderAction.Back { onEvent(TimeLimitEventV2.BackClicked) },
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
                        title = stringResource(R.string.timelimit_activate_title),
                        desc = stringResource(R.string.timelimit_activate_desc),
                        iconEmoji = "⏳",
                        iconBg = colors.orangeLight,
                        isActive = state.isTimeLimitActive,
                        onToggle = { onEvent(TimeLimitEventV2.ToggleActive(it)) })

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isTimeLimitActive) {
                        Text(
                            text = stringResource(R.string.timelimit_quick_select),
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
                                title = stringResource(R.string.timelimit_30min),
                                isSelected = state.hours == 0 && state.minutes == 30,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(0, 30)) }
                            TimePresetButton(
                                title = stringResource(R.string.timelimit_1hour),
                                isSelected = state.hours == 1 && state.minutes == 0,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(1, 0)) }
                            TimePresetButton(
                                title = stringResource(R.string.timelimit_2hours),
                                isSelected = state.hours == 2 && state.minutes == 0,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(2, 0)) }
                            TimePresetButton(
                                title = stringResource(R.string.timelimit_3hours),
                                isSelected = state.hours == 3 && state.minutes == 0,
                                modifier = Modifier.weight(1f)
                            ) { onEvent(TimeLimitEventV2.TimePresetSelected(3, 0)) }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = stringResource(R.string.timelimit_custom_time),
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

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
                                horizontalArrangement = Arrangement.Center) {
                                TimeDisplayBox(
                                    label = stringResource(R.string.hour),
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
                                    label = stringResource(R.string.minute),
                                    value = state.minutes.toString(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        ToggleRowV2(
                            title = "پاداش زمان با ورزش",
                            desc = "کودک می‌تواند با ورزش کردن، زمان اضافه دریافت کند",
                            iconEmoji = "🏃",
                            iconBg = colors.greenLight,
                            isActive = state.isExerciseRewardEnabled,
                            onToggle = { onEvent(TimeLimitEventV2.ToggleExerciseReward(it)) }
                        )

                        if (state.isExerciseRewardEnabled) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "حداکثر زمان پاداش در روز",
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
                                    title = "۱ ساعت",
                                    isSelected = state.maxRewardHours == 1,
                                    modifier = Modifier.weight(1f)
                                ) { onEvent(TimeLimitEventV2.MaxRewardSelected(1)) }
                                TimePresetButton(
                                    title = "۲ ساعت",
                                    isSelected = state.maxRewardHours == 2,
                                    modifier = Modifier.weight(1f)
                                ) { onEvent(TimeLimitEventV2.MaxRewardSelected(2)) }
                                TimePresetButton(
                                    title = "۳ ساعت",
                                    isSelected = state.maxRewardHours == 3,
                                    modifier = Modifier.weight(1f)
                                ) { onEvent(TimeLimitEventV2.MaxRewardSelected(3)) }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    colors.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    colors.primary.copy(alpha = 0.15f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(16.dp), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⏰", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    stringResource(R.string.timelimit_daily_allowed),
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                val hourLabel = stringResource(R.string.hour)
                                val minuteLabel = stringResource(R.string.minute)
                                val noLimit = stringResource(R.string.unlimited)

                                val previewText = when {
                                    state.hours > 0 && state.minutes > 0 -> "${state.hours} $hourLabel و ${state.minutes} $minuteLabel"
                                    state.hours > 0 -> "${state.hours} $hourLabel"
                                    state.minutes > 0 -> "${state.minutes} $minuteLabel"
                                    else -> noLimit
                                }
                                Text(
                                    previewText,
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
                                stringResource(R.string.button_save_settings),
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

        if (state.isPickerVisible) {
            DynamicTimePickerV2(
                mode = PickerPresentationMode.BOTTOM_SHEET,
                title = stringResource(R.string.time_limit_title),
                initialHours = state.hours,
                initialMinutes = state.minutes,
                hoursRange = 0..23,
                minutesRange = 0..59,
                onDismiss = { onEvent(TimeLimitEventV2.ClosePicker) },
                onConfirm = { h, m -> onEvent(TimeLimitEventV2.ConfirmTime(h, m)) })
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
fun TimePresetButton(
    title: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.08f) else colors.surface)
            .border(
                2.dp, if (isSelected) colors.primary else colors.divider, RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
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
            checked = isActive, onCheckedChange = onToggle, colors = SwitchDefaults.colors(
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