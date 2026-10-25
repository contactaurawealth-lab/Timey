package com.timey.app.core.blocker

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusAccessibilityBlockerService : AccessibilityService() {

    private val TAG = "FocusBlockerService"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            if (BlockerManager.shouldBlock(packageName)) {
                Log.d(TAG, "Intercepted launch of blocked app: $packageName")

                val blockerIntent = Intent(this, BlockerOverlayActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(BlockerOverlayActivity.EXTRA_BLOCKED_PACKAGE, packageName)
                }
                startActivity(blockerIntent)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }
}
