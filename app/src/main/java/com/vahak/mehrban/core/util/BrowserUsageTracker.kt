package com.vahak.mehrban.core.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object BrowserUsageTracker {
    var isBrowserForeground: Boolean = false

    // 🚀 NEW: Emergency kill-switch signal
    private val _forceCloseFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val forceCloseFlow = _forceCloseFlow.asSharedFlow()

    fun triggerForceClose() {
        _forceCloseFlow.tryEmit(Unit)
    }
}