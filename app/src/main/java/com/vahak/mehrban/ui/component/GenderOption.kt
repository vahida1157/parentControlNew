package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

@Composable
fun GenderOption(
    isSelected: Boolean,
    title: String,
    icon: Painter,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    // Determine dynamic colors based on selection state
    val borderColor = if (isSelected) activeColor else colors.divider
    val backgroundColor = if (isSelected) activeColor.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (isSelected) activeColor else colors.textSecondary

    Box(
        modifier = modifier
            .height(56.dp)
            .background(backgroundColor, RoundedCornerShape(18.dp))
            .border(2.dp, borderColor, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = contentColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Preview(showBackground = true, name = "Gender Options Grid", locale = "fa")
@Composable
fun GenderOptionPreview() {
    ParentControlTheme {
        val colors = LocalCustomColors.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selected Girl Option
            GenderOption(
                isSelected = true,
                title = "دختر",
                icon = AppIcons.Female,
                activeColor = colors.red,
                modifier = Modifier.weight(1f),
                onClick = {}
            )

            // Unselected Boy Option
            GenderOption(
                isSelected = false,
                title = "پسر",
                icon = AppIcons.Male,
                activeColor = colors.blue,
                modifier = Modifier.weight(1f),
                onClick = {}
            )
        }
    }
}