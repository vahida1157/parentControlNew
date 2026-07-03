package com.vahak.mehrban.core.analytics

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAnalytics @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics
) {
    /** Enable or disable all Firebase Analytics collection */
    fun setAnalyticsCollectionEnabled(isEnabled: Boolean) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(isEnabled)
    }

    /** Record where the app was downloaded from */
    fun setInstallSource(source: String) {
        firebaseAnalytics.setUserProperty("install_source", source)
    }

    /** Tie the anonymous device to a specific user */
    fun setUserId(userId: String) {
        firebaseAnalytics.setUserId(userId)
    }

    /** Automatically track every Compose screen change */
    fun logScreenView(screenName: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenName)
        }
    }

    /** OTP Sent successfully */
    fun logOtpRequested(phoneLength: Int) {
        firebaseAnalytics.logEvent("otp_requested") {
            param(
                "phone_length", phoneLength.toLong()
            )
        }
    }

    /** OTP Verified */
    fun logLoginSuccess(isNewSession: Boolean) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN) {
            param(FirebaseAnalytics.Param.METHOD, "otp")
            param("is_new_session", isNewSession.toString())
        }
    }

    /** Dashboard: Protection activated */
    fun logProtectionActivated() {
        firebaseAnalytics.logEvent("protection_activated", null)
    }

    /** Add Child: Track creation without PII */
    fun logChildAdded(childId: String) {
        firebaseAnalytics.logEvent("child_added") {
            param("child_id", childId)
        }
    }

    /** Time Limit: Track how strict parents are being */
    fun logTimeLimitSet(totalMinutes: Int, isWarningEnabled: Boolean) {
        firebaseAnalytics.logEvent("time_limit_configured") {
            param("limit_minutes", totalMinutes.toLong())
            param("warning_enabled", isWarningEnabled.toString())
        }
    }

    /** Sleep Time: Track how parents configure bedtime */
    fun logSleepTimeConfigured(isActive: Boolean, startHour: Int, endHour: Int) {
        firebaseAnalytics.logEvent("sleep_time_configured") {
            param("is_active", isActive.toString())
            if (isActive) {
                param("start_hour", startHour.toLong())
                param("end_hour", endHour.toLong())
            }
        }
    }

    fun logChildDeleted() {
        firebaseAnalytics.logEvent("child_profile_deleted", null)
    }

    fun logLauncherExited(method: String) {
        firebaseAnalytics.logEvent("launcher_exited") {
            param("method", method) // e.g., "pin", "recovery"
        }
    }

    /** Permission Manager: Track when user starts resolving permissions */
    fun logPermissionSetupStarted(featureName: String, missingCount: Int) {
        firebaseAnalytics.logEvent("permission_setup_started") {
            param("feature_name", featureName)
            param("missing_count", missingCount.toLong())
        }
    }

    /** Permission Manager: Track when user clicks "Grant" to go to OS Settings */
    fun logPermissionStepClicked(permissionName: String) {
        firebaseAnalytics.logEvent("permission_step_clicked") {
            param("permission_name", permissionName)
        }
    }

    /** Permission Manager: Track when the app detects the permission was ACTUALLY granted */
    fun logPermissionStepSuccess(permissionName: String) {
        firebaseAnalytics.logEvent("permission_step_success") {
            param("permission_name", permissionName)
        }
    }

    /** Permission Manager: Track when all permissions are done and they enter the feature */
    fun logPermissionSetupFinished(featureName: String) {
        firebaseAnalytics.logEvent("permission_setup_completed") {
            param("feature_name", featureName)
        }
    }

    fun logAppRulesSaved(blockedCount: Int, allowedCount: Int) {
        firebaseAnalytics.logEvent("app_rules_saved") {
            param("total_blocked", blockedCount.toLong())
            param("total_allowed", allowedCount.toLong())
        }
    }

    fun logSecuritySetupCompleted() {
        firebaseAnalytics.logEvent("security_setup_completed", null)
    }
}