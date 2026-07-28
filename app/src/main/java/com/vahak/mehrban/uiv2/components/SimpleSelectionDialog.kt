package com.vahak.mehrban.uiv2.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vahak.mehrban.R
import com.vahak.mehrban.uiv2.theme.AppIcons
import com.vahak.mehrban.uiv2.theme.LocalCustomColors
import com.vahak.mehrban.uiv2.theme.ParentControlTheme

// 1. The Wrapper (Safe for Previews)
@Composable
fun <T> SimpleSelectionDialog(
    title: String,
    options: List<Pair<T, String>>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val cancelLabel = stringResource(R.string.cancel)
    Dialog(onDismissRequest = onDismiss) {
        SimpleSelectionDialogContent(
            title, cancelLabel, options, selectedOption, onSelect, onDismiss
        )
    }
}

// 2. The Beautiful Content
@Composable
fun <T> SimpleSelectionDialogContent(
    title: String,
    cancelLabel: String,
    options: List<Pair<T, String>>,
    selectedOption: T?,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCustomColors.current

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {

            // Header
            Text(
                text = title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = colors.textPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Modern Selectable List
            options.forEach { (value, label) ->
                val isSelected = selectedOption == value

                // Smooth Color Animations
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary.copy(alpha = 0.1f) else Color.Transparent,
                    animationSpec = tween(300),
                    label = "bg_color"
                )
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else colors.divider.copy(alpha = 0.5f),
                    animationSpec = tween(300),
                    label = "border_color"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) colors.primary else colors.textPrimary,
                    animationSpec = tween(300),
                    label = "text_color"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(value); onDismiss() }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 16.sp
                        )

                        // Custom animated selection indicator instead of boring RadioButton
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    color = if (isSelected) colors.primary else Color.Transparent,
                                    shape = CircleShape
                                ), contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                // Uses your custom icon, or standard check mark
                                Icon(
                                    painter = AppIcons.Check, // Assuming you have a check icon
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Actions
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(
                    onClick = onDismiss, shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = cancelLabel,
                        color = colors.textSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// 3. The Preview
@Preview(showBackground = true, locale = "fa")
@Composable
fun PreviewSimpleSelectionDialog() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            SimpleSelectionDialogContent(
                title = "انتخاب موتور جستجو",
                cancelLabel = "انصراف",
                options = listOf(
                    "shaadbin" to "شادبین (Shaadbin)",
                    "kiddle" to "کیدل (Kiddle)",
                    "duckduckgo" to "داک‌داک‌گو (DuckDuckGo)",
                ),
                selectedOption = "shaadbin",
                onSelect = {},
                onDismiss = {})
        }
    }
}