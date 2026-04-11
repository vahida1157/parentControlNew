package com.vahak.parentcontroll.core.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.vahak.parentcontroll.ui.screens.overlay.BedtimeOverlayScreen
import com.vahak.parentcontroll.ui.screens.overlay.DizzyPhoneScreen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

// 1. PRO FIX: Define the specific reasons an overlay might appear
enum class OverlayType {
    TIME_LIMIT,
    BEDTIME,
    APP_BLOCK
}

class RestrictionOverlay(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val type: OverlayType // 2. Accept the type in the constructor
) : SavedStateRegistryOwner, ViewModelStoreOwner {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var isShowing = false

    fun isShowing(): Boolean = isShowing

    // --- Lifecycle Implementation ---
    override val lifecycle: Lifecycle
        get() = lifecycleOwner.lifecycle

    // --- SavedStateRegistry Implementation ---
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // --- ViewModelStore Implementation ---
    override val viewModelStore = ViewModelStore()

    init {
        savedStateRegistryController.performRestore(null)
    }

    fun show() {
        if (isShowing) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@RestrictionOverlay)
            setViewTreeViewModelStoreOwner(this@RestrictionOverlay)
            setViewTreeSavedStateRegistryOwner(this@RestrictionOverlay)

            setContent {
                ParentControlTheme {
                    // 3. PRO FIX: Dynamically route to the correct UI!
                    when (type) {
                        OverlayType.TIME_LIMIT -> {
                            DizzyPhoneScreen(onBackClick = { navigateHome() })
                        }

                        OverlayType.BEDTIME -> {
                            BedtimeOverlayScreen(onBackClick = { navigateHome() })
                        }

                        OverlayType.APP_BLOCK -> {
                            // Reusing Dizzy Phone for blocked apps for now,
                            // but you can easily plug in an "AppBlockOverlayScreen" here later!
                            DizzyPhoneScreen(onBackClick = { navigateHome() })
                        }
                    }
                }
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        windowManager?.addView(composeView, layoutParams)
        isShowing = true
    }

    fun hide() {
        if (!isShowing) return
        try {
            windowManager?.removeView(composeView)
            composeView = null
            isShowing = false

            viewModelStore.clear()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 4. PRO FIX: Extracted for clean reuse across all screens
    private fun navigateHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
        hide()
    }
}