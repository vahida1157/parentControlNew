package com.vahak.mehrban.uiv2.screens.sleeptime

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
import com.vahak.mehrban.presentation.sleeptime.SleepTimeEffectV2
import com.vahak.mehrban.presentation.sleeptime.SleepTimeEventV2
import com.vahak.mehrban.presentation.sleeptime.SleepTimeStateV2
import com.vahak.mehrban.presentation.sleeptime.SleepTimeViewModelV2
import com.vahak.mehrban.presentation.sleeptime.TimeEditModeV2
import com.vahak.mehrban.uiv2.components.DynamicTimePickerV2
import com.vahak.mehrban.uiv2.components.header.HeaderAction
import com.vahak.mehrban.uiv2.components.header.MehrbanHeader
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme
import java.time.LocalTime

private fun formatTimeStandard(time: LocalTime): String {
    return String.format("%02d:%02d", time.hour, time.minute)
}

@Composable
fun SleepTimeScreen(
    viewModel: SleepTimeViewModelV2 = hiltViewModel(), onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val sleepSettingsSavedMessage = stringResource(R.string.sleep_settings_saved)
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SleepTimeEffectV2.NavigateBack -> onBackClick()
                is SleepTimeEffectV2.ShowSavedToast -> {
                    Toast.makeText(
                        context, sleepSettingsSavedMessage, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    SleepTimeContent(
        state = state, onEvent = viewModel::onEvent
    )
}

@Composable
fun SleepTimeContent(
    state: SleepTimeStateV2, onEvent: (SleepTimeEventV2) -> Unit
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
            // Header
            MehrbanHeader(
                title = stringResource(R.string.sleeptime_title),
                subtitle = stringResource(R.string.sleeptime_subtitle),
                iconEmoji = "🌙",
                action = HeaderAction.Back { onEvent(SleepTimeEventV2.BackClicked) },
            )

            // Main Card
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

                    // Main Toggle
                    ToggleRowV2(
                        title = stringResource(R.string.sleeptime_activate_title),
                        desc = stringResource(R.string.sleeptime_activate_desc),
                        iconEmoji = "🌙",
                        iconBg = Color(0xFFE8F5E9),
                        isActive = state.isSleepTimeActive,
                        onToggle = { onEvent(SleepTimeEventV2.ToggleActive(it)) })

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isSleepTimeActive) {
                        // Time Inputs
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                TimeInputGroupV2(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.sleeptime_start_label),
                                    timeText = formatTimeStandard(state.startTime),
                                    onClick = { onEvent(SleepTimeEventV2.OpenPicker(TimeEditModeV2.START)) })
                                TimeInputGroupV2(
                                    modifier = Modifier.weight(1f),
                                    label = stringResource(R.string.sleeptime_end_label),
                                    timeText = formatTimeStandard(state.endTime),
                                    onClick = { onEvent(SleepTimeEventV2.OpenPicker(TimeEditModeV2.END)) })
                            }
                        }

                        // TODO future: add these features in future when it's ready
//                        Spacer(modifier = Modifier.height(24.dp))
//
//                        // Extra Toggles
//                        ToggleRowV2(
//                            title = "حالت عدم مزاحمت",
//                            desc = "بی‌صدا کردن اعلان‌ها در زمان خواب",
//                            iconEmoji = "📱",
//                            iconBg = Color(0xFFFCE4EC), // Light pink
//                            isActive = state.isDndEnabled,
//                            onToggle = { onEvent(SleepTimeEventV2.ToggleDnd(it)) }
//                        )
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        ToggleRowV2(
//                            title = "تماس‌های اضطراری",
//                            desc = "پذیرش تماس از شماره‌های خاص",
//                            iconEmoji = "📞",
//                            iconBg = Color(0xFFE3F2FD), // Light blue
//                            isActive = state.isEmergencyCallsEnabled,
//                            onToggle = { onEvent(SleepTimeEventV2.ToggleEmergencyCalls(it)) }
//                        )
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        ToggleRowV2(
//                            title = "فیلتر نور آبی",
//                            desc = "کاهش نور آبی صفحه در شب",
//                            iconEmoji = "💡",
//                            iconBg = Color(0xFFF3E5F5), // Light purple
//                            isActive = state.isBlueLightFilterEnabled,
//                            onToggle = { onEvent(SleepTimeEventV2.ToggleBlueLight(it)) }
//                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onEvent(SleepTimeEventV2.SaveClicked) },
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

        // Time Picker Modal
        if (state.isPickerVisible) {
            val isStart = state.currentEditMode == TimeEditModeV2.START

            DynamicTimePickerV2(
                title = if (isStart) stringResource(R.string.sleeptime_picker_start_title) else stringResource(
                    R.string.sleeptime_picker_end_title
                ),
                initialHours = if (isStart) state.startTime.hour else state.endTime.hour,
                initialMinutes = if (isStart) state.startTime.minute else state.endTime.minute,
                hoursRange = 0..23,
                minutesRange = 0..59,
                onDismiss = { onEvent(SleepTimeEventV2.ClosePicker) },
                onConfirm = { h, m -> onEvent(SleepTimeEventV2.ConfirmTime(h, m)) })
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
private fun TimeInputGroupV2(
    modifier: Modifier = Modifier, label: String, timeText: String, onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Column(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = timeText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary,
            letterSpacing = 2.sp
        )
    }
}

@Composable
private fun ToggleRowV2(
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
            Text(desc, fontSize = 11.sp, color = colors.textSecondary, lineHeight = 16.sp)
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

@Preview(showBackground = true, name = "1. SleepTime V2 (Active)", locale = "fa")
@Composable
fun SleepTimePreviewActive() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        SleepTimeContent(
            state = SleepTimeStateV2(
                isSleepTimeActive = true,
                startTime = LocalTime.of(21, 30),
                endTime = LocalTime.of(7, 0)
            ), onEvent = {})
    }
}

@Preview(showBackground = true, name = "2. SleepTime V2 (Disabled - Dark)", locale = "fa")
@Composable
fun SleepTimePreviewDisabled() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        SleepTimeContent(
            state = SleepTimeStateV2(isSleepTimeActive = false), onEvent = {})
    }
}