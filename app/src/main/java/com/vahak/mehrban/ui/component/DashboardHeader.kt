package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme


@Composable
fun DashboardHeader(
    onHelpClick: () -> Unit,
    onUnlockClick: () -> Unit,
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(colors.primary, colors.secondary),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                ), shape = RoundedCornerShape(bottomStart = 45.dp, bottomEnd = 45.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.05f))
                .clip(RoundedCornerShape(bottomStart = 45.dp, bottomEnd = 45.dp))
        )
        Row(
            modifier = Modifier
                .padding(top = 40.dp, start = 25.dp, end = 25.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            GlassIconButton(icon = AppIcons.LockBadge, onClick = onUnlockClick)
            GlassIconButton(icon = AppIcons.Help, onClick = onHelpClick)
        }
    }
}

@Composable
fun GlassIconButton(icon: Painter, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                color = Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(12.dp)
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() }, contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true, name = "1. Dashboard Header", widthDp = 360, locale = "fa")
@Composable
fun DashboardHeaderPreview() {
    ParentControlTheme {
        DashboardHeader(onHelpClick = {}, onUnlockClick = {})
    }
}