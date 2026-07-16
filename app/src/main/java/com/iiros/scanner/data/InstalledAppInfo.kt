package com.iiros.scanner.data

import android.graphics.drawable.Drawable

/**
 * Snapshot of the PackageManager facts about one installed app that the risk
 * engine and UI need. Deliberately holds no [android.content.Context] so it
 * can be passed around/cached freely.
 */
data class InstalledAppInfo(
    val packageName: String,
    val appLabel: String,
    val icon: Drawable?,
    val permissions: List<String>,
    val isSystemApp: Boolean,
    val installerPackageName: String?,
    val signingCertSha256: List<String>,
    val targetSdkVersion: Int,
    val apkSourcePath: String,
    val versionName: String?,
)
