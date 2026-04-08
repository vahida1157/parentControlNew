package com.vahak.parentcontroll.core.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

class TimeLockOverlay(private val context: Context, private val message: String) {

    private var windowManager: WindowManager? = null
    private var lockView: ViewGroup? = null
    private var isShowing = false

    private val dangerRed = "#FF5252".toColorInt()
    private val surfaceColor = "#FFFFFF".toColorInt()

    fun show() {
        if (isShowing) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        lockView = LinearLayout(context).apply {
            setBackgroundColor(dangerRed)
            gravity = Gravity.CENTER
            orientation = LinearLayout.VERTICAL

            // 1. The Image Placeholder
            val imageView = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_secure) // Built-in lock icon for now
                setColorFilter(android.graphics.Color.WHITE) // Make it white to pop against red
                layoutParams = LinearLayout.LayoutParams(250, 250).apply {
                    setMargins(0, 0, 0, 60) // Add margin below the image
                }
            }
            addView(imageView)

            // 2. The Title
            val text = TextView(context).apply {
                this.text = message
                this.textSize = 24f
                this.setTextColor(android.graphics.Color.WHITE)
                this.gravity = Gravity.CENTER
                this.setPadding(0, 0, 0, 80)
            }
            addView(text)

            // 3. The "Back to Launcher" Button
            val returnButton = Button(context).apply {
                this.text = "بازگشت به محیط امن"
                this.setBackgroundColor(surfaceColor)
                this.setTextColor(dangerRed)
                this.textSize = 16f
                this.setPadding(40, 20, 40, 20)

                setOnClickListener {
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                    hide()
                }
            }
            addView(returnButton)
        }

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

@Preview(showBackground = true, locale = "fa", name = "Overlay UI Design")
@Composable
fun TimeLockOverlayPreview() {
    ParentControlTheme {
        // This exactly mimics the programmatic Android View we wrote!
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFF5252)), // Danger Red
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. The Image Placeholder
            Icon(
                painter = painterResource(id = android.R.drawable.ic_secure),
                contentDescription = "Lock",
                tint = Color.White,
                modifier = Modifier
                    .size(100.dp)
                    .padding(bottom = 20.dp)
            )

            // 2. The Title
            Text(
                text = "استفاده از این نرم افزار مجاز نیست!",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 40.dp)
            )

            // 3. The "Back to Launcher" Button
            Button(
                onClick = { /* Dummy Click */ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFFF5252)
                ),
                contentPadding = PaddingValues(horizontal = 30.dp, vertical = 15.dp)
            ) {
                Text(
                    text = "بازگشت به محیط امن",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}