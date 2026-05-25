package com.vahak.mehrban.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings

object LauncherManager {

    fun enableLauncherMode(context: Context) {
        val packageManager = context.packageManager
        val aliasComponent = ComponentName(
            context.packageName,
            "com.vahak.mehrban.ChildLauncherAlias",
        )

        // 1. Enable the Alias
        packageManager.setComponentEnabledSetting(
            aliasComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // 2. The OS needs a split second to rebuild its internal list of Launchers
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                // THE FIX: Open the official Android "Default Home App" settings page.
                // This bypasses the broken popup and forces MIUI/Samsung to save your choice.
                val intent = Intent(Settings.ACTION_HOME_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                // Absolute fallback just in case the device lacks this specific settings menu
                val fallbackIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallbackIntent)
            }
        }, 300)
    }

    fun disableLauncherMode(context: Context) {
        val packageManager = context.packageManager
        val aliasComponent = ComponentName(
            context.packageName,
            "com.vahak.mehrban.ChildLauncherAlias",
        )

        // Turn the alias off. The phone will instantly drop back to the system launcher.
        packageManager.setComponentEnabledSetting(
            aliasComponent,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}