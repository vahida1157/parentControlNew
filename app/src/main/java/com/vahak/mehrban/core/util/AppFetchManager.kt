package com.vahak.mehrban.core.util

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.vahak.mehrban.presentation.appselection.AppItemUi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppFetchManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun getInstalledApps(): List<AppItemUi> {
        val pm = context.packageManager
        
        // Only get apps that appear in the app drawer
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = pm.queryIntentActivities(intent, 0)

        return resolveInfos.mapNotNull { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            val appName = resolveInfo.loadLabel(pm).toString()
            val drawable = resolveInfo.loadIcon(pm)

            // Convert Android Drawable to Jetpack Compose ImageBitmap safely
            val imageBitmap: ImageBitmap? = try {
                drawable.toBitmap(width = 144, height = 144).asImageBitmap()
            } catch (e: Exception) {
                null // Fallback if the icon is corrupted
            }

            // Exclude our own app from the list so parents can't accidentally block the Parent Control app
            if (packageName == context.packageName) return@mapNotNull null

            AppItemUi(
                packageName = packageName,
                appName = appName,
                isAllowed = false, // Default state, ViewModel will overwrite this with DB data
                iconBitmap = imageBitmap
            )
        }.distinctBy { it.packageName } // Remove duplicates
         .sortedBy { it.appName }       // Alphabetical order
    }
}