package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

data class BottomNavItem(val title: String, val icon: Painter, val route: String)

@Composable
fun DashboardBottomNav(navController: NavController) {
    val colors = LocalCustomColors.current

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // PRO TIP: Define items outside recomposition, or use `remember`
    // so we don't allocate new lists in memory every time a frame draws.
    val items = listOf(
        BottomNavItem("تنظیمات", AppIcons.Settings, "application_settings"),
        BottomNavItem("آمار", AppIcons.ChartPie, "stats"),
        BottomNavItem("اشتراک", AppIcons.Wallet, "subscription"),
        BottomNavItem("خانه", AppIcons.Home, "home")
    )

    // Wrap in a box matching the background color to fill the bottom corners
    Box(modifier = Modifier.background(colors.background)) {
        Card(
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            elevation = CardDefaults.cardElevation(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            NavigationBar(
                containerColor = Color.Transparent, tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route

                    NavigationBarItem(
                        selected = isSelected, onClick = {
                            if (currentRoute != item.route) {
                                navController.navigate(item.route) {
                                    navController.graph.startDestinationRoute?.let { route ->
                                        popUpTo(route) { saveState = true }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }, icon = {
                            Icon(
                                painter = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp)
                            )
                        }, label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }, colors = NavigationBarItemDefaults.colors(
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
            // Pass a dummy navController for the preview
            DashboardBottomNav(rememberNavController())
        }
    }
}