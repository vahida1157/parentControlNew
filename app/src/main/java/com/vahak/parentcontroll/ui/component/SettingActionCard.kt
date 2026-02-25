package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun SettingActionCard(
    headerTitle: String,
    headerIcon: Painter,
    valueText: String,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    // Dynamic Colors based on whether it is enabled or disabled
    val headerBgColor = if (isEnabled) colors.primary else colors.divider
    val headerTextColor = if (isEnabled) Color.White else colors.textSecondary
    val valueTextColor = if (isEnabled) colors.textPrimary else colors.textHint

    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        // Drop shadow only if enabled
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 15.dp)
            .border(1.dp, colors.divider, RoundedCornerShape(15.dp))
            .alpha(if (isEnabled) 1f else 0.6f) // Fades the entire card
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBgColor)
                    .padding(horizontal = 15.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerTitle,
                    color = headerTextColor,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    painter = headerIcon,
                    contentDescription = null,
                    tint = headerTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = isEnabled) { onClick() }
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = AppIcons.ChevronLeft,
                    contentDescription = null,
                    tint = colors.divider,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = valueTextColor
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "1. Action Card - Enabled", locale = "fa")
@Composable
fun SettingActionCardEnabledPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            SettingActionCard(
                headerTitle = "میزان استفاده از گوشی",
                headerIcon = AppIcons.ChartPie,
                valueText = "2 ساعت و 30 دقیقه",
                isEnabled = true,
                onClick = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "2. Action Card - Disabled", locale = "fa")
@Composable
fun SettingActionCardDisabledPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            SettingActionCard(
                headerTitle = "میزان استفاده از گوشی",
                headerIcon = AppIcons.ChartPie,
                valueText = "2 ساعت و 30 دقیقه",
                isEnabled = false,
                onClick = {}
            )
        }
    }
}