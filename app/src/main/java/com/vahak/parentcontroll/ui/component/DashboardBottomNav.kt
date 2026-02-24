package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun DashboardBottomNav() {
    val colors = LocalCustomColors.current

    Card(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        elevation = CardDefaults.cardElevation(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp,
            modifier = Modifier.height(80.dp)
        ) {
            // Helper to create items
            val items = listOf("پروفایل", "آمار", "اشتراک", "خانه")
            val icons = listOf(AppIcons.Profile, AppIcons.ChartPie, AppIcons.Wallet, AppIcons.Home)

            items.forEachIndexed { index, label ->
                val isSelected = index == 3

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { /* Navigate */ },
                    icon = {
                        Icon(
                            painter = icons[index],
                            contentDescription = label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = colors.primary,
                        selectedTextColor = colors.primary,
                        indicatorColor = colors.primary.copy(alpha = 0.15f),
                        unselectedIconColor = Color(0xFF95A5A6),
                        unselectedTextColor = Color(0xFF95A5A6)
                    )
                )
            }
        }
    }
}

@Preview(showBackground = false, name = "4. Bottom Nav", widthDp = 360, locale = "fa")
@Composable
fun DashboardBottomNavPreview() {
    ParentControlTheme {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .background(Color.Gray)
                .padding(top = 20.dp)
        ) {
            DashboardBottomNav()
        }
    }
}