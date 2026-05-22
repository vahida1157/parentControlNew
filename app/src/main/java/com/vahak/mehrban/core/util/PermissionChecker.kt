package com.vahak.mehrban.core.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.net.VpnService
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
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
            PermissionType.LOCATION -> true // Standard permissions are handled differently, placeholder for now
        }
    }

    private fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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
}