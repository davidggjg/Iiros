package com.iiros.scanner.core.permissions

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel

/**
 * Turns a raw list of permission strings requested by an app into a set of
 * explainable [Finding]s. Pure function, no Android dependency, so it is easy
 * to unit test and reuse (e.g. from a CLI or a future desktop build).
 */
object PermissionRiskAnalyzer {

    fun analyze(requestedPermissions: Collection<String>): List<Finding> {
        val permissions = requestedPermissions.toSet()
        val findings = mutableListOf<Finding>()

        val privacyHits = permissions.intersect(DangerousPermissions.PRIVACY_SENSITIVE)
        if (privacyHits.isNotEmpty()) {
            findings += Finding(
                id = "perm.privacy_sensitive",
                title = "Requests ${privacyHits.size} privacy-sensitive permission(s)",
                detail = privacyHits.joinToString(", ") { it.substringAfterLast('.') },
                severity = if (privacyHits.size >= 4) RiskLevel.MEDIUM else RiskLevel.LOW,
                weight = (privacyHits.size * 4).coerceAtMost(24),
            )
        }

        val controlHits = permissions.intersect(DangerousPermissions.CONTROL_ESCALATION)
        if (controlHits.isNotEmpty()) {
            findings += Finding(
                id = "perm.control_escalation",
                title = "Requests ${controlHits.size} device-control permission(s)",
                detail = controlHits.joinToString(", ") { it.substringAfterLast('.') },
                severity = if (controlHits.size >= 2) RiskLevel.HIGH else RiskLevel.MEDIUM,
                weight = (controlHits.size * 8).coerceAtMost(40),
            )
        }

        for (combo in DangerousPermissions.SUSPICIOUS_COMBOS) {
            if (permissions.containsAll(combo.permissions)) {
                findings += Finding(
                    id = combo.id,
                    title = combo.title,
                    detail = combo.permissions.joinToString(", ") { it.substringAfterLast('.') },
                    severity = RiskLevel.HIGH,
                    weight = combo.weight,
                )
            }
        }

        return findings
    }
}
