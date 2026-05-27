package com.vahak.mehrban.core.data.local

import android.content.Context
import androidx.core.content.edit
import com.vahak.mehrban.data.remote.AppVersionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UpdateCacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("update_security_cache", Context.MODE_PRIVATE)

    fun saveUpdateInfo(info: AppVersionDto, lastCheckTime: Long) {
        prefs.edit {
            putInt("latestVersionCode", info.latestVersionCode)
                .putString("latestVersionName", info.latestVersionName)
                .putString("releaseNotes", info.releaseNotes)
                .putString("downloadUrl", info.downloadUrl)
                .putBoolean("isForced", info.isForced)
                .putLong("lastCheckTime", lastCheckTime)
        }
    }

    fun getCachedUpdateInfo(): AppVersionDto? {
        val code = prefs.getInt("latestVersionCode", -1)
        if (code == -1) return null

        return AppVersionDto(
            latestVersionCode = code,
            latestVersionName = prefs.getString("latestVersionName", "") ?: "",
            releaseNotes = prefs.getString("releaseNotes", "") ?: "",
            downloadUrl = prefs.getString("downloadUrl", "") ?: "",
            isForced = prefs.getBoolean("isForced", false)
        )
    }

    fun getLastCheckTime(): Long = prefs.getLong("lastCheckTime", 0L)

    // Remembers if the user clicked "Ignore" for a specific OPTIONAL update
    fun setIgnoredVersion(versionCode: Int) {
        prefs.edit { putInt("ignoredVersionCode", versionCode) }
    }

    fun getIgnoredVersion(): Int = prefs.getInt("ignoredVersionCode", -1)
}