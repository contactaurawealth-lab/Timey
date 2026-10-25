package com.timey.app.core.blocker

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BlockerManager {

    private val _isFocusActive = MutableStateFlow(false)
    val isFocusActive: StateFlow<Boolean> = _isFocusActive.asStateFlow()

    private val _isBlockingEnabled = MutableStateFlow(false)
    val isBlockingEnabled: StateFlow<Boolean> = _isBlockingEnabled.asStateFlow()

    private val _blockedPackages = MutableStateFlow<Set<String>>(emptySet())
    val blockedPackages: StateFlow<Set<String>> = _blockedPackages.asStateFlow()

    private val _pinProtectionEnabled = MutableStateFlow(false)
    val pinProtectionEnabled: StateFlow<Boolean> = _pinProtectionEnabled.asStateFlow()

    private var currentPin: String = "1234"

    var currentSubject: String = "Focus Session"
    var remainingSeconds: Int = 0

    // White-listed system and communication packages that should never be blocked
    private val systemWhitelist = setOf(
        "com.timey.app",
        "com.android.systemui",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.android.phone",
        "com.android.emergency",
        "com.google.android.inputmethod.latin",
        "com.android.settings"
    )

    fun updateSessionState(
        isActive: Boolean,
        subject: String,
        remainingSec: Int
    ) {
        _isFocusActive.value = isActive
        currentSubject = subject
        remainingSeconds = remainingSec
    }

    fun updateConfig(
        blockingEnabled: Boolean,
        packages: Set<String>,
        pinEnabled: Boolean,
        pin: String
    ) {
        _isBlockingEnabled.value = blockingEnabled
        _blockedPackages.value = packages
        _pinProtectionEnabled.value = pinEnabled
        currentPin = pin
    }

    fun shouldBlock(packageName: String?): Boolean {
        if (packageName == null) return false
        if (!_isBlockingEnabled.value || !_isFocusActive.value) return false
        if (systemWhitelist.contains(packageName) || packageName.contains("launcher")) return false
        return _blockedPackages.value.contains(packageName)
    }

    fun verifyPin(pin: String): Boolean {
        return pin == currentPin
    }
}
