package com.iiros.scanner.core.scoring

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel
import com.iiros.scanner.core.malware.MalwareSignatureDatabase
import com.iiros.scanner.core.permissions.PermissionRiskAnalyzer

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

    fun scanApp(
        input: AppScanInput,
        malwareDb: MalwareSignatureDatabase = MalwareSignatureDatabase(),
        apkFindings: List<Finding> = emptyList(),
    ): AppScanResult {
        val findings = mutableListOf<Finding>()

        findings += PermissionRiskAnalyzer.analyze(input.permissions)
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
