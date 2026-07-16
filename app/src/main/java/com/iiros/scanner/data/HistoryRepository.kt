package com.iiros.scanner.data

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.scoring.AppScanResult
import com.iiros.scanner.core.url.UrlScanResult
import com.iiros.scanner.data.db.ScanHistoryDao
import com.iiros.scanner.data.db.ScanHistoryEntity
import com.iiros.scanner.data.db.ScanType
import kotlinx.coroutines.flow.Flow

private const val FINDING_SEPARATOR = "~~"
private const val FINDING_FIELD_SEPARATOR = "::"

class HistoryRepository(private val dao: ScanHistoryDao) {

    fun observeHistory(): Flow<List<ScanHistoryEntity>> = dao.observeAll()

    suspend fun clear() = dao.clear()

    suspend fun record(result: AppScanResult) {
        dao.insert(
            ScanHistoryEntity(
                type = ScanType.APP,
                target = result.packageName,
                label = result.appLabel,
                score = result.score,
                riskLevel = result.riskLevel.name,
                findingsSummary = serializeFindings(result.findings),
                scannedAtEpochMillis = result.scannedAtEpochMillis,
            )
        )
    }

    suspend fun record(result: UrlScanResult) {
        dao.insert(
            ScanHistoryEntity(
                type = ScanType.URL,
                target = result.input,
                label = result.host ?: result.input,
                score = result.score,
                riskLevel = result.riskLevel.name,
                findingsSummary = serializeFindings(result.findings),
                scannedAtEpochMillis = System.currentTimeMillis(),
            )
        )
    }

    private fun serializeFindings(findings: List<Finding>): String =
        findings.joinToString(FINDING_SEPARATOR) { "${it.title}$FINDING_FIELD_SEPARATOR${it.severity.name}" }
}

fun ScanHistoryEntity.deserializeFindingTitles(): List<Pair<String, String>> =
    if (findingsSummary.isBlank()) {
        emptyList()
    } else {
        findingsSummary.split(FINDING_SEPARATOR).mapNotNull { entry ->
            val parts = entry.split(FINDING_FIELD_SEPARATOR)
            if (parts.size == 2) parts[0] to parts[1] else null
        }
    }
