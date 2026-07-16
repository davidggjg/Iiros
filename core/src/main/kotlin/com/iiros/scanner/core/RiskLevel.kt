package com.iiros.scanner.core

/** Overall verdict severity, declared from safest to most dangerous. */
enum class RiskLevel {
    SAFE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    companion object {
        fun fromScore(score: Int): RiskLevel = when {
            score >= 80 -> CRITICAL
            score >= 55 -> HIGH
            score >= 30 -> MEDIUM
            score >= 10 -> LOW
            else -> SAFE
        }
    }
}

/**
 * A single explainable signal that contributed to a verdict, so the UI can show
 * the user *why* something was flagged instead of a bare score.
 */
data class Finding(
    val id: String,
    val title: String,
    val detail: String,
    val severity: RiskLevel,
    val weight: Int
)
