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
import com.vahak.parentcontroll.ui.component.AddChildScreen
import com.vahak.parentcontroll.ui.screens.TimeLimitScreen
import com.vahak.parentcontroll.ui.screens.dashboard.ModernFamilyDashboard
import com.vahak.parentcontroll.ui.screens.family.FamilyManagementScreen
import com.vahak.parentcontroll.ui.screens.launcher.ChildLauncherScreen
import com.vahak.parentcontroll.ui.screens.login.LoginScreen
import com.vahak.parentcontroll.ui.screens.login.OtpScreen
import com.vahak.parentcontroll.ui.screens.permissions.PermissionSliderScreen
import com.vahak.parentcontroll.ui.screens.settings.ChildSettingsScreen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ParentControlNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    onDisableLauncherRequested: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
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
            ModernFamilyDashboard(
                onAddChildClick = { navController.navigate(Screen.AddChild.route) },
                onSettingsClick = { childId ->
                    navController.navigate("child_settings/$childId")
                },
                onManageFamilyClick = { navController.navigate(Screen.FamilyManagement.route) },
                onLogoutComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                },
            )
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
                    navController.navigate("child_settings/$childId")
                }
            )
        }

        composable(route = "child_settings/{childId}") { backStackEntry ->
            val currentChildId = backStackEntry.arguments?.getString("childId") ?: ""

            ChildSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToFeature = { featureRoute ->
                    navController.navigate("$featureRoute/$currentChildId")
                },
                onInterceptForPermissions = { route, missing ->
                    val missingString = missing.joinToString(",")
                    navController.navigate("permission_slider/$route/$currentChildId/$missingString")
                }
            )
        }

        composable(route = "time_limit/{childId}") { backStackEntry ->
            TimeLimitScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // We store the route pattern in a variable to avoid typos in the popUpTo later
        val permissionRoutePattern =
            "permission_slider/{featureRoute}/{childId}/{missingPermissions}"

        composable(
            route = permissionRoutePattern,
            arguments = listOf(
                navArgument("featureRoute") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType },
                navArgument("missingPermissions") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val featureRoute = backStackEntry.arguments?.getString("featureRoute") ?: ""

            PermissionSliderScreen(
                onNavigateToFeature = { targetRoute ->
                    navController.navigate("$targetRoute/$childId") {
                        // PRO FIX: Using the variable guarantees it matches exactly
                        popUpTo(permissionRoutePattern) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(route = "child_launcher") {
            ChildLauncherScreen(
                onExitLauncherClick = {
                    // Trigger the OS disable request in MainActivity
                    onDisableLauncherRequested()
                }
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Main App Flow")
@Composable
fun AppNavigationPreview() {
    ParentControlTheme {
        ParentControlNavGraph(
            startDestination = "login",
            onDisableLauncherRequested = {},
        )
    }
}