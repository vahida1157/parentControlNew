package com.vahak.parentcontroll.ui.component.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun LauncherAppItem(
    modifier: Modifier = Modifier,
    name: String,
    icon: Painter, // We will pass standard icons for now
    iconColor: Color,
    bgColor: Color,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp)) // Ripple effect boundaries
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(75.dp)
                .background(bgColor, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = name,
                tint = iconColor,
                modifier = Modifier.size(35.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            maxLines = 1 // Prevents text from breaking the grid
        )
    }
}

@Preview(showBackground = true, name = "Launcher App Item", locale = "fa")
@Composable
fun LauncherAppItemPreview() {
    ParentControlTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            LauncherAppItem(
                name = "کارتون‌ها",
                icon = AppIcons.Games, // Replace with your AppIcons.Movies later
                iconColor = Color(0xFF9C27B0),
                bgColor = Color(0xFFF3E5F5),
                onClick = {}
            )
        }
    }
}