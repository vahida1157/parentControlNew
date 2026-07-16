package com.vahak.mehrban.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vahak.mehrban.MainActivity
import com.vahak.mehrban.R
import com.vahak.mehrban.core.analytics.AppAnalytics
import com.vahak.mehrban.core.data.local.SessionManager
import com.vahak.mehrban.core.data.local.dao.AppRuleDao
import com.vahak.mehrban.core.data.local.dao.ChildDao
import com.vahak.mehrban.core.data.local.dao.ChildSettingsDao
import com.vahak.mehrban.core.data.local.dao.SafeBrowserDao
import com.vahak.mehrban.uiv2.navigation.Screen
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.TimeUnit

@HiltWorker
class InactivityWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val sessionManager: SessionManager,
    private val childDao: ChildDao,
    private val childSettingsDao: ChildSettingsDao,
    private val safeBrowserDao: SafeBrowserDao,
    private val appRuleDao: AppRuleDao,
    private val appAnalytics: AppAnalytics,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("--- InactivityWorker WOKE UP ---")
        val daysInactive = inputData.getInt("days_inactive", 1)

        val savedLanguage = sessionManager.appLanguageFlow.firstOrNull() ?: "fa"
        val localizedContext = getLocalizedContext(context, savedLanguage)

        // 🚀 THE FIX: Use ChildDao to see if they have ANY children in their account
        val children = childDao.getAllChildren().firstOrNull() ?: emptyList()
        val targetChildId = children.firstOrNull()?.id
        Timber.d("Parent has ${children.size} children. Target Child ID: $targetChildId")

        val (titleRes, textRes, destinationRoute) = determineSmartNotification(
            targetChildId,
            daysInactive
        )

        showSystemNotification(localizedContext, titleRes, textRes, destinationRoute)
        scheduleNextCheck(daysInactive)

        Timber.d("--- InactivityWorker FINISHED ---")
        return Result.success()
    }

    private suspend fun determineSmartNotification(
        childId: String?,
        daysInactive: Int
    ): Triple<Int, Int, String> {
        val isLoggedIn = sessionManager.isLoggedIn.firstOrNull() ?: false
        Timber.d("Auth Status -> Logged In: $isLoggedIn")

        if (!isLoggedIn) {
            Timber.d("User is not logged in. Routing to Login.")
            // Ensure these string resources exist in your XML
            return Triple(R.string.notif_login_title, R.string.notif_login_text, Screen.Login.route)
        }

        if (childId == null) {
            Timber.d("User logged in but no child found. Routing to AddChild.")
            return Triple(
                R.string.notif_no_child_title,
                R.string.notif_no_child_text,
                Screen.AddChild.route
            )
        }

        if (daysInactive >= 7) {
            Timber.d("User inactive for >= 7 'days' ($daysInactive). Sending general reminder.")
            return getGeneralMessage(daysInactive)
        }

        // --- SMART FEATURE DETECTION ---
        val globalSettings = childSettingsDao.getGlobalSettings(childId).firstOrNull()
        val browserSettings = safeBrowserDao.getSettingsSync(childId)
        val appRules = appRuleDao.observeAllAppRules(childId).firstOrNull() ?: emptyList()

        val isTimeLimitMissing = globalSettings == null || !globalSettings.isTimeLimitActive
        val isSleepTimeMissing = globalSettings == null || !globalSettings.isSleepTimeActive
        val isBrowserMissing =
            browserSettings == null || browserSettings.filterMode.name == "DISABLED"
        val isAppLockMissing = appRules.isEmpty() || appRules.all { it.isAllowed }

        Timber.d("Feature Check -> TimeLimitMissing: $isTimeLimitMissing, SleepTimeMissing: $isSleepTimeMissing, BrowserMissing: $isBrowserMissing, AppLockMissing: $isAppLockMissing")

        return when {
            isBrowserMissing -> Triple(
                R.string.notif_browser_title,
                R.string.notif_browser_text,
                Screen.BrowserSettingMenu.route
            )

            isTimeLimitMissing -> Triple(
                R.string.notif_time_title,
                R.string.notif_time_text,
                Screen.TimeLimit.route
            )

            isAppLockMissing -> Triple(
                R.string.notif_app_title,
                R.string.notif_app_text,
                Screen.AppLock.route
            )

            isSleepTimeMissing -> Triple(
                R.string.notif_sleep_title,
                R.string.notif_sleep_text,
                Screen.SleepTime.route
            )

            else -> {
                Timber.d("All features are configured! Sending general reminder for stage $daysInactive.")
                getGeneralMessage(daysInactive)
            }
        }
    }

    private fun getGeneralMessage(daysInactive: Int): Triple<Int, Int, String> {
        val route = Screen.ChildSettings.route
        return when (daysInactive) {
            1 -> Triple(R.string.notif_gen_day1_title, R.string.notif_gen_day1_text, route)
            3 -> Triple(R.string.notif_gen_day3_title, R.string.notif_gen_day3_text, route)
            else -> Triple(R.string.notif_gen_day7_title, R.string.notif_gen_day7_text, route)
        }
    }

    private fun showSystemNotification(
        localizedContext: Context,
        titleRes: Int,
        textRes: Int,
        destinationRoute: String
    ) {
        Timber.d("Firing NotificationManager for route: $destinationRoute")

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "parent_alerts_smart_channel"

        val channel = NotificationChannel(
            channelId,
            localizedContext.getString(R.string.parent_alerts_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("destination", destinationRoute)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val textString = localizedContext.getString(textRes)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_shield)
            .setContentTitle(localizedContext.getString(titleRes))
            .setContentText(textString)
            .setStyle(NotificationCompat.BigTextStyle().bigText(textString))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1001, builder.build())
        appAnalytics.logInactivityNotificationDelivered(destinationRoute, titleRes.toString())

    }

    private fun scheduleNextCheck(currentDaysInactive: Int) {
        val nextDaysInactive = when (currentDaysInactive) {
            1 -> 3
            3 -> 7
            else -> currentDaysInactive + 3
        }

        val delayToNextCheck = nextDaysInactive - currentDaysInactive
        Timber.d("Scheduling next worker in $delayToNextCheck 'days' (seconds for testing) for stage $nextDaysInactive.")

        val inputData = Data.Builder().putInt("days_inactive", nextDaysInactive).build()
        val workRequest = OneTimeWorkRequestBuilder<InactivityWorker>()
            .setInitialDelay(delayToNextCheck.toLong(), TimeUnit.DAYS)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            NotificationScheduler.INACTIVITY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun getLocalizedContext(context: Context, languageCode: String): Context {
        val locale = Locale.Builder().setLanguage(languageCode).build()
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}