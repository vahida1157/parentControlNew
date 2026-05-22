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

    // --- Child Specific Features ---
    object ChildSettings : Screen("child_settings/{childId}") {
        fun createRoute(childId: String) = "child_settings/$childId"
    }

    object TimeLimit : Screen("time_limit/{childId}") {
        fun createRoute(childId: String) = "time_limit/$childId"
    }

    object SleepTime : Screen("sleep_time/{childId}") {
        fun createRoute(childId: String) = "sleep_time/$childId"
    }

    object AppLock : Screen("app_lock/{childId}") {
        fun createRoute(childId: String) = "app_lock/$childId"
    }

    object UsageReport : Screen("usage_report/{childId}") {
        fun createRoute(childId: String) = "usage_report/$childId"
    }

    // --- System & Permissions ---
    object PermissionSlider :
        Screen("permission_slider/{featureRoute}/{childId}/{missingPermissions}") {
        fun createRoute(featureRoute: String, childId: String, missingPermissions: String) =
            "permission_slider/$featureRoute/$childId/$missingPermissions"
    }

    object ChildLauncher : Screen("child_launcher")
}