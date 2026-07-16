package com.iiros.scanner.core.scoring

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel
import com.iiros.scanner.core.malware.MalwareSignatureDatabase
import com.iiros.scanner.core.permissions.PermissionRiskAnalyzer
import kotlin.math.roundToInt

/**
 * Combines permission analysis, malware-signature lookups and (optional) APK
 * static-analysis findings into a single explainable [AppScanResult].
 */
object RiskScoreEngine {

    private val TRUSTED_INSTALLERS = setOf(
        "com.android.vending",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.amazon.venezia",
        "com.sec.android.app.samsungapps",
    )

    /**
     * Dangerous *permissions* are a weak signal on their own: OS components and
     * legitimate security/assistant apps (Play Services, Bixby, antivirus
     * suites, ...) routinely hold overlay+accessibility+device-admin for real
     * reasons. Without this, every such app scored "Critical" identically to
     * an actual sideloaded trojan requesting the same permissions — permission
     * requests are dampened by how trustworthy the install source is, while
     * hard evidence (malware-DB hits, decompiled-code findings) stays at full
     * weight regardless of source.
     */
    private fun permissionTrustMultiplier(input: AppScanInput): Double = when {
        input.isSystemApp -> 0.15
        input.installerPackageName in TRUSTED_INSTALLERS -> 0.5
        else -> 1.0
    }

    fun scanApp(
        input: AppScanInput,
        malwareDb: MalwareSignatureDatabase = MalwareSignatureDatabase(),
        apkFindings: List<Finding> = emptyList(),
    ): AppScanResult {
        val findings = mutableListOf<Finding>()

        val trustMultiplier = permissionTrustMultiplier(input)
        findings += PermissionRiskAnalyzer.analyze(input.permissions).map { finding ->
            if (trustMultiplier == 1.0) {
                finding
            } else {
                finding.copy(weight = (finding.weight * trustMultiplier).roundToInt())
            }
        }
        if (trustMultiplier < 1.0) {
            findings += Finding(
                id = "trust.discounted_permissions",
                title = "Permission risk reduced: trusted install source",
                detail = if (input.isSystemApp) "system app" else "installed via ${input.installerPackageName}",
                severity = RiskLevel.SAFE,
                weight = 0,
            )
        }
        findings += malwareDb.lookup(input.packageName, input.appLabel, input.signingCertSha256)
        findings += apkFindings

        if (!input.isSystemApp && input.installerPackageName !in TRUSTED_INSTALLERS) {
            findings += Finding(
                id = "source.sideloaded",
                title = "Installed from an untrusted or unknown source",
                detail = "installer=${input.installerPackageName ?: "unknown (sideloaded/ADB)"}",
                severity = RiskLevel.LOW,
                weight = 8,
            )
        }

        if (input.targetSdkVersion in 1 until 23) {
            findings += Finding(
                id = "manifest.legacy_target_sdk",
                title = "Targets a very old Android API level",
                detail = "targetSdk=${input.targetSdkVersion} (predates runtime permissions)",
                severity = RiskLevel.LOW,
                weight = 6,
            )
        }

        val score = findings.sumOf { it.weight }.coerceAtMost(100)
        return AppScanResult(
            packageName = input.packageName,
            appLabel = input.appLabel,
            score = score,
            riskLevel = RiskLevel.fromScore(score),
            findings = findings.sortedByDescending { it.weight },
            scannedAtEpochMillis = System.currentTimeMillis(),
        )
    }
}
