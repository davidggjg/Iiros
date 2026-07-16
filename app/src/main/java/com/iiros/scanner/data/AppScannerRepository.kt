package com.iiros.scanner.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.apk.ApkStringScanner
import com.iiros.scanner.core.malware.MalwareSignatureDatabase
import com.iiros.scanner.core.scoring.AppScanInput
import com.iiros.scanner.core.scoring.AppScanResult
import com.iiros.scanner.core.scoring.RiskScoreEngine
import java.io.File
import java.security.MessageDigest

/**
 * Bridges Android's [PackageManager] to the framework-free `:core` risk
 * engine. Everything here runs entirely on-device; nothing is uploaded
 * anywhere.
 */
class AppScannerRepository(private val context: Context) {

    private val malwareSignatureDatabase = MalwareSignatureDatabase()

    private val packageQueryFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_PERMISSIONS or PackageManager.GET_SIGNATURES
        }

    fun listInstalledApps(): List<InstalledAppInfo> =
        context.packageManager.getInstalledPackages(packageQueryFlags).map { toInstalledAppInfo(it) }

    fun getInstalledApp(packageName: String): InstalledAppInfo? {
        val packageInfo = try {
            context.packageManager.getPackageInfo(packageName, packageQueryFlags)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
        return toInstalledAppInfo(packageInfo)
    }

    private fun toInstalledAppInfo(packageInfo: PackageInfo): InstalledAppInfo {
        val pm = context.packageManager
        val appInfo = packageInfo.applicationInfo
        val isSystem = appInfo != null && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(packageInfo.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(packageInfo.packageName)
            }
        } catch (e: Exception) {
            null
        }
        return InstalledAppInfo(
            packageName = packageInfo.packageName,
            appLabel = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: packageInfo.packageName,
            icon = appInfo?.let { pm.getApplicationIcon(it) },
            permissions = packageInfo.requestedPermissions?.toList() ?: emptyList(),
            isSystemApp = isSystem,
            installerPackageName = installer,
            signingCertSha256 = signingCertSha256(packageInfo),
            targetSdkVersion = appInfo?.targetSdkVersion ?: 0,
            apkSourcePath = appInfo?.sourceDir ?: "",
            versionName = packageInfo.versionName,
        )
    }

    fun quickScan(app: InstalledAppInfo): AppScanResult =
        RiskScoreEngine.scanApp(app.toScanInput(), malwareSignatureDatabase)

    /**
     * Slower, opt-in analysis that opens the APK as a zip and greps its dex
     * entries for suspicious API signatures. Not run automatically for every
     * app in the list because reading every APK on a phone with 200+ apps
     * would make the list screen feel sluggish.
     */
    fun deepScan(app: InstalledAppInfo): AppScanResult {
        val apkFindings: List<Finding> = try {
            ApkStringScanner.scanApkFile(File(app.apkSourcePath))
        } catch (e: Exception) {
            emptyList()
        }
        return RiskScoreEngine.scanApp(app.toScanInput(), malwareSignatureDatabase, apkFindings)
    }

    private fun InstalledAppInfo.toScanInput() = AppScanInput(
        packageName = packageName,
        appLabel = appLabel,
        permissions = permissions,
        isSystemApp = isSystemApp,
        installerPackageName = installerPackageName,
        signingCertSha256 = signingCertSha256,
        targetSdkVersion = targetSdkVersion,
    )

    @Suppress("DEPRECATION")
    private fun signingCertSha256(packageInfo: PackageInfo): List<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo
            when {
                signingInfo == null -> emptyArray()
                signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
                else -> signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures ?: emptyArray()
        }

        val digest = MessageDigest.getInstance("SHA-256")
        return signatures.map { signature ->
            digest.reset()
            digest.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
        }
    }
}
