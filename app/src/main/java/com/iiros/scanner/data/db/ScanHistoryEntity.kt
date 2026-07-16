package com.iiros.scanner.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScanType { APP, URL }

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ScanType,
    /** Package name for APP scans, the scanned URL text for URL scans. */
    val target: String,
    val label: String,
    val score: Int,
    val riskLevel: String,
    /** Findings serialized as "title|severity" pairs joined by "~~", to avoid pulling in a JSON dependency for one small field. */
    val findingsSummary: String,
    val scannedAtEpochMillis: Long,
)
