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
import com.vahak.parentcontroll.ui.screens.login.LoginScreen
import com.vahak.parentcontroll.ui.screens.login.OtpScreen
import com.vahak.parentcontroll.ui.screens.permissions.PermissionSliderScreen
import com.vahak.parentcontroll.ui.screens.settings.ChildSettingsScreen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ParentControlNavGraph(
    startDestination: String,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
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

        // Connect your new dashboard here!
        composable(route = Screen.Dashboard.route) {
            ModernFamilyDashboard(
                onAddChildClick = { navController.navigate(Screen.AddChild.route) },
                onSettingsClick = { childId ->
                    navController.navigate("child_settings/$childId")
                },
                onManageFamilyClick = { navController.navigate(Screen.FamilyManagement.route) },
                onLogoutComplete = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) {
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
                onBackClick = { navController.popBackStack() }, // Go back to Dashboard
                onAddChildClick = { navController.navigate(Screen.AddChild.route) }, // Open Add Child
                onChildSettingsClick = { childId ->
                    // TODO: We will build this next!
                    // navController.navigate("child_settings/$childId")
                }
            )
        }
        composable(route = "child_settings/{childId}") { backStackEntry ->
            // Extract the childId safely
            val currentChildId = backStackEntry.arguments?.getString("childId") ?: ""

            ChildSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToFeature = { featureRoute ->
                    // Standard navigation: Glue the route and ID together
                    navController.navigate("$featureRoute/$currentChildId")
                },
                onInterceptForPermissions = { route, missing ->
                    val missingString = missing.joinToString(",")
                    // Intercept navigation: Pass the childId safely as its own variable!
                    navController.navigate("permission_slider/$route/$currentChildId/$missingString")
                }
            )
        }

        composable(route = "time_limit/{childId}") { backStackEntry ->
            TimeLimitScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        // PRO FIX: Added {childId} as a dedicated argument to avoid the Slash Trap!
        composable(
            route = "permission_slider/{featureRoute}/{childId}/{missingPermissions}",
            arguments = listOf(
                navArgument("featureRoute") { type = NavType.StringType },
                navArgument("childId") { type = NavType.StringType }, // NEW ARGUMENT
                navArgument("missingPermissions") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val childId = backStackEntry.arguments?.getString("childId") ?: ""
            val featureRoute = backStackEntry.arguments?.getString("featureRoute") ?: ""

            PermissionSliderScreen(
                onNavigateToFeature = { targetRoute ->
                    // Once finished, we glue the base route (e.g., "time_limit") and the childId together!
                    navController.navigate("$targetRoute/$childId") {
                        // Pop the slider so they don't see it if they hit the back button
                        popUpTo("permission_slider/{featureRoute}/{childId}/{missingPermissions}") {
                            inclusive = true
                        }
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Main App Flow")
@Composable
fun AppNavigationPreview() {
    ParentControlTheme {
        // Passing a dummy NavController allows the preview to render the NavHost
        ParentControlNavGraph(startDestination = "login", navController = rememberNavController())
    }
}