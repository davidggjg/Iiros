package com.iiros.scanner.core.apk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApkStringScannerTest {

    @Test
    fun `extracts printable ascii runs and ignores short or binary noise`() {
        val bytes = byteArrayOf(0, 1, 2) + "hello_world".toByteArray() + byteArrayOf(0, 0) + "ab".toByteArray()
        val strings = ApkStringScanner.extractPrintableStrings(bytes, minLength = 5)
        assertEquals(listOf("hello_world"), strings)
    }

    @Test
    fun `clean dex-like content produces no findings`() {
        val entries = mapOf(
            "classes.dex" to "Landroid/widget/TextView;Lcom/example/app/MainActivity;".toByteArray()
        )
        val findings = ApkStringScanner.scanEntries(entries)
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `dynamic class loading signature is detected`() {
        val entries = mapOf(
            "classes.dex" to "dalvik/system/DexClassLoader;loadClass".toByteArray()
        )
        val findings = ApkStringScanner.scanEntries(entries)
        assertTrue(findings.any { it.id == "apk.dynamic_code_loading" })
    }

    @Test
    fun `dynamic loading plus crypto together raise the encrypted dropper combo finding`() {
        val entries = mapOf(
            "classes.dex" to "dalvik/system/DexClassLoader;javax/crypto/Cipher;Cipher.getInstance(\"AES".toByteArray()
        )
        val findings = ApkStringScanner.scanEntries(entries)
        assertTrue(findings.any { it.id == "apk.combo.encrypted_dropper" })
    }

    @Test
    fun `accessibility abuse plus sms access raise the otp theft combo finding`() {
        val entries = mapOf(
            "classes.dex" to "getRootInActiveWindow;performGlobalAction;android/telephony/SmsManager;sendTextMessage".toByteArray()
        )
        val findings = ApkStringScanner.scanEntries(entries)
        assertTrue(findings.any { it.id == "apk.combo.overlay_otp_theft" })
    }

    @Test
    fun `non-dex entries are ignored when dex entries are present`() {
        val entries = mapOf(
            "classes.dex" to "Landroid/widget/TextView;".toByteArray(),
            "assets/readme.txt" to "dalvik/system/DexClassLoader".toByteArray(),
        )
        val findings = ApkStringScanner.scanEntries(entries)
        assertTrue(findings.isEmpty())
    }
}
