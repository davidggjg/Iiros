package com.iiros.scanner.core.scoring

import com.iiros.scanner.core.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RiskScoreEngineTest {

    @Test
    fun `trusted play store app with benign permissions is safe`() {
        val input = AppScanInput(
            packageName = "com.spotify.music",
            appLabel = "Spotify",
            permissions = listOf("android.permission.INTERNET"),
            isSystemApp = false,
            installerPackageName = "com.android.vending",
            signingCertSha256 = emptyList(),
            targetSdkVersion = 34,
        )
        val result = RiskScoreEngine.scanApp(input)
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun `sideloaded app with takeover permission combo is high or critical risk`() {
        val input = AppScanInput(
            packageName = "com.free.flashlight.pro",
            appLabel = "Super Flashlight",
            permissions = listOf(
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_SMS",
            ),
            isSystemApp = false,
            installerPackageName = null,
            signingCertSha256 = emptyList(),
            targetSdkVersion = 33,
        )
        val result = RiskScoreEngine.scanApp(input)
        assertTrue(result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.CRITICAL)
        assertTrue(result.findings.any { it.id == "source.sideloaded" })
        assertTrue(result.findings.any { it.id == "combo.overlay_accessibility" })
    }

    @Test
    fun `known bad package name always reaches critical`() {
        val input = AppScanInput(
            packageName = "com.android.provider.settings",
            appLabel = "Settings",
            permissions = emptyList(),
            isSystemApp = false,
            installerPackageName = null,
            signingCertSha256 = emptyList(),
            targetSdkVersion = 34,
        )
        val result = RiskScoreEngine.scanApp(input)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun `system app is never penalized for install source`() {
        val input = AppScanInput(
            packageName = "com.android.settings",
            appLabel = "Settings",
            permissions = listOf("android.permission.WRITE_SECURE_SETTINGS"),
            isSystemApp = true,
            installerPackageName = null,
            signingCertSha256 = emptyList(),
            targetSdkVersion = 34,
        )
        val result = RiskScoreEngine.scanApp(input)
        assertTrue(result.findings.none { it.id == "source.sideloaded" })
    }

    @Test
    fun `preinstalled google play services is not critical despite broad permissions`() {
        // Regression test: a real device flagged com.google.android.gms as
        // "Critical" purely because it holds the same overlay+accessibility+
        // device-admin style permissions a banking trojan would, with no
        // regard for it being a pre-installed OS component.
        val input = AppScanInput(
            packageName = "com.google.android.gms",
            appLabel = "Google Play Services",
            permissions = listOf(
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.PACKAGE_USAGE_STATS",
                "android.permission.RECEIVE_BOOT_COMPLETED",
                "android.permission.CAMERA",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.READ_PHONE_STATE",
            ),
            isSystemApp = true,
            installerPackageName = null,
            signingCertSha256 = emptyList(),
            targetSdkVersion = 34,
        )
        val result = RiskScoreEngine.scanApp(input)
        assertTrue(
            result.riskLevel == RiskLevel.SAFE || result.riskLevel == RiskLevel.LOW,
            "expected SAFE/LOW but was ${result.riskLevel} (score=${result.score}, findings=${result.findings})",
        )
        assertTrue(result.findings.none { it.id == "malware.brand_impersonation" })
    }

    @Test
    fun `play-store-installed security app with same permissions is dampened but not zeroed`() {
        val input = AppScanInput(
            packageName = "com.eset.ems2.gp",
            appLabel = "ESET Mobile Security",
            permissions = listOf(
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.BIND_DEVICE_ADMIN",
                "android.permission.PACKAGE_USAGE_STATS",
                "android.permission.READ_SMS",
                "android.permission.CAMERA",
            ),
            isSystemApp = false,
            installerPackageName = "com.android.vending",
            signingCertSha256 = emptyList(),
            targetSdkVersion = 34,
        )
        val result = RiskScoreEngine.scanApp(input)
        assertTrue(
            result.riskLevel != RiskLevel.CRITICAL,
            "expected below CRITICAL but was ${result.riskLevel} (score=${result.score})",
        )
    }
}
