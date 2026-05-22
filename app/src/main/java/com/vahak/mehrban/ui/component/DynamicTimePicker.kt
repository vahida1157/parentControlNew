package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicTimePicker(
    mode: PickerPresentationMode = PickerPresentationMode.DIALOG,
    title: String? = null,
    initialHours: Int = 2,
    initialMinutes: Int = 30,
    hoursRange: IntRange = 0..12,
    minutesRange: IntRange = 0..59,
    onDismiss: () -> Unit,
    onConfirm: (hours: Int, minutes: Int) -> Unit
) {
    val colors = LocalCustomColors.current

    if (mode == PickerPresentationMode.DIALOG) {
        // --- CENTERED MODAL ---
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.surface)
            ) {
                TimePickerContent(
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
        // --- BOTTOM SHEET ---
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.surface
        ) {
            TimePickerContent(
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

// The Stateless UI Content
@Composable
private fun TimePickerContent(
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
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(30.dp))
        }

        // Labels for the columns
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("دقیقه", color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("ساعت", color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // The 2-Column Picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Minute Column
            NumberPickerColumn(
                modifier = Modifier.weight(1f),
                range = minutesRange,
                initialValue = initialMinutes,
                pickerStyle = WheelPickerStyle.THREE,
                onValueChange = { selectedMinutes = it }
            )

            // Hour Column
            NumberPickerColumn(
                modifier = Modifier.weight(1f),
                range = hoursRange,
                initialValue = initialHours,
                pickerStyle = WheelPickerStyle.THREE,
                onValueChange = { selectedHours = it }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Button(
                onClick = { onConfirm(selectedHours, selectedMinutes) },
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("تایید", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .height(55.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.divider.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(15.dp)
            ) {
                Text("انصراف", color = colors.textPrimary, fontWeight = FontWeight.Bold)
            }
        }

        // Dynamically trim the bottom padding for Dialogs
        Spacer(modifier = Modifier.height(if (isDialog) 0.dp else 20.dp))
    }
}

@Preview(showBackground = true, name = "1. Time Picker (Bottom Sheet)", locale = "fa")
@Composable
fun DynamicTimePickerSheetPreview() {
    ParentControlTheme {
        DynamicTimePicker(
            mode = PickerPresentationMode.BOTTOM_SHEET,
            title = "تعیین سقف مصرف روزانه",
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "2. Time Picker (Centered Dialog)", locale = "fa")
@Composable
fun DynamicTimePickerDialogPreview() {
    ParentControlTheme {
        DynamicTimePicker(
            mode = PickerPresentationMode.DIALOG,
            title = null, // Testing without title!
            onDismiss = {},
            onConfirm = { _, _ -> }
        )
    }
}