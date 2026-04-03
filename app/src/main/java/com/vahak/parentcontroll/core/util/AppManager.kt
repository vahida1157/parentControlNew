package com.vahak.parentcontroll.core.util

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// The data we need to show the app on screen
data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

object AppManager {
    /**
     * Scans the system for all apps that have a "Launcher" icon.
     * We run this on the IO dispatcher because loading icons can be heavy!
     */
    suspend fun getInstalledApps(context: Context): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        
        // We only want apps that can actually be opened (Launchers)
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList = packageManager.queryIntentActivities(intent, 0)
        
        resolveInfoList.mapNotNull { resolveInfo ->
            try {
                AppInfo(
                    name = resolveInfo.loadLabel(packageManager).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            } catch (e: Exception) {
                null // Skip apps that fail to load
            }
        }
        .filter { it.packageName != context.packageName } // Don't show our own app in the kid's drawer!
        .sortedBy { it.name }
    }

    /**
     * Opens the app when the child taps the icon.
     */
    fun launchApp(context: Context, packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}