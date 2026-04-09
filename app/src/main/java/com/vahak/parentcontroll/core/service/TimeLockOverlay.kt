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
import com.vahak.parentcontroll.ui.screens.overlay.DizzyPhoneScreen
import com.vahak.parentcontroll.ui.theme.ParentControlTheme

class TimeLockOverlay(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner
) : SavedStateRegistryOwner, ViewModelStoreOwner {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var isShowing = false

    // 1. THE MISSING PIECE: Provide the lifecycle!
    override val lifecycle: Lifecycle
        get() = lifecycleOwner.lifecycle

    // --- SavedStateRegistry Implementation ---
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    // --- ViewModelStore Implementation ---
    override val viewModelStore = ViewModelStore()

    init {
        // Now this will work perfectly because it has a lifecycle to attach to!
        savedStateRegistryController.performRestore(null)
    }

    fun show() {
        if (isShowing) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        composeView = ComposeView(context).apply {

            setViewTreeLifecycleOwner(this@TimeLockOverlay) // Changed to this class!
            setViewTreeViewModelStoreOwner(this@TimeLockOverlay)
            setViewTreeSavedStateRegistryOwner(this@TimeLockOverlay)

            setContent {
                ParentControlTheme {
                    DizzyPhoneScreen(
                        onBackClick = {
                            val intent = Intent(Intent.ACTION_MAIN).apply {
                                addCategory(Intent.CATEGORY_HOME)
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            hide()
                        }
                    )
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
}