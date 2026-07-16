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
}
