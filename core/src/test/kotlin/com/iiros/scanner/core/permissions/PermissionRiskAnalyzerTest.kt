package com.iiros.scanner.core.permissions

import com.iiros.scanner.core.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PermissionRiskAnalyzerTest {

    @Test
    fun `benign permission set produces no findings`() {
        val findings = PermissionRiskAnalyzer.analyze(
            listOf("android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE")
        )
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `single privacy sensitive permission is low severity`() {
        val findings = PermissionRiskAnalyzer.analyze(listOf("android.permission.CAMERA"))
        assertEquals(1, findings.size)
        assertEquals(RiskLevel.LOW, findings.first().severity)
    }

    @Test
    fun `overlay plus accessibility triggers the takeover combo finding`() {
        val findings = PermissionRiskAnalyzer.analyze(
            listOf(
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
            )
        )
        assertTrue(findings.any { it.id == "combo.overlay_accessibility" })
        assertTrue(findings.any { it.severity == RiskLevel.HIGH })
    }

    @Test
    fun `sms plus overlay triggers otp interception combo`() {
        val findings = PermissionRiskAnalyzer.analyze(
            listOf(
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_SMS",
                "android.permission.SYSTEM_ALERT_WINDOW",
            )
        )
        assertTrue(findings.any { it.id == "combo.sms_otp_interception" })
    }

    @Test
    fun `many privacy permissions escalate to medium severity`() {
        val findings = PermissionRiskAnalyzer.analyze(
            listOf(
                "android.permission.READ_CONTACTS",
                "android.permission.READ_CALL_LOG",
                "android.permission.RECORD_AUDIO",
                "android.permission.CAMERA",
                "android.permission.ACCESS_FINE_LOCATION",
            )
        )
        val privacyFinding = findings.first { it.id == "perm.privacy_sensitive" }
        assertEquals(RiskLevel.MEDIUM, privacyFinding.severity)
    }
}
