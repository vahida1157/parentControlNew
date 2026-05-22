package com.vahak.mehrban.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors

@Composable
fun ClickableListItem(
    name: String,
    avatarIcon: Painter,
    avatarTint: Color,
    avatarBg: Color,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    val colors = LocalCustomColors.current
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(avatarBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = avatarIcon,
                        contentDescription = null,
                        tint = avatarTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Icon(
                painter = AppIcons.ChevronLeft,
                contentDescription = null,
                tint = colors.divider,
                modifier = Modifier.size(16.dp)
            )
        }
        if (showDivider) {
            HorizontalDivider(
                color = colors.divider,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}