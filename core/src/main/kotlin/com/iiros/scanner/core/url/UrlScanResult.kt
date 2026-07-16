package com.iiros.scanner.core.url

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel

data class UrlScanResult(
    val input: String,
    val normalizedUrl: String?,
    val host: String?,
    val score: Int,
    val riskLevel: RiskLevel,
    val findings: List<Finding>,
)
