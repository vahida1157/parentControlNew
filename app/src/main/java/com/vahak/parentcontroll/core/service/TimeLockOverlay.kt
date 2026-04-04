package com.vahak.parentcontroll.core.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.toColorInt

class TimeLockOverlay(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var lockView: ViewGroup? = null
    private var isShowing = false

    // Put your exact LocalCustomColors hex codes here!
    private val dangerRed = "#FF5252".toColorInt() // Replace with your colors.red
    private val surfaceColor = "#FFFFFF".toColorInt() // Replace with colors.surface
    private val textPrimary = "#333333".toColorInt()

    fun show() {
        if (isShowing) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        lockView = LinearLayout(context).apply {
            setBackgroundColor(dangerRed) // Using your theme color
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL

            // The Title
            val text = TextView(context).apply {
                this.text = "زمان استفاده شما به پایان رسید!"
                this.textSize = 24f
                this.setTextColor(Color.WHITE)
                this.gravity = Gravity.CENTER
                this.setPadding(0, 0, 0, 80)
                // this.typeface = ... (You can load your Vazir font here later)
            }
            addView(text)

            // The "Back to Launcher" Button
            val returnButton = Button(context).apply {
                this.text = "بازگشت به محیط امن"
                this.setBackgroundColor(surfaceColor)
                this.setTextColor(dangerRed)
                this.textSize = 16f
                this.setPadding(40, 20, 40, 20)

                setOnClickListener {
                    // Send the child back to your Launcher!
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    hide() // Hide the overlay as they go back
                }
            }
            addView(returnButton)
        }

        // Steal focus so they can't bypass it!
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(lockView, layoutParams)
        isShowing = true
    }

    fun hide() {
        if (!isShowing) return
        try {
            windowManager?.removeView(lockView)
            lockView = null
            isShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}