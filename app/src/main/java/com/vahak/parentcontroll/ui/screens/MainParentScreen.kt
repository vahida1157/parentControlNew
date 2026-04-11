package com.vahak.parentcontroll.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vahak.parentcontroll.ui.component.DashboardBottomNav
import com.vahak.parentcontroll.ui.navigation.Screen
import com.vahak.parentcontroll.ui.screens.dashboard.ModernFamilyDashboard
import com.vahak.parentcontroll.ui.screens.settings.ApplicationSettingsScreen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun MainParentScreen(
    rootNavController: NavHostController,
    onLogoutComplete: () -> Unit,
) {
    val bottomNavController = rememberNavController()
    ParentControlTheme {
        Scaffold(
            bottomBar = { DashboardBottomNav(navController = bottomNavController) }) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {

                NavHost(
                    navController = bottomNavController,
                    startDestination = "home",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Tab 1: Home (Dashboard)
                    composable("home") {
                        ModernFamilyDashboard(
                            onAddChildClick = { rootNavController.navigate(Screen.AddChild.route) },
                            onManageFamilyClick = { rootNavController.navigate(Screen.FamilyManagement.route) },
                            onSettingsClick = { childId -> rootNavController.navigate(Screen.ChildSettings.createRoute(childId)) },
                            onReportClick = { childId -> rootNavController.navigate(Screen.UsageReport.createRoute(childId)) },
                            onNavigateToPasswordSetup = { rootNavController.navigate(Screen.PasswordManagement.route) },
                            onLogoutComplete = onLogoutComplete
                        )
                    }

                    // Tab 2: Subscription (Placeholder for now)
                    composable("subscription") {
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // Tab 3: Stats (Placeholder for now)
                    composable("stats") {
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    // Tab 4: Parent Settings
                    composable("application_settings") {
                        ApplicationSettingsScreen(
                            onNavigateToPasswordManagement = {
                                rootNavController.navigate(Screen.PasswordManagement.route)
                            }, onLogoutComplete = onLogoutComplete
                        )
                    }
                }
            }
        }
    }
}