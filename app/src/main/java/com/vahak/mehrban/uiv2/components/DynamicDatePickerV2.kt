package com.vahak.mehrban.uiv2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.R
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicDatePickerV2(
    mode: PickerPresentationMode = PickerPresentationMode.DIALOG,
    title: String = stringResource(R.string.select_date),
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
        Dialog(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.surface)
            ) {
                DatePickerContentV2(
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            DatePickerContentV2(
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

@Composable
private fun DatePickerContentV2(
    isDialog: Boolean,
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = colors.textPrimary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(colors.cardInnerBG, RoundedCornerShape(16.dp))
                .border(1.dp, colors.divider, RoundedCornerShape(16.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
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
                Text(stringResource(R.string.cancel), color = colors.textSecondary, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { onConfirm(selectedYear, selectedMonth, selectedDay) },
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
                Text(stringResource(R.string.confirm), color = colors.textOnPrimaryVariant, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(if (isDialog) 0.dp else 40.dp))
    }
}

@Preview(showBackground = true, name = "1. Date Picker (Bottom Sheet - Light)", locale = "fa")
@Composable
fun DatePickerBottomSheetPreviewV2() {
    ParentControlTheme(themeMode = AppTheme.LIGHT) {
        DynamicDatePickerV2(
            mode = PickerPresentationMode.BOTTOM_SHEET,
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "2. Date Picker (Centered Dialog - Dark)", locale = "fa")
@Composable
fun DatePickerDialogPreviewV2() {
    ParentControlTheme(themeMode = AppTheme.DARK) {
        DynamicDatePickerV2(
            mode = PickerPresentationMode.DIALOG,
            onDismiss = {},
            onConfirm = { _, _, _ -> }
        )
    }
}