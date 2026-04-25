package com.vahak.parentcontroll.core.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RestrictionAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RestrictionAccService"
    }

    // 🚀 PRO FIX: The Cooldown Tracker
    private var lastHomePressTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
        this.serviceInfo = info
        Log.i(TAG, "🛡️ Accessibility Service Connected and Programmatically Configured!")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            Log.d(TAG, "📱 Window Changed: $packageName")

            if (packageName == "com.android.settings") {
                val currentTime = System.currentTimeMillis()

                // It swallows the 37ms glitch and prevents the 300ms camera double-tap.
                // But it is physically impossible for a human to see the home screen,
                // find the Settings icon, and tap it again in under half a second.
                if (currentTime - lastHomePressTime > 500) {
                    Log.w(TAG, "🚨 BLOCKING PRIORITY 0: Settings detected. Kicking to Home.")

                    val homePressed = performGlobalAction(GLOBAL_ACTION_HOME)

                    if (homePressed) {
                        Log.d(TAG, "✅ System Home action executed successfully.")
                        lastHomePressTime = currentTime // Reset the cooldown timer
                    } else {
                        Log.e(TAG, "❌ Failed to execute Home action.")
                    }
                } else {
                    Log.v(TAG, "⏳ Ignoring duplicate Settings event (Cooldown active)")
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "⚠️ Accessibility Service Interrupted")
    }
}