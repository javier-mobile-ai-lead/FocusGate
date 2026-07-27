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
    private var unlockUntil: Long = 0L          // epoch ms — live-checked on every event
    private var blockedPackages: Set<String> = BlocklistManager.defaultPackages

    override fun onServiceConnected() {
        isRunning = true
        scope.launch {
            // Keep the unlock timestamp up-to-date; the actual expired-or-not check
            // is done fresh inside onAccessibilityEvent so cooldown expiry works without
            // waiting for another DataStore emission.
            SessionManager.unlockUntilFlow(this@AppBlockerService).collect {
                unlockUntil = it
            }
        }
        scope.launch {
            BlocklistManager.blockedPackagesFlow(this@AppBlockerService).collect {
                blockedPackages = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        if (pkg in blockedPackages && System.currentTimeMillis() > unlockUntil) {
            startActivity(
                Intent(this, BlockOverlayActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
            )
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        isRunning = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        var isRunning = false
            private set
    }
}
