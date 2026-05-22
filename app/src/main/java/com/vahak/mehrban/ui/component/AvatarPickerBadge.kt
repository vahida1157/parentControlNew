package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors

@Composable
fun AvatarPickerBadge(
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    Box(
        modifier = Modifier
            .size(110.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(colors.divider, colors.background)))
                .border(5.dp, colors.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = AppIcons.CameraAlt,
                contentDescription = "Upload Photo",
                tint = colors.textHint,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(30.dp)
                .align(Alignment.BottomStart)
                .offset(x = 5.dp, y = (-5).dp)
                .background(colors.primary, CircleShape)
                .border(2.dp, colors.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = AppIcons.Add,
                contentDescription = "Add",
                tint = colors.surface,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}