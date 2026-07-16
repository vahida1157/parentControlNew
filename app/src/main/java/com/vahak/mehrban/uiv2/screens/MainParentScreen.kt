package com.vahak.mehrban.uiv2.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.uiv2.components.BottomNavItem
import com.vahak.mehrban.uiv2.components.DashboardBottomNav
import com.vahak.mehrban.uiv2.navigation.Screen
import com.vahak.mehrban.uiv2.screens.dashboard.DashboardScreen
import com.vahak.mehrban.uiv2.screens.family.FamilyManagementScreen
import com.vahak.mehrban.uiv2.screens.report.UsageReportScreen
import com.vahak.mehrban.uiv2.screens.settings.ApplicationSettingsScreen
import com.vahak.mehrban.uiv2.theme.LocalCustomColors

@Composable
fun MainParentScreen(
    rootNavController: NavHostController,
    onLogoutComplete: () -> Unit,
    analytics: AppAnalytics,
) {
    val bottomNavController = rememberNavController()
    val colors = LocalCustomColors.current

    LaunchedEffect(rootNavController) {
        rootNavController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                val cleanScreenName = route.substringBefore("/")
                analytics.logScreenView(cleanScreenName)
            }
        }
    }

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
                        onSettingsClick = {
                            rootNavController.navigate(Screen.ChildSettings.route)
                        },
                        onReportClick = {
                            rootNavController.navigate(Screen.UsageReport.route)
                        },
                        onTimeLockClick = {
                            rootNavController.navigate(Screen.TimeLimit.route)
                        },
                        onNavigateToPasswordSetup = { rootNavController.navigate(Screen.PasswordManagement.route) },
                        onLogoutComplete = onLogoutComplete,
                        onSecurityFabClick = { missingPermissions ->
                            rootNavController.navigate(
                                Screen.PermissionSlider.createRoute(
                                    Screen.Dashboard.route, missingPermissions
                                )
                            )
                        },
                        onProfileClick = {
                            bottomNavController.navigate(BottomNavItem.Profile.route) {
                                popUpTo(bottomNavController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        })
                }

                // Tab 2: Children Management
                composable(BottomNavItem.Children.route) {
                    FamilyManagementScreen(
                        onAddChildClick = { rootNavController.navigate(Screen.AddChild.route) },
                        onChildSettingsClick = {
                            rootNavController.navigate(Screen.ChildSettings.route) // 🚀 FIXED
                        },
                        onBackClick = {})
                }

                // Tab 3: Reports (Wired up to our new screen!)
                composable(BottomNavItem.Reports.route) {
                    UsageReportScreen(
                        onBackClick = {
                            bottomNavController.navigate(BottomNavItem.Home.route) {
                                popUpTo(bottomNavController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        })
                }

                // Tab 4: Parent Profile & Settings
                composable(BottomNavItem.Profile.route) {
                    ApplicationSettingsScreen(
                        onNavigateToPasswordManagement = { rootNavController.navigate(Screen.PasswordManagement.route) },
                        onNavigateToNotifications = { rootNavController.navigate(Screen.Notifications.route) },
                        onLogoutComplete = onLogoutComplete,
                    )
                }
            }
        }
    }
}