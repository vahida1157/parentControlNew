package com.vahak.parentcontroll.core.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.edit
import com.vahak.parentcontroll.core.data.local.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RestrictionAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var sessionManager: SessionManager

    companion object {
        private const val TAG = "RestrictionAccService"
    }

    // Coroutine Scope for the background service
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    // State Trackers
    private var isProtectionActive = false // 🚀 ZERO-LATENCY MEMORY FLAG
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
        Log.i(TAG, "🛡️ Accessibility Service Connected!")

        // 🚀 PRO FIX: Listen to the SessionManager in the background
        serviceScope.launch {
            sessionManager.activeChildIdFlow.collectLatest { activeId ->
                isProtectionActive = activeId != null
                Log.d(TAG, "🔄 Protection State Changed: $isProtectionActive")
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 🚀 HARD GATE: If protection is OFF, ignore everything instantly. Do not waste CPU.
        if (!isProtectionActive) return

        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        if (packageName == currentForegroundApp) return

        currentForegroundApp = packageName

        val prefs = getSharedPreferences("security_prefs", Context.MODE_PRIVATE)
        val isBridgeOpen = prefs.getBoolean("settings_bridge_open", false)
        val currentTime = System.currentTimeMillis()

        if (isBridgeOpen && currentForegroundApp != "com.android.settings") {
            Log.d(TAG, "💥 Secure chain broken! Destroying the Settings Bridge.")
            prefs.edit { putBoolean("settings_bridge_open", false) }
        }

        if (currentForegroundApp == "com.android.settings") {
            if (isBridgeOpen) return

            if (currentTime - lastHomePressTime > 500) {
                Log.w(TAG, "🚨 UNAUTHORIZED SETTINGS ACCESS: Kicking to Home.")
                val homePressed = performGlobalAction(GLOBAL_ACTION_HOME)
                if (homePressed) {
                    lastHomePressTime = currentTime
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "⚠️ Accessibility Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel() // Clean up coroutines to prevent memory leaks!
    }
}