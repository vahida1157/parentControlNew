package com.vahak.parentcontroll.core.util

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

object PermissionChecker {

    fun hasPermission(context: Context, type: PermissionType): Boolean {
        return when (type) {
            PermissionType.USAGE_STATS -> hasUsageStatsPermission(context)
            PermissionType.OVERLAY -> hasOverlayPermission(context)
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
}