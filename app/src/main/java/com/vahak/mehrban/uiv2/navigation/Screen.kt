package com.vahak.mehrban.uiv2.navigation

sealed class Screen(val route: String) {
    // --- Auth & Setup ---
    object Login : Screen("login")
    object Otp : Screen("otp/{phoneNumber}/{expiresInSeconds}") {
        fun createRoute(phoneNumber: String, expiresInSeconds: Int) =
            "otp/$phoneNumber/$expiresInSeconds"
    }

    object PasswordManagement : Screen("password_management")

    // --- Main Flow ---
    object Dashboard : Screen("dashboard")
    object AddChild : Screen("add_child")
    object Settings : Screen("settings")
    object FamilyManagement : Screen("family_management")
    object ChildSettings : Screen("child_settings")
    object TimeLimit : Screen("time_limit")
    object SleepTime : Screen("sleep_time")
    object AppLock : Screen("app_lock")
    object UsageReport : Screen("usage_report")

    // --- System & Permissions ---
    object PermissionSlider : Screen("permission_slider/{featureRoute}/{missingPermissions}") {
        fun createRoute(featureRoute: String, missingPermissions: String) =
            "permission_slider/$featureRoute/$missingPermissions"
    }

    object Notifications : Screen("notifications_screen")
}