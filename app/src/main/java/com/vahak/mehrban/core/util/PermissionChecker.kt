package com.vahak.mehrban.core.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.vahak.mehrban.core.receiver.SecurityAdminReceiver
import com.vahak.mehrban.core.service.RestrictionAccessibilityService

object PermissionChecker {

    fun hasPermission(context: Context, type: PermissionType): Boolean {
        return when (type) {
            PermissionType.USAGE_STATS -> hasUsageStatsPermission(context)
            PermissionType.OVERLAY -> hasOverlayPermission(context)
            PermissionType.DEVICE_ADMIN -> hasDeviceAdminPermission(context)
            PermissionType.ACCESSIBILITY -> hasAccessibilityPermission(context)
            PermissionType.VPN -> hasVpnPermission(context)
            PermissionType.NOTIFICATIONS -> hasNotificationPermission(context)
            PermissionType.LOCATION -> true // Standard permissions are handled differently, placeholder for now
        }
    }

    @Suppress("DEPRECATION")
    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    private fun hasDeviceAdminPermission(context: Context): Boolean {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(context, SecurityAdminReceiver::class.java)
        return dpm.isAdminActive(adminComponent)
    }

    private fun hasAccessibilityPermission(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabledServices.any {
            it.resolveInfo.serviceInfo.name == RestrictionAccessibilityService::class.java.name
        }
    }

    private fun hasVpnPermission(context: Context): Boolean {
        return VpnService.prepare(context) == null
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ requires explicit runtime permission
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 8.0 to 12.0: Check if the user has disabled the app's notifications in system settings
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.areNotificationsEnabled()
        }
    }
}