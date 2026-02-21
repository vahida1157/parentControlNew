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
import com.vahak.parentcontroll.ui.component.LoginScreen
import com.vahak.parentcontroll.ui.component.ModernFamilyDashboard
import com.vahak.parentcontroll.ui.component.OtpScreen
import com.vahak.parentcontroll.ui.component.ParentalSettingsScreen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

@Composable
fun ParentControlNavGraph(
    modifier: Modifier = Modifier, navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController, startDestination = Screen.Login.route, modifier = modifier
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
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
            )
        }
        composable(route = Screen.AddChild.route) {
            AddChildScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(route = Screen.Settings.route) {
            ParentalSettingsScreen(
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

@Preview(showBackground = true, locale = "fa", name = "Main App Flow")
@Composable
fun AppNavigationPreview() {
    ParentControlTheme {
        // Passing a dummy NavController allows the preview to render the NavHost
        ParentControlNavGraph(navController = rememberNavController())
    }
}