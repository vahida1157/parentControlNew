package com.vahak.parentcontroll.domain.manager

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import java.util.Calendar

class UsageTracker(private val context: Context) {

    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val packageManager = context.packageManager

    /**
     * Helper function to get the exact start time of today (Midnight)
     */
    private fun getStartOfToday(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    /**
     * Helper function to get ONLY the packages of real, user-facing apps.
     * Requires the <queries> block in AndroidManifest.xml.
     */
    private fun getLaunchablePackages(): Set<String> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = packageManager.queryIntentActivities(mainIntent, 0)
        return resolveInfos.map { it.activityInfo.packageName }.toSet()
    }

    /**
     * Returns the TOTAL usage time in minutes for today.
     * Used by the Foreground Service to trigger the lock screen.
     */
    fun getTodayUsageInMinutes(): Int {
        val aggregatedStats = usageStatsManager.queryAndAggregateUsageStats(
            getStartOfToday(),
            System.currentTimeMillis()
        )

        val launchablePackages = getLaunchablePackages()
        var totalTimeMs = 0L

        for ((packageName, usageStats) in aggregatedStats) {
            if (usageStats.totalTimeInForeground > 0 &&
                packageName != context.packageName && // Ignore our own app
                launchablePackages.contains(packageName)
            ) {
                totalTimeMs += usageStats.totalTimeInForeground
            }
        }

        return (totalTimeMs / (1000 * 60)).toInt() // Ms to Minutes
    }

    /**
     * Returns a Map of PackageName -> UsageTime (in minutes).
     * The map is strictly SORTED from highest usage time to lowest.
     * Used for the "App Lock" or "Activity Report" screens.
     */
    fun getUsageByAppInMinutes(): Map<String, Int> {
        val aggregatedStats = usageStatsManager.queryAndAggregateUsageStats(
            getStartOfToday(),
            System.currentTimeMillis()
        )

        val launchablePackages = getLaunchablePackages()
        val appUsageMap = mutableMapOf<String, Int>()

        for ((packageName, usageStats) in aggregatedStats) {
            if (usageStats.totalTimeInForeground > 0 &&
                packageName != context.packageName &&
                launchablePackages.contains(packageName)
            ) {
                val minutesUsed = (usageStats.totalTimeInForeground / (1000 * 60)).toInt()

                // Only include apps that have been used for at least 1 minute
                if (minutesUsed > 0) {
                    appUsageMap[packageName] = minutesUsed
                }
            }
        }

        // Sort the map by values (time) in descending order.
        // Kotlin's toMap() guarantees order preservation by returning a LinkedHashMap.
        return appUsageMap.toList()
            .sortedByDescending { (_, timeInMinutes) -> timeInMinutes }
            .toMap()
    }
}