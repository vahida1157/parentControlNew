package com.vahak.parentcontroll.uiv2.screens

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
import com.vahak.parentcontroll.ui.navigation.Screen
import com.vahak.parentcontroll.uiv2.components.BottomNavItem
import com.vahak.parentcontroll.uiv2.components.DashboardBottomNav
import com.vahak.parentcontroll.uiv2.screens.dashboard.DashboardScreen
import com.vahak.parentcontroll.uiv2.screens.family.FamilyManagementScreen
import com.vahak.parentcontroll.uiv2.screens.report.UsageReportScreen
import com.vahak.parentcontroll.uiv2.screens.settings.ApplicationSettingsScreen
import com.vahak.parentcontroll.uiv2.theme.LocalCustomColors

@Composable
fun MainParentScreen(
    rootNavController: NavHostController,
    onLogoutComplete: () -> Unit,
) {
    val bottomNavController = rememberNavController()
    val colors = LocalCustomColors.current

    Scaffold(
        bottomBar = { DashboardBottomNav(navController = bottomNavController) },
        containerColor = colors.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = bottomNavController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                // Tab 1: Home (Dashboard)
                composable(BottomNavItem.Home.route) {
                    DashboardScreen(
                        onAddChildClick = { rootNavController.navigate(Screen.AddChild.route) },
                        onManageFamilyClick = { rootNavController.navigate(Screen.FamilyManagement.route) },
                        onSettingsClick = { childId ->
                            rootNavController.navigate(Screen.ChildSettings.createRoute(childId))
                        },
                        onReportClick = { childId ->
                            rootNavController.navigate(Screen.UsageReport.createRoute(childId))
                        },
                        onNavigateToPasswordSetup = { rootNavController.navigate(Screen.PasswordManagement.route) },
                        onLogoutComplete = onLogoutComplete,
                        onSecurityFabClick = { missingPermissions ->
                            rootNavController.navigate(
                                Screen.PermissionSlider.createRoute(
                                    Screen.Dashboard.route,
                                    "global",
                                    missingPermissions
                                )
                            )
                        }
                    )
                }

                // Tab 2: Children Management
                composable(BottomNavItem.Children.route) {
                    FamilyManagementScreen(
                        onAddChildClick = { rootNavController.navigate(Screen.AddChild.route) },
                        onChildSettingsClick = { childId ->
                            rootNavController.navigate(Screen.ChildSettings.createRoute(childId))
                        },
                        onBackClick = {}
                    )
                }

                // Tab 3: Reports (Wired up to our new screen!)
                composable(BottomNavItem.Reports.route) {
                    UsageReportScreen(
                        onBackClick = {
                            bottomNavController.navigate(BottomNavItem.Home.route) {
                                popUpTo(bottomNavController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // Tab 4: Parent Profile & Settings
                composable(BottomNavItem.Profile.route) {
                    ApplicationSettingsScreen(
                        onNavigateToPasswordManagement = { rootNavController.navigate(Screen.PasswordManagement.route) },
                        onLogoutComplete = onLogoutComplete
                    )
                }
            }
        }
    }
}