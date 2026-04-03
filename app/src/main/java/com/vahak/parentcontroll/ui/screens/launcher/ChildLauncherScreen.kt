package com.vahak.parentcontroll.ui.screens.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.vahak.parentcontroll.presentation.launcher.LauncherViewModel
import com.vahak.parentcontroll.ui.component.launcher.AppDrawerBottomSheet
import com.vahak.parentcontroll.ui.component.launcher.LauncherBottomDock
import com.vahak.parentcontroll.ui.component.launcher.LauncherHeader
import com.vahak.parentcontroll.ui.theme.AppIcons
import com.vahak.parentcontroll.ui.theme.LocalCustomColors
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ChildLauncherScreen(
    childName: String = "محمدمهدی",
    viewModel: LauncherViewModel = hiltViewModel(), // Inject the Engine!
    onExitLauncherClick: () -> Unit
) {
    val colors = LocalCustomColors.current

    // UI States
    var isDrawerOpen by remember { mutableStateOf(false) }

    // ViewModel States
    val installedApps by viewModel.installedApps.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val backgroundBrush = Brush.radialGradient(
        colors = listOf(Color(0xFFE8F5E9), colors.background), radius = 1500f
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {

        // --- 1. THE EXIT DOOR ---
        Card(
            shape = RoundedCornerShape(15.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
            modifier = Modifier
                .padding(20.dp)
                .size(45.dp)
                .align(Alignment.TopStart)
                .clickable { onExitLauncherClick() }) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = AppIcons.LockBadge,
                    contentDescription = "Exit",
                    tint = colors.textSecondary
                )
            }
        }

        // --- 2. HEADER ---
        Column(
            modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LauncherHeader(
                childName = childName, modifier = Modifier.padding(top = 80.dp)
            )
            // (Quick Apps Grid will go here in the future)
        }

        // --- 3. BOTTOM DOCK ---
        LauncherBottomDock(
            modifier = Modifier.align(Alignment.BottomCenter),
            onLeftIconClick = { /* Handle Gallery */ },
            onRightIconClick = { /* Handle Phone */ },
            onCenterDrawerClick = { isDrawerOpen = true } // Open the sheet!
        )
    }

    // --- MODALS ---
    if (isDrawerOpen) {
        AppDrawerBottomSheet(
            apps = installedApps,
            isLoading = isLoading,
            onDismiss = { isDrawerOpen = false },
            onAppClick = { packageName ->
                // Launch the real app, then immediately close the drawer
                viewModel.launchApp(packageName)
                isDrawerOpen = false
            })
    }
}

@Preview(showBackground = true, locale = "fa")
@Composable
fun ChildLauncherScreenPreview() {
    ParentControlTheme {
        ChildLauncherScreen(onExitLauncherClick = {})
    }
}