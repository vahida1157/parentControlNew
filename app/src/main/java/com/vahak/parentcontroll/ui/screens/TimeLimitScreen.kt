package com.vahak.parentcontroll.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitEffect
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitEvent
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitState
import com.vahak.parentcontroll.presentation.timelimit.TimeLimitViewModel
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
@OptIn(ExperimentalMaterial3Api::class)
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
                    onClick = { onEvent(TimeLimitEvent.OpenPicker) }
                )
            }
        }

        // Bottom Sheet Time Picker
        if (state.isBottomSheetVisible) {
            ModalBottomSheet(
                onDismissRequest = { onEvent(TimeLimitEvent.ClosePicker) },
                containerColor = colors.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "تعیین سقف مصرف روزانه",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(30.dp))

                    // Simple Slider for Time (Hours)
                    var tempHours by remember { mutableFloatStateOf(state.selectedHours.toFloat()) }
                    var tempMinutes by remember { mutableFloatStateOf(state.selectedMinutes.toFloat()) }

                    Text(
                        "ساعت: ${tempHours.toInt().toPersian()}",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Slider(
                        value = tempHours,
                        onValueChange = { tempHours = it },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Text(
                        "دقیقه: ${tempMinutes.toInt().toPersian()}",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Slider(
                        value = tempMinutes,
                        onValueChange = { tempMinutes = it },
                        valueRange = 0f..59f,
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(30.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Button(
                            onClick = {
                                onEvent(
                                    TimeLimitEvent.ConfirmTime(
                                        tempHours.toInt(),
                                        tempMinutes.toInt()
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text(
                                "تایید",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Button(
                            onClick = { onEvent(TimeLimitEvent.ClosePicker) },
                            modifier = Modifier
                                .weight(1f)
                                .height(55.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.divider.copy(
                                    alpha = 0.5f
                                )
                            ),
                            shape = RoundedCornerShape(15.dp)
                        ) {
                            Text(
                                "انصراف",
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

// 3. SAFE PREVIEW
@Preview(showBackground = true, name = "Time Limit Active", locale = "fa")
@Composable
fun TimeLimitPreview() {
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