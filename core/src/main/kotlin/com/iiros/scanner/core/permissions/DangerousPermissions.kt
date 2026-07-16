package com.iiros.scanner.core.permissions

/**
 * Reference data about Android permission strings that matter for a malware triage
 * tool. Kept as plain string constants (rather than depending on the Android SDK)
 * so this module stays a pure-JVM library that can be unit tested outside an
 * Android environment.
 */
object DangerousPermissions {

    /** Permissions that grant access to sensitive personal data or hardware. */
    val PRIVACY_SENSITIVE: Set<String> = setOf(
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.READ_PHONE_STATE",
        "android.permission.READ_PHONE_NUMBERS",
        "android.permission.GET_ACCOUNTS",
        "android.permission.BODY_SENSORS",
        "android.permission.READ_CALENDAR",
        "android.permission.ACTIVITY_RECOGNITION",
    )

    /**
     * Permissions/APIs that are individually legitimate (accessibility tools,
     * screen readers, MDM) but are the exact building blocks used by banking
     * trojans and stalkerware to take control of a device: drawing over other
     * apps, reading screen content, surviving uninstall attempts, installing
     * more APKs, or disabling Play Protect.
     */
    val CONTROL_ESCALATION: Set<String> = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.BIND_ACCESSIBILITY_SERVICE",
        "android.permission.BIND_DEVICE_ADMIN",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
        "android.permission.WRITE_SECURE_SETTINGS",
        "android.permission.BIND_VPN_SERVICE",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.RECEIVE_BOOT_COMPLETED",
        "android.permission.SCHEDULE_EXACT_ALARM",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
    )

    /**
     * Permission combinations that are individually explainable but, together,
     * are a strong signature of a specific attack pattern (overlay phishing,
     * SMS-OTP interception for banking fraud, stalkerware, etc). Each entry is
     * (id, human title, permissions that must ALL be present, weight).
     */
    data class Combo(val id: String, val title: String, val permissions: Set<String>, val weight: Int)

    val SUSPICIOUS_COMBOS: List<Combo> = listOf(
        Combo(
            id = "combo.overlay_accessibility",
            title = "Overlay + Accessibility Service",
            permissions = setOf(
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
            ),
            weight = 35,
        ),
        Combo(
            id = "combo.sms_otp_interception",
            title = "SMS interception + Overlay (OTP/banking-fraud pattern)",
            permissions = setOf(
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_SMS",
                "android.permission.SYSTEM_ALERT_WINDOW",
            ),
            weight = 40,
        ),
        Combo(
            id = "combo.stalkerware",
            title = "Location + Call log + Mic + hidden persistence (stalkerware pattern)",
            permissions = setOf(
                "android.permission.ACCESS_BACKGROUND_LOCATION",
                "android.permission.READ_CALL_LOG",
                "android.permission.RECORD_AUDIO",
                "android.permission.RECEIVE_BOOT_COMPLETED",
            ),
            weight = 40,
        ),
        Combo(
            id = "combo.device_takeover",
            title = "Device admin + Accessibility + install packages (full takeover pattern)",
            permissions = setOf(
                "android.permission.BIND_DEVICE_ADMIN",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.REQUEST_INSTALL_PACKAGES",
            ),
            weight = 45,
        ),
    )
}
