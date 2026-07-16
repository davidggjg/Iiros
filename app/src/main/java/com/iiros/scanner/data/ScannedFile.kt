package com.iiros.scanner.data

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel
import com.iiros.scanner.core.file.FileCategory

data class ScannedFile(
    val path: String,
    val name: String,
    val category: FileCategory,
    val sizeBytes: Long,
    val findings: List<Finding>,
) {
    val riskLevel: RiskLevel get() = findings.maxOfOrNull { it.severity } ?: RiskLevel.SAFE
}

data class FileScanProgress(
    val scannedCount: Int,
    val currentPath: String,
    val newlyFlagged: ScannedFile? = null,
)
