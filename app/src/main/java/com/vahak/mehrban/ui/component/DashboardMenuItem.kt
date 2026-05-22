package com.vahak.mehrban.ui.component

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.mehrban.ui.theme.AppIcons
import com.vahak.mehrban.ui.theme.LocalCustomColors
import com.vahak.mehrban.ui.theme.ParentControlTheme

@Composable
fun DashboardMenuItem(
    title: String,
    icon: Painter,
    onClick: () -> Unit
) {
    val colors = LocalCustomColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp) // Half of gap 18px
            .height(88.dp) // Approx based on padding
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon Box
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(colors.primary.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(18.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge, // ~1.05rem
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            // Chevron Left (Standard for RTL is pointing left for "Go")
            Icon(
                painter = AppIcons.ChevronLeft, // You need to have this icon
                contentDescription = null,
                tint = Color(0xFFBDC3C7),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true, name = "2. Menu Item", widthDp = 360, locale = "fa")
@Composable
fun DashboardMenuItemPreview() {
    ParentControlTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            DashboardMenuItem(
                title = "تنظیمات خانواده",
                icon = AppIcons.Settings, // Ensure you have this in your AppIcons
                onClick = {}
            )
            Spacer(modifier = Modifier.height(10.dp))
            DashboardMenuItem(
                title = "گزارش فعالیت‌ها",
                icon = AppIcons.ChartBar,
                onClick = {}
            )
        }
    }
}