package com.timey.app.core.blocker

data class InstalledAppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val isDistractionRisk: Boolean = false
)
