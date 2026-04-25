package com.vahak.parentcontroll.core.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class RestrictionAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "RestrictionAccService"
    }

    // 🚀 PRO FIX: State Trackers
    private var lastHomePressTime = 0L
    private var currentForegroundApp: String = ""

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
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore duplicate OS events to save battery and processing power
        if (packageName == currentForegroundApp) return

        currentForegroundApp = packageName
        Log.d(TAG, "📱 App Switched: $currentForegroundApp")

        // 1. Get the current state of the Bridge from SharedPreferences
        val prefs = getSharedPreferences("security_prefs", MODE_PRIVATE)
        val isBridgeOpen = prefs.getBoolean("settings_bridge_open", false)

        val currentTime = System.currentTimeMillis()

        // 🚀 THE TRAPDOOR: If the bridge is open, but they navigated ANYWHERE else
        // (Home screen, Recent Apps, or back to our app), we instantly destroy the bridge.
        if (isBridgeOpen && currentForegroundApp != "com.android.settings") {
            Log.d(TAG, "💥 Secure chain broken! Destroying the Settings Bridge.")
            prefs.edit().putBoolean("settings_bridge_open", false).apply()
        }

        // Priority 0: Settings Block Logic
        if (currentForegroundApp == "com.android.settings") {

            if (isBridgeOpen) {
                // The parent is safely inside Settings. Let them stay as long as they need.
                Log.v(TAG, "🌉 Bridge is active. Parent is navigating Settings safely.")
                return
            }

            // If the bridge is FALSE, they are blocked immediately.
            // We maintain the 500ms debounce to prevent the Samsung camera double-tap glitch.
            if (currentTime - lastHomePressTime > 500) {
                Log.w(TAG, "🚨 UNAUTHORIZED SETTINGS ACCESS: Kicking to Home.")

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

    override fun onInterrupt() {
        Log.e(TAG, "⚠️ Accessibility Service Interrupted")
    }
}