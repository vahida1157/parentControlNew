package com.vahak.parentcontroll.uiv2.screens.bedtime

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.bedtime.BedtimeEffectV2
import com.vahak.parentcontroll.presentation.bedtime.BedtimeEventV2
import com.vahak.parentcontroll.presentation.bedtime.BedtimeStateV2
import com.vahak.parentcontroll.presentation.bedtime.BedtimeViewModelV2
import com.vahak.parentcontroll.presentation.bedtime.TimeEditModeV2
import com.vahak.parentcontroll.ui.component.DynamicTimePicker
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme
import java.time.LocalTime

// Standard time formatter (No Persian conversion)
private fun formatTimeStandard(time: LocalTime): String {
    return String.format("%02d:%02d", time.hour, time.minute)
}

@Composable
fun BedtimeScreen(
    viewModel: BedtimeViewModelV2 = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is BedtimeEffectV2.NavigateBack -> onBackClick()
                is BedtimeEffectV2.ShowToast -> Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    BedtimeContent(
        state = state,
        onEvent = viewModel::onEvent
    )
}

@Composable
fun BedtimeContent(
    state: BedtimeStateV2,
    onEvent: (BedtimeEventV2) -> Unit
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
            ScreenHeaderV2(
                title = "زمان خواب",
                subtitle = "تنظیمات",
                iconEmoji = "🌙",
                onBackClick = { onEvent(BedtimeEventV2.BackClicked) }
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
                        title = "فعال‌سازی زمان خواب",
                        desc = "قفل خودکار دستگاه در زمان مشخص",
                        iconEmoji = "🌙",
                        iconBg = Color(0xFFE8F5E9), // Light green background from HTML
                        isActive = state.isBedtimeActive,
                        onToggle = { onEvent(BedtimeEventV2.ToggleActive(it)) }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isBedtimeActive) {
                        // Time Inputs (Side by Side)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TimeInputGroupV2(
                                modifier = Modifier.weight(1f),
                                label = "زمان شروع خواب",
                                timeText = formatTimeStandard(state.startTime),
                                onClick = { onEvent(BedtimeEventV2.OpenPicker(TimeEditModeV2.START)) }
                            )
                            TimeInputGroupV2(
                                modifier = Modifier.weight(1f),
                                label = "زمان بیداری 🌄",
                                timeText = formatTimeStandard(state.endTime),
                                onClick = { onEvent(BedtimeEventV2.OpenPicker(TimeEditModeV2.END)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Extra Toggles
                        ToggleRowV2(
                            title = "حالت عدم مزاحمت",
                            desc = "بی‌صدا کردن اعلان‌ها در زمان خواب",
                            iconEmoji = "📱",
                            iconBg = Color(0xFFFCE4EC), // Light pink
                            isActive = state.isDndEnabled,
                            onToggle = { onEvent(BedtimeEventV2.ToggleDnd(it)) }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ToggleRowV2(
                            title = "تماس‌های اضطراری",
                            desc = "پذیرش تماس از شماره‌های خاص",
                            iconEmoji = "📞",
                            iconBg = Color(0xFFE3F2FD), // Light blue
                            isActive = state.isEmergencyCallsEnabled,
                            onToggle = { onEvent(BedtimeEventV2.ToggleEmergencyCalls(it)) }
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        ToggleRowV2(
                            title = "فیلتر نور آبی",
                            desc = "کاهش نور آبی صفحه در شب",
                            iconEmoji = "💡",
                            iconBg = Color(0xFFF3E5F5), // Light purple
                            isActive = state.isBlueLightFilterEnabled,
                            onToggle = { onEvent(BedtimeEventV2.ToggleBlueLight(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onEvent(BedtimeEventV2.SaveClicked) },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(14.dp), spotColor = colors.primary.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            disabledContainerColor = colors.backgroundButtonDisable
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(color = colors.textOnPrimaryVariant, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("ذخیره تنظیمات", color = colors.textOnPrimaryVariant, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        // Time Picker Modal
        if (state.isPickerVisible) {
            val isStart = state.currentEditMode == TimeEditModeV2.START
            
            DynamicTimePicker(
                title = if (isStart) "زمان شروع خواب" else "زمان بیداری",
                initialHours = if (isStart) state.startTime.hour else state.endTime.hour,
                initialMinutes = if (isStart) state.startTime.minute else state.endTime.minute,
                hoursRange = 0..23,
                minutesRange = 0..59,
                onDismiss = { onEvent(BedtimeEventV2.ClosePicker) },
                onConfirm = { h, m -> onEvent(BedtimeEventV2.ConfirmTime(h, m)) }
            )
        }
    }
}

// --- SUB COMPONENTS ---

@Composable
private fun ScreenHeaderV2(title: String, subtitle: String? = null, iconEmoji: String? = null, onBackClick: () -> Unit) {
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
                        Text(text = subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                    }
                    Text(text = title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
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
private fun TimeInputGroupV2(
    modifier: Modifier = Modifier,
    label: String,
    timeText: String,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Column(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            checked = isActive,
            onCheckedChange = onToggle,
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

@Preview(showBackground = true, name = "1. Bedtime V2 (Active)", locale = "fa")
@Composable
fun BedtimePreviewActive() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        BedtimeContent(
            state = BedtimeStateV2(
                isBedtimeActive = true,
                startTime = LocalTime.of(21, 30),
                endTime = LocalTime.of(7, 0)
            ),
            onEvent = {}
        )
    }
}

@Preview(showBackground = true, name = "2. Bedtime V2 (Disabled - Dark)", locale = "fa")
@Composable
fun BedtimePreviewDisabled() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        BedtimeContent(
            state = BedtimeStateV2(isBedtimeActive = false),
            onEvent = {}
        )
    }
}