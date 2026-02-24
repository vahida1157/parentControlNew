package com.vahak.parentcontroll.core.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class TimeLockOverlay(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var lockView: ViewGroup? = null
    private var isShowing = false

    fun show() {
        if (isShowing) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create a programmatic View (You can replace this with a LayoutInflater inflating an XML layout later!)
        lockView = LinearLayout(context).apply {
            setBackgroundColor(Color.parseColor("#E53935")) // Red background
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL


            setOnClickListener { hide() }


            val text = TextView(context).apply {
                this.text = "زمان استفاده شما به پایان رسید!"
                this.textSize = 24f
                this.setTextColor(Color.WHITE)
                // Set custom font here if needed
            }
            addView(text)
        }

        // Extremely aggressive Window Parameters to prevent bypassing
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(lockView, layoutParams)
        isShowing = true
    }

    fun hide() {
        if (!isShowing) return
        try {
            windowManager?.removeView(lockView)
            isShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}