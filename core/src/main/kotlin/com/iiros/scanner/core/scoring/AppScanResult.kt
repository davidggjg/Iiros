package com.iiros.scanner.core.scoring

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel

data class AppScanResult(
    val packageName: String,
    val appLabel: String,
    val score: Int,
    val riskLevel: RiskLevel,
    val findings: List<Finding>,
    val scannedAtEpochMillis: Long,
)
