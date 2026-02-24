package com.vahak.parentcontroll.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Otp : Screen("otp/{phoneNumber}") {
        fun createRoute(phoneNumber: String) = "otp/$phoneNumber"
    }

    object Dashboard : Screen("dashboard")
    object AddChild : Screen("add_child")
    object Settings : Screen("settings")
    object FamilyManagement : Screen("family_management")
}