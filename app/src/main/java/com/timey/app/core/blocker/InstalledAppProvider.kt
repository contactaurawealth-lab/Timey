package com.timey.app.core.blocker

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

object InstalledAppProvider {

    private val commonDistractionKeywords = listOf(
        "instagram", "tiktok", "twitter", "reddit", "youtube", "facebook",
        "snapchat", "netflix", "discord", "twitch", "game", "reel", "short"
    )

    fun getInstalledLaunchableApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
        val appList = mutableListOf<InstalledAppInfo>()
        val seenPackages = mutableSetOf<String>()

        for (resolveInfo in resolveInfos) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName || seenPackages.contains(pkg)) continue
            seenPackages.add(pkg)

            val appName = resolveInfo.loadLabel(pm).toString()
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

            val lowerPkg = pkg.lowercase()
            val lowerName = appName.lowercase()
            val isRisk = commonDistractionKeywords.any { kw ->
                lowerPkg.contains(kw) || lowerName.contains(kw)
            }

            appList.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = appName,
                    isSystemApp = isSystem,
                    isDistractionRisk = isRisk
                )
            )
        }

        return appList.sortedWith(
            compareByDescending<InstalledAppInfo> { it.isDistractionRisk }
                .thenBy { it.appName.lowercase() }
        )
    }
}
