package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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

// 2. The Dynamic Wrapper
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDatePicker(
    mode: PickerPresentationMode = PickerPresentationMode.DIALOG,
    title: String = "انتخاب تاریخ",
    initialYear: Int = 1395,
    initialMonth: Int = 1,
    initialDay: Int = 1,
    yearRange: IntRange = 1380..1420,
    monthRange: IntRange = 1..12,
    dayRange: IntRange = 1..31,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit
) {
    val colors = LocalCustomColors.current

    if (mode == PickerPresentationMode.DIALOG) {
        // --- CENTERED MODAL ---
        Dialog(onDismissRequest = onDismiss) {
            // A Dialog needs a background shape!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(25.dp))
                    .background(colors.surface)
            ) {
                DatePickerContent(
                    isDialog = true,
                    title = title,
                    initialYear = initialYear,
                    initialMonth = initialMonth,
                    initialDay = initialDay,
                    yearRange = yearRange,
                    monthRange = monthRange,
                    dayRange = dayRange,
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
            DatePickerContent(
                isDialog = false,
                title = title,
                initialYear = initialYear,
                initialMonth = initialMonth,
                initialDay = initialDay,
                yearRange = yearRange,
                monthRange = monthRange,
                dayRange = dayRange,
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }
    }
}

// 3. The Pure Stateless UI Content
@Composable
private fun DatePickerContent(
    isDialog: Boolean, // Helps us adjust padding dynamically
    title: String,
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int,
    yearRange: IntRange,
    monthRange: IntRange,
    dayRange: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (year: Int, month: Int, day: Int) -> Unit
) {
    val colors = LocalCustomColors.current

    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonth by remember { mutableIntStateOf(initialMonth) }
    var selectedDay by remember { mutableIntStateOf(initialDay) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(25.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(20.dp))

        // The 3-Column Picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp), // Using our compact 3-item wheel height
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NumberPickerColumn(
                modifier = Modifier.weight(1f),
                range = dayRange,
                initialValue = initialDay,
                pickerStyle = WheelPickerStyle.FIVE,
                onValueChange = { selectedDay = it }
            )
            NumberPickerColumn(
                modifier = Modifier.weight(1f),
                range = monthRange,
                initialValue = initialMonth,
                pickerStyle = WheelPickerStyle.FIVE,
                onValueChange = { selectedMonth = it }
            )
            NumberPickerColumn(
                modifier = Modifier.weight(1f),
                range = yearRange,
                initialValue = initialYear,
                pickerStyle = WheelPickerStyle.FIVE,
                onValueChange = { selectedYear = it }
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Button(
                onClick = { onConfirm(selectedYear, selectedMonth, selectedDay) },
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

        Spacer(modifier = Modifier.height(if (isDialog) 0.dp else 40.dp))
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Date Picker (Bottom Sheet)", locale = "fa")
@Composable
fun DatePickerBottomSheetPreview() {
    ParentControlTheme {
        DynamicDatePicker(
            mode = PickerPresentationMode.BOTTOM_SHEET,
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "2. Date Picker (Centered Dialog)", locale = "fa")
@Composable
fun DatePickerDialogPreview() {
    ParentControlTheme {
        DynamicDatePicker(
            mode = PickerPresentationMode.DIALOG,
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}