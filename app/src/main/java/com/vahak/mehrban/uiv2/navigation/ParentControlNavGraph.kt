package com.vahak.mehrban.uiv2.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.ui.screens.settings.SiteManagementScreen
import com.vahak.mehrban.uiv2.screens.MainParentScreen
import com.vahak.mehrban.uiv2.screens.addchild.AddChildScreen
import com.vahak.mehrban.uiv2.screens.applimit.AppSelectionScreen
import com.vahak.mehrban.uiv2.screens.browser.SafeBrowserScreen
import com.vahak.mehrban.uiv2.screens.browser.BrowserSettingsScreen
import com.vahak.mehrban.uiv2.screens.family.FamilyManagementScreen
import com.vahak.mehrban.uiv2.screens.login.LoginScreen
import com.vahak.mehrban.uiv2.screens.login.OtpScreen
import com.vahak.mehrban.uiv2.screens.notification.NotificationScreen
import com.vahak.mehrban.uiv2.screens.password.PasswordManagementScreen
import com.vahak.mehrban.uiv2.screens.permissions.PermissionSliderScreen
import com.vahak.mehrban.uiv2.screens.report.UsageReportScreen
import com.vahak.mehrban.uiv2.screens.settings.ChildSettingsScreen
import com.vahak.mehrban.uiv2.screens.sleeptime.SleepTimeScreen
import com.vahak.mehrban.uiv2.screens.timelimit.TimeLimitScreen

@Composable
fun ParentControlNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    analytics: AppAnalytics,
) {
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            destination.route?.let { route ->
                val cleanScreenName = route.substringBefore("/")
                analytics.logScreenView(cleanScreenName)
            }
        }
    }

    NavHost(
        navController = navController, startDestination = startDestination, modifier = modifier
    ) {

        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToOtp = { phone, expiresInSeconds ->
                    if (phone.isNotBlank()) {
                        navController.navigate(Screen.Otp.createRoute(phone, expiresInSeconds))
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
                },
                onNavigateToPasswordSetup = { navController.navigate(Screen.PasswordManagement.route) })
        }

        composable(route = Screen.Dashboard.route) {
            MainParentScreen(
                rootNavController = navController,
                analytics = analytics,
                onLogoutComplete = {
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
                onChildSettingsClick = {
                    navController.navigate(Screen.ChildSettings.route)
                })
        }

        composable(route = Screen.ChildSettings.route) {
            ChildSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToFeature = { featureRoute ->
                    navController.navigate(featureRoute)
                },
                onInterceptForPermissions = { route, missing ->
                    val missingString = missing.joinToString(",")
                    navController.navigate(
                        Screen.PermissionSlider.createRoute(route, missingString)
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
                navArgument("missingPermissions") { type = NavType.StringType }
            )
        ) { _ ->
            PermissionSliderScreen(
                onNavigateToFeature = { targetRoute ->
                    navController.navigate(targetRoute) {
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

        composable(route = Screen.BrowserSettings.route) {
            BrowserSettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(route = Screen.PasswordManagement.route) {
            PasswordManagementScreen(
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
            SleepTimeScreen(
                onBackClick = { navController.popBackStack() })
        }

        composable(route = "site_management") {
            SiteManagementScreen(
                onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Notifications.route) {
            NotificationScreen(
                onNavigateBack = { navController.popBackStack() })
        }
        composable("safe_browser") {
            SafeBrowserScreen(onCloseClick = { navController.popBackStack() })
        }
    }
}