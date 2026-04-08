package com.vahak.parentcontroll.ui.screens

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
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitEffect
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitEvent
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitState
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitViewModel
import com.vahak.parentcontroll.ui.component.DynamicTimePicker
import com.vahak.parentcontroll.ui.component.FeatureToggleCard
import com.vahak.parentcontroll.ui.component.SettingActionCard
import com.vahak.parentcontroll.ui.component.SimpleFlatHeader
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

// Persian Number Converter
fun Int.toPersian(): String {
    return this.toString().map { char ->
        if (char.isDigit()) (char.code + 1728).toChar() else char
    }.joinToString("")
}

// 1. STATEFUL WRAPPER
@Composable
fun TimeLimitScreen(
    viewModel: TimeLimitViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            if (effect is TimeLimitEffect.NavigateBack) onBackClick()
        }
    }

    TimeLimitContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

// 2. STATELESS CONTENT
@Composable
fun TimeLimitContent(
    state: TimeLimitState,
    onEvent: (TimeLimitEvent) -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Reusing the Simple Flat Header we built for Family Management!
            SimpleFlatHeader(
                title = "مدیریت زمان",
                onBackClick = { onEvent(TimeLimitEvent.BackClicked) }
            )

            Column(modifier = Modifier.padding(20.dp)) {

                // Reusable Toggle Component
                FeatureToggleCard(
                    title = "مدیریت زمان",
                    description = "با فعال کردن این قسمت می‌تونی تعیین کنی فرزندت هر روز چقدر می‌تونه با گوشی کار کنه.",
                    isActive = state.isTimeLimitActive,
                    onToggle = { onEvent(TimeLimitEvent.ToggleActive(it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Reusable Setting Action Component
                SettingActionCard(
                    headerTitle = "میزان استفاده از گوشی",
                    headerIcon = AppIcons.ChartPie,
                    valueText = "${state.selectedHours.toPersian()} ساعت و ${state.selectedMinutes.toPersian()} دقیقه",
                    isEnabled = state.isTimeLimitActive,
                    onClick = { onEvent(TimeLimitEvent.OpenPicker) }
                )
            }
        }

        // Bottom Sheet Time Picker
        if (state.isBottomSheetVisible) {
            DynamicTimePicker(
                initialHours = state.selectedHours,
                initialMinutes = state.selectedMinutes,
                onDismiss = { onEvent(TimeLimitEvent.ClosePicker) },
                onConfirm = { h, m ->
                    onEvent(TimeLimitEvent.ConfirmTime(h, m))
                }
            )
        }
    }
}

@Preview(showBackground = true, name = "Time Limit Active", locale = "fa")
@Composable
fun TimeLimitActivePreview() {
    ParentControlTheme {
        TimeLimitContent(
            state = TimeLimitState(
                isTimeLimitActive = true,
                selectedHours = 2,
                selectedMinutes = 30
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "Time Limit Deactivate", locale = "fa")
@Composable
fun TimeLimitDeactivatePreview() {
    ParentControlTheme {
        TimeLimitContent(
            state = TimeLimitState(
                isTimeLimitActive = false,
                selectedHours = 2,
                selectedMinutes = 30
            ),
            onEvent = {}
        )
    }
}