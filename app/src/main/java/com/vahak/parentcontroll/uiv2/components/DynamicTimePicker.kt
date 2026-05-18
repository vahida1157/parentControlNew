package com.vahak.parentcontroll.uiv2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vahak.parentcontroll.ui.component.PickerPresentationMode
import com.vahak.parentcontroll.uiv2.theme.AppTheme
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors
import com.vahak.parentcontroll.uiv2.theme.ParentControlTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTimePickerV2(
    mode: PickerPresentationMode = PickerPresentationMode.DIALOG,
    title: String? = null,
    initialHours: Int = 2,
    initialMinutes: Int = 30,
    hoursRange: IntRange = 0..23,
    minutesRange: IntRange = 0..59,
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit
) {
    val colors = LocalCustomColors.current

    if (mode == PickerPresentationMode.DIALOG) {
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surface)
            ) {
                TimePickerContentV2(
                    isDialog = true,
                    title = title,
                    initialHours = initialHours,
                    initialMinutes = initialMinutes,
                    hoursRange = hoursRange,
                    minutesRange = minutesRange,
                    onDismiss = onDismiss,
                    onConfirm = onConfirm
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            TimePickerContentV2(
                isDialog = false,
                title = title,
                initialHours = initialHours,
                initialMinutes = initialMinutes,
                hoursRange = hoursRange,
                minutesRange = minutesRange,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
    }
}

@Composable
private fun TimePickerContentV2(
    isDialog: Boolean,
    title: String? = null,
    initialHours: Int,
    initialMinutes: Int,
    hoursRange: IntRange,
    minutesRange: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit
) {
    val colors = LocalCustomColors.current

    var selectedHours by remember { mutableIntStateOf(initialHours) }
    var selectedMinutes by remember { mutableIntStateOf(initialMinutes) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("دقیقه", color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("ساعت", color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(colors.cardInnerBG, RoundedCornerShape(16.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(16.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NumberPickerColumn(
                modifier = Modifier.weight(1f),
                range = minutesRange,
                initialValue = initialMinutes,
                pickerStyle = WheelPickerStyle.THREE,
                onValueChange = { selectedMinutes = it }
            )
            NumberPickerColumn(
                modifier = Modifier.weight(1f),
                range = hoursRange,
                initialValue = initialHours,
                pickerStyle = WheelPickerStyle.THREE,
                onValueChange = { selectedHours = it }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.divider)
            ) {
                Text("انصراف", color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onConfirm(selectedHours, selectedMinutes) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .shadow(
                        6.dp,
                        RoundedCornerShape(14.dp),
                        spotColor = colors.primary.copy(alpha = 0.4f)
                    ),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("تأیید", color = colors.textOnPrimaryVariant, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(if (isDialog) 0.dp else 40.dp))
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Time Picker (Bottom Sheet - Light)", locale = "fa")
@Composable
fun DynamicTimePickerSheetPreviewV2Light() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        DynamicTimePickerV2(
            mode = PickerPresentationMode.BOTTOM_SHEET,
            title = "تعیین سقف مصرف روزانه",
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "2. Time Picker (Centered Dialog - Dark)", locale = "fa")
@Composable
fun DynamicTimePickerDialogPreviewV2Dark() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        DynamicTimePickerV2(
            mode = PickerPresentationMode.DIALOG,
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}