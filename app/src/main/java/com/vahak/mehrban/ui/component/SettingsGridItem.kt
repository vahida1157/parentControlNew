package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

@Composable
fun SettingsGridItem(
    label: String,
    icon: Painter,
    isLocked: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        shape = RoundedCornerShape(20.dp),
        // Fix 1: Material 3 uses CardDefaults.cardElevation
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .height(110.dp)
            .fillMaxWidth()
            .clickable { onClick() },
        // Fix 2: Material 3 uses 'colors' instead of 'backgroundColor'
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            // Lock Badge (Top Start for RTL)
            if (isLocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(Color(0xFFFFF8E1), RoundedCornerShape(8.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        painter = AppIcons.LockBadge,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = colors.yellow
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(colors.cardInnerBG, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = label,
                    // Fix 3: 'caption' is now 'labelMedium' or 'bodySmall' in Material 3
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Grid Item Light", locale = "fa")
@Composable
fun SettingsGridItemPreview() {
    // Wrap in your theme and RTL direction
    ParentControlTheme {
        SettingsGridItem(
            label = "بازی‌ها",
            icon = AppIcons.Games,
            isLocked = true,
            onClick = {}
        )
    }
}