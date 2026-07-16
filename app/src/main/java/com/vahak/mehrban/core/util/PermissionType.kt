package com.vahak.mehrban.core.util

import android.app.admin.DevicePolicyManager
import android.provider.Settings
import com.vahak.mehrban.R

enum class PermissionType(
    val titleRes: Int,
    val descRes: Int,
    val instructionResIds: List<Int>,
    val androidSettingsAction: String
) {
    USAGE_STATS(
        titleRes = R.string.permission_usage_stats_title,
        descRes = R.string.permission_usage_stats_desc,
        instructionResIds = listOf(
            R.string.permission_usage_stats_step1,
            R.string.permission_usage_stats_step2,
            R.string.permission_usage_stats_step3
        ),
        androidSettingsAction = Settings.ACTION_USAGE_ACCESS_SETTINGS
    ),
    OVERLAY(
        titleRes = R.string.permission_overlay_title,
        descRes = R.string.permission_overlay_desc,
        instructionResIds = listOf(
            R.string.permission_overlay_step1,
            R.string.permission_overlay_step2,
            R.string.permission_overlay_step3
        ),
        androidSettingsAction = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
    ),
    DEVICE_ADMIN(
        titleRes = R.string.permission_device_admin_title,
        descRes = R.string.permission_device_admin_desc,
        instructionResIds = listOf(
            R.string.permission_device_admin_step1,
            R.string.permission_device_admin_step2,
            R.string.permission_device_admin_step3
        ),
        androidSettingsAction = DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN
    ),
    ACCESSIBILITY(
        titleRes = R.string.permission_accessibility_title,
        descRes = R.string.permission_accessibility_desc,
        instructionResIds = listOf(
            R.string.permission_accessibility_step1,
            R.string.permission_accessibility_step2,
            R.string.permission_accessibility_step3
        ),
        androidSettingsAction = Settings.ACTION_ACCESSIBILITY_SETTINGS
    ),
    VPN(
        titleRes = R.string.permission_vpn_title,
        descRes = R.string.permission_vpn_desc,
        instructionResIds = listOf(
            R.string.permission_vpn_step1,
            R.string.permission_vpn_step2,
            R.string.permission_vpn_step3
        ),
        androidSettingsAction = "ACTION_REQUEST_VPN" // custom flag
    ),
    LOCATION(
        titleRes = R.string.permission_location_title,
        descRes = R.string.permission_location_desc,
        instructionResIds = listOf(
            R.string.permission_location_step1,
            R.string.permission_location_step2,
            R.string.permission_location_step3
        ),
        androidSettingsAction = Settings.ACTION_LOCATION_SOURCE_SETTINGS
    ),
    NOTIFICATIONS(
        titleRes = R.string.permission_notifications_title,
        descRes = R.string.permission_notifications_desc,
        instructionResIds = listOf(R.string.permission_notifications_step_1),
        androidSettingsAction = Settings.ACTION_APP_NOTIFICATION_SETTINGS
    )
}