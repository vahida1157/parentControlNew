package com.vahak.mehrban.uiv2.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.R
import com.vahak.mehrban.uiv2.theme.AppTheme
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

// ==========================================
// STATEFUL COMPONENT
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicNumberPicker(
	mode: PickerPresentationMode = PickerPresentationMode.BOTTOM_SHEET,
	title: String? = null,
	unitLabel: String? = null,
	initialValue: Int = 30,
	range: IntRange = 10..90,
	pickerStyle: WheelPickerStyle = WheelPickerStyle.THREE,
	onDismiss: () -> Unit,
	onConfirm: (value: Int) -> Unit
) {
	val colors = LocalCustomColors.current
	var selectedValue by remember { mutableIntStateOf(initialValue.coerceIn(range)) }

	if (mode == PickerPresentationMode.DIALOG) {
		Dialog(onDismissRequest = onDismiss) {
			Column(
				modifier = Modifier
					.fillMaxWidth()
					.shadow(16.dp, RoundedCornerShape(24.dp))
					.clip(RoundedCornerShape(24.dp))
					.background(colors.surface)
			) {
				NumberPickerContent(
					isDialog = true,
					title = title,
					unitLabel = unitLabel,
					selectedValue = selectedValue,
					range = range,
					pickerStyle = pickerStyle,
					onValueChange = { selectedValue = it },
					onDismiss = onDismiss,
					onConfirm = { onConfirm(selectedValue) }
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
			NumberPickerContent(
				isDialog = false,
				title = title,
				unitLabel = unitLabel,
				selectedValue = selectedValue,
				range = range,
				pickerStyle = pickerStyle,
				onValueChange = { selectedValue = it },
				onDismiss = onDismiss,
				onConfirm = { onConfirm(selectedValue) }
			)
		}
	}
}

// ==========================================
// STATELESS COMPONENT
// ==========================================

@Composable
fun NumberPickerContent(
	isDialog: Boolean,
	title: String? = null,
	unitLabel: String? = null,
	selectedValue: Int,
	range: IntRange,
	pickerStyle: WheelPickerStyle = WheelPickerStyle.THREE,
	onValueChange: (Int) -> Unit,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit
) {
	val colors = LocalCustomColors.current

	val cancelText = stringResource(R.string.cancel)
	val confirmText = stringResource(R.string.confirm)

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
			Spacer(modifier = Modifier.height(20.dp))
		}

		if (unitLabel != null) {
			Text(
				text = unitLabel,
				style = MaterialTheme.typography.labelMedium,
				fontWeight = FontWeight.Bold,
				color = colors.textSecondary,
				modifier = Modifier.padding(bottom = 8.dp)
			)
		}

		Box(
			modifier = Modifier
				.fillMaxWidth()
				.height(140.dp)
				.background(colors.cardInnerBG, RoundedCornerShape(16.dp))
				.border(1.dp, colors.divider, RoundedCornerShape(16.dp))
				.padding(8.dp),
			contentAlignment = Alignment.Center
		) {
			NumberPickerColumn(
				modifier = Modifier.fillMaxWidth(),
				range = range,
				initialValue = selectedValue,
				pickerStyle = pickerStyle,
				onValueChange = onValueChange
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
				border = BorderStroke(1.dp, colors.divider)
			) {
				Text(cancelText, color = colors.textSecondary, fontWeight = FontWeight.Bold)
			}

			Button(
				onClick = onConfirm,
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
				Text(confirmText, color = colors.textOnPrimaryVariant, fontWeight = FontWeight.Bold)
			}
		}

		Spacer(modifier = Modifier.height(if (isDialog) 0.dp else 40.dp))
	}
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "1. Number Picker (Bottom Sheet - Light)", locale = "fa")
@Composable
fun DynamicNumberPickerSheetPreviewLight() {
	ParentControlTheme(themeMode = AppTheme.LIGHT) {
		DynamicNumberPicker(
			mode = PickerPresentationMode.BOTTOM_SHEET,
			title = "انتخاب وزن کودک",
			unitLabel = "کیلوگرم",
			initialValue = 30,
			range = 10..90,
			onDismiss = {},
			onConfirm = {}
		)
	}
}

@Preview(showBackground = true, name = "2. Number Picker (Centered Dialog - Dark)", locale = "fa")
@Composable
fun DynamicNumberPickerDialogPreviewDark() {
	ParentControlTheme(themeMode = AppTheme.DARK) {
		DynamicNumberPicker(
			mode = PickerPresentationMode.DIALOG,
			title = "انتخاب وزن کودک",
			unitLabel = "کیلوگرم",
			initialValue = 30,
			range = 10..90,
			onDismiss = {},
			onConfirm = {}
		)
	}
}