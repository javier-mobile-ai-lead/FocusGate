package com.pe.learnai

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppBlockerService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sessionDone = false

    override fun onServiceConnected() {
        scope.launch {
            SessionManager.sessionCompleteFlow(this@AppBlockerService).collect {
                sessionDone = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (BlockedApps.isBlocked(pkg) && !sessionDone) {
            startActivity(
                Intent(this, BlockOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
