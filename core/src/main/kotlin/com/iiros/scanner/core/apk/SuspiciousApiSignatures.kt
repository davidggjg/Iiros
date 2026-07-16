package com.iiros.scanner.core.apk

/**
 * Plain-text signatures to look for in the string pool of an app's dex/APK
 * contents. This is intentionally a *lightweight* static-analysis technique
 * (comparable to running `strings` over the dex and grepping) rather than a
 * full bytecode decompiler: it cannot prove intent, but the presence and,
 * especially, the *combination* of these strings is a well known heuristic
 * used by real-world triage tools to flag droppers, banking trojans and
 * overlay/accessibility abuse for human review.
 */
object SuspiciousApiSignatures {

    data class Category(
        val id: String,
        val title: String,
        val signatures: List<String>,
        val weight: Int,
    )

    val CATEGORIES: List<Category> = listOf(
        Category(
            id = "apk.dynamic_code_loading",
            title = "Loads additional code at runtime (dropper pattern)",
            signatures = listOf(
                "dalvik/system/DexClassLoader",
                "dalvik/system/InMemoryDexClassLoader",
                "dalvik/system/PathClassLoader",
                "loadDex",
                "openDexFile",
                "DexFile.loadDex",
            ),
            weight = 22,
        ),
        Category(
            id = "apk.shell_exec",
            title = "Executes shell commands / requests root",
            signatures = listOf(
                "Runtime.getRuntime().exec",
                "java/lang/ProcessBuilder",
                "/system/bin/su",
                "/system/xbin/su",
                "su -c",
                "Superuser.apk",
            ),
            weight = 18,
        ),
        Category(
            id = "apk.sms_abuse",
            title = "Reads or sends SMS programmatically",
            signatures = listOf(
                "android/telephony/SmsManager",
                "sendTextMessage",
                "sendMultipartTextMessage",
                "content://sms",
            ),
            weight = 16,
        ),
        Category(
            id = "apk.accessibility_abuse",
            title = "Drives the screen via Accessibility APIs (overlay/auto-click pattern)",
            signatures = listOf(
                "getRootInActiveWindow",
                "performGlobalAction",
                "dispatchGesture",
                "TYPE_ACCESSIBILITY_OVERLAY",
                "AccessibilityService",
            ),
            weight = 20,
        ),
        Category(
            id = "apk.device_admin_persistence",
            title = "Uses device-admin APIs for persistence or wiping",
            signatures = listOf(
                "DeviceAdminReceiver",
                "wipeData",
                "lockNow",
                "resetPassword",
                "DevicePolicyManager",
            ),
            weight = 18,
        ),
        Category(
            id = "apk.c2_networking",
            title = "Hardcoded raw IP address or .onion endpoint (possible C2)",
            signatures = listOf(
                ".onion",
            ),
            weight = 14,
        ),
        Category(
            id = "apk.anti_analysis",
            title = "Checks for emulator/debugger/Xposed (analysis evasion)",
            signatures = listOf(
                "TracerPid",
                "isDebuggerConnected",
                "Genymotion",
                "de.robv.android.xposed",
                "frida-server",
                "goldfish",
            ),
            weight = 15,
        ),
        Category(
            id = "apk.known_packer",
            title = "Wrapped by a known APK packer/protector (often used to hide payloads)",
            signatures = listOf(
                "com.tencent.StubShell",
                "com.qihoo360.mobilesafe",
                "com.secneo.apkwrapper",
                "com.stub.StubApp",
                "com.jiagu.sdk",
            ),
            weight = 12,
        ),
        Category(
            id = "apk.crypto_payload",
            title = "Decrypts bundled assets at runtime (possible hidden payload)",
            signatures = listOf(
                "Cipher.getInstance(\"AES",
                "javax/crypto/Cipher",
                "assets/payload",
                "assets/secret",
            ),
            weight = 14,
        ),
    )

    /** Minimum run length for a byte sequence to be treated as a printable ASCII string. */
    const val MIN_STRING_LENGTH = 5
}
