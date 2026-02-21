package com.vahak.parentcontroll.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ModernFamilyDashboard(
    onAddChildClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val colors = LocalCustomColors.current

    // Use Scaffod for BottomBar, but we need custom positioning for FAB
    // So we'll use a Box for the main layout to layer the FAB correctly
    Scaffold(
        bottomBar = {
            DashboardBottomNav()
        },
        containerColor = colors.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Header
                DashboardHeader(onHelpClick = {}, onUnlockClick = {})

                // 2. Main Content Area
                Column(
                    modifier = Modifier.padding(horizontal = 25.dp)
                ) {
                    Spacer(modifier = Modifier.height(35.dp)) // Space for FAB

                    // Menu Items
                    DashboardMenuItem(
                        title = "تنظیمات خانواده",
                        icon = AppIcons.Settings,
                        onClick = { onSettingsClick() }
                    )

                    DashboardMenuItem(
                        title = "گزارش فعالیت‌ها",
                        icon = AppIcons.ChartBar,
                        onClick = {}
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Slider
                    SwipeToActivateButton(onActivate = {})

                    Spacer(modifier = Modifier.height(100.dp)) // Bottom padding
                }
            }

            // 3. FAB (Add Child)
            // Positioned absolute relative to the screen, overlapping header and content
            FloatingActionButton(
                onClick = { onAddChildClick() },
                containerColor = colors.yellow, // Accent Color
                contentColor = Color(0xFF333333),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopStart) // RTL support
                    .padding(top = 310.dp, start = 30.dp) // top: 300px + padding
                    .size(65.dp)
                // Add shadow manually if needed or rely on default elevation
            ) {
                Icon(
                    painter = AppIcons.Add,
                    contentDescription = "Add Child",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    name = "5. Full Dashboard Screen",
    heightDp = 800,
    widthDp = 360,
    locale = "fa"
)
@Composable
fun FullDashboardPreview() {
    ParentControlTheme {
        ModernFamilyDashboard()
    }
}