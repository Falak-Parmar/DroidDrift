package com.drift.droiddrift

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class InputAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Fallback receiver: No event handling is required for passive accessibility lifecycle.
    }

    override fun onInterrupt() {
        // Not needed
    }
}
