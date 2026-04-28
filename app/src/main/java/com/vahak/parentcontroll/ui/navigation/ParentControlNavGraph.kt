package com.vahak.parentcontroll.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vahak.parentcontroll.ui.screens.AddChildScreen
import com.vahak.parentcontroll.ui.screens.AppSelectionScreen
import com.vahak.parentcontroll.ui.screens.MainParentScreen
import com.vahak.parentcontroll.ui.screens.TimeLimitScreen
import com.vahak.parentcontroll.ui.screens.bedtime.BedtimeScreen
import com.vahak.parentcontroll.ui.screens.family.FamilyManagementScreen
import com.vahak.parentcontroll.ui.screens.login.LoginScreen
import com.vahak.parentcontroll.ui.screens.login.OtpScreen
import com.vahak.parentcontroll.ui.screens.password.PasswordManagementScreen
import com.vahak.parentcontroll.ui.screens.permissions.PermissionSliderScreen
import com.vahak.parentcontroll.ui.screens.report.UsageReportScreen
import com.vahak.parentcontroll.ui.screens.settings.ChildSettingsScreen
import com.vahak.parentcontroll.ui.screens.settings.SiteManagementScreen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ParentControlNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onDisableLauncherRequested: () -> Unit,
) {
    NavHost(
        navController = navController, startDestination = startDestination, modifier = modifier
    ) {

        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToOtp = { phone ->
                    if (phone.isNotBlank()) {
                        navController.navigate(Screen.Otp.createRoute(phone))
                    }
                })
        }

        composable(
            route = Screen.Otp.route,
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""

            OtpScreen(
                phoneNumber = phoneNumber,
                onBackClick = { navController.popBackStack() },
                onVerifyClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                })
        }

        composable(route = Screen.Dashboard.route) {
            MainParentScreen(
                rootNavController = navController, onLogoutComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                })
        }

        composable(route = Screen.AddChild.route) {
            AddChildScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(route = Screen.FamilyManagement.route) {
            FamilyManagementScreen(
                onBackClick = { navController.popBackStack() },
                onAddChildClick = { navController.navigate(Screen.AddChild.route) },
                onChildSettingsClick = { childId ->
                    navController.navigate(Screen.ChildSettings.createRoute(childId))
                })
        }

        composable(route = Screen.ChildSettings.route) { backStackEntry ->
            val currentChildId = backStackEntry.arguments?.getString("childId") ?: ""

            ChildSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToFeature = { featureRoute ->
                    navController.navigate("$featureRoute/$currentChildId")
                },
                onInterceptForPermissions = { route, missing ->
                    val missingString = missing.joinToString(",")
                    navController.navigate(
                        Screen.PermissionSlider.createRoute(
                            route, currentChildId, missingString
                        )
                    )
                })
        }

        composable(route = Screen.TimeLimit.route) {
            TimeLimitScreen(
                onBackClick = { navController.popBackStack() })
        }

        composable(
            route = Screen.PermissionSlider.route,
            arguments = listOf(
                navArgument("featureRoute") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType },
                navArgument("missingPermissions") {
                    type = NavType.StringType
                })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val featureRoute = backStackEntry.arguments?.getString("featureRoute") ?: ""

            PermissionSliderScreen(
                onNavigateToFeature = { targetRoute ->
                    val finalRoute = if (childId == "global") {
                        targetRoute // Just go exactly to the return address provided
                    } else {
                        "$targetRoute/$childId" // Otherwise, it's a child feature, append the ID
                    }
                    navController.navigate(finalRoute) {
                        popUpTo(Screen.PermissionSlider.route) {
                            inclusive = true
                        }
                    }
                })
        }

        composable(route = Screen.AppLock.route) {
            AppSelectionScreen(
                onBackClick = { navController.popBackStack() })
        }

        composable(route = Screen.PasswordManagement.route) {
            PasswordManagementScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToDashboard = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                })
        }

        composable(route = Screen.UsageReport.route) {
            UsageReportScreen(
                onBackClick = { navController.popBackStack() })
        }

        composable(route = Screen.SleepTime.route) {
            BedtimeScreen(
                onBackClick = { navController.popBackStack() })
        }

        composable(
            route = "site_management/{childId}",
            arguments = listOf(navArgument("childId") { type = NavType.StringType })
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""

            SiteManagementScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Main App Flow")
@Composable
fun AppNavigationPreview() {
    ParentControlTheme {
        ParentControlNavGraph(
            startDestination = Screen.Login.route,
            onDisableLauncherRequested = {},
        )
    }
}