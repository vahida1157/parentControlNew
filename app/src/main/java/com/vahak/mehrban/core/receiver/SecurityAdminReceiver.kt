package com.vahak.mehrban.core.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.vahak.mehrban.R

class SecurityAdminReceiver : DeviceAdminReceiver() {

    companion object {
        private const val TAG = "SecurityAdmin"
    }

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i(TAG, "🔒 Device Admin Enabled. Uninstall Protection Active.")
        // TODO: You can update your DataStore here to mark setup as complete
    }

    // 🚀 PRO FIX: The psychological deterrent
    // If the child tries to deactivate it, Android pops up a dialog. 
    // This text will appear inside that Android OS dialog!
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.w(TAG, "⚠️ Deactivation attempted!")
        return context.getString(R.string.device_admin_disable_warning)
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Log.e(TAG, "🔓 Device Admin Disabled! App is now vulnerable.")
        // TODO: Trigger a high-priority push notification to the Parent's phone!
    }
}