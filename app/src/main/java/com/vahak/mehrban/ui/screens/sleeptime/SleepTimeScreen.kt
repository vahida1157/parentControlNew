package com.vahak.mehrban.ui.screens.sleeptime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.mehrban.presentation.sleeptime.SleepTimeEffect
import com.vahak.mehrban.presentation.sleeptime.SleepTimeEvent
import com.vahak.mehrban.presentation.sleeptime.SleepTimeState
import com.vahak.mehrban.presentation.sleeptime.SleepTimeViewModel
import com.vahak.mehrban.presentation.sleeptime.TimeEditMode
import com.vahak.mehrban.ui.component.DynamicTimePicker
import com.vahak.mehrban.ui.component.FeatureToggleCard
import com.vahak.mehrban.ui.component.SettingActionCard
import com.vahak.mehrban.ui.component.SimpleFlatHeader
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme
import java.time.LocalTime

// Helper to format time properly (e.g., 07:05 instead of 7:5) and make it Persian
fun formatTime(hour: Int, minute: Int): String {
    val formatted = String.format("%02d:%02d", hour, minute)
    return formatted.map { char ->
        if (char.isDigit()) (char.code + 1728).toChar() else char
    }.joinToString("")
}

// --- 1. STATEFUL WRAPPER ---
@Composable
fun SleepTimeScreen(
    viewModel: SleepTimeViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is SleepTimeEffect.NavigateBack) onBackClick()
        }
    }

    SleepTimeContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

// --- 2. STATELESS CONTENT ---
@Composable
fun SleepTimeContent(
    state: SleepTimeState,
    onEvent: (SleepTimeEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            SimpleFlatHeader(
                title = "برنامه خواب",
                onBackClick = { onEvent(SleepTimeEvent.BackClicked) }
            )

            Column(modifier = Modifier.padding(20.dp)) {

                // Main Toggle
                FeatureToggleCard(
                    title = "فعال‌سازی برنامه خواب",
                    description = "در این ساعات، گوشی به حالت استراحت می‌رود و دسترسی به برنامه‌ها مسدود می‌شود تا فرزندتان خواب راحتی داشته باشد.",
                    isActive = state.isSleepTimeActive,
                    onToggle = { onEvent(SleepTimeEvent.ToggleActive(it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Start Time Card (When to sleep)
                SettingActionCard(
                    headerTitle = "زمان شروع خواب",
                    headerIcon = AppIcons.Moon, // PRO TIP: Replace with a Moon Icon if you have one!
                    valueText = formatTime(state.startTime.hour, state.startTime.minute),
                    isEnabled = state.isSleepTimeActive,
                    onClick = { onEvent(SleepTimeEvent.OpenPicker(TimeEditMode.START)) }
                )

                // End Time Card (When to wake up)
                SettingActionCard(
                    headerTitle = "زمان بیداری",
                    headerIcon = AppIcons.Sun, // PRO TIP: Replace with a Sun/Alarm Icon if you have one!
                    valueText = formatTime(state.endTime.hour, state.endTime.minute),
                    isEnabled = state.isSleepTimeActive,
                    onClick = { onEvent(SleepTimeEvent.OpenPicker(TimeEditMode.END)) }
                )
            }
        }

        // --- DYNAMIC TIME PICKER MODAL ---
        if (state.isPickerVisible) {
            val isStart = state.currentEditMode == TimeEditMode.START

            DynamicTimePicker(
                title = if (isStart) "انتخاب زمان شروع خواب" else "انتخاب زمان بیداری",
                initialHours = if (isStart) state.startTime.hour else state.endTime.hour,
                initialMinutes = if (isStart) state.startTime.minute else state.endTime.minute,
                hoursRange = 0..23, // PRO FIX: SleepTime needs a 24-hour clock range!
                minutesRange = 0..59,
                onDismiss = { onEvent(SleepTimeEvent.ClosePicker) },
                onConfirm = { h, m -> onEvent(SleepTimeEvent.ConfirmTime(h, m)) }
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. SleepTime (Active)", locale = "fa")
@Composable
fun SleepTimeActivePreview() {
    ParentControlTheme {
        SleepTimeContent(
            state = SleepTimeState(
                isSleepTimeActive = true,
                startTime = LocalTime.of(22, 30),
                endTime = LocalTime.of(7, 0),
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. SleepTime (Disabled)", locale = "fa")
@Composable
fun SleepTimeDisabledPreview() {
    ParentControlTheme {
        SleepTimeContent(
            state = SleepTimeState(
                isSleepTimeActive = false,
                startTime = LocalTime.of(22, 30),
                endTime = LocalTime.of(7, 0),
            ),
            onEvent = {}
        )
    }
}