package com.iiros.scanner.core.file

import com.iiros.scanner.core.RiskLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FileDisguiseScannerTest {

    private fun bytes(vararg ints: Int): ByteArray = ints.map { it.toByte() }.toByteArray()

    @Test
    fun `real photo is not flagged`() {
        val findings = FileDisguiseScanner.scan("vacation.jpg", bytes(0xFF, 0xD8, 0xFF, 0xE0))
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `apk disguised as a photo is flagged critical`() {
        val dexHeader = "dex\n035 ".toByteArray()
        val findings = FileDisguiseScanner.scan("vacation.jpg", dexHeader)
        assertEquals(1, findings.size)
        assertEquals(RiskLevel.CRITICAL, findings.first().severity)
        assertEquals("file.disguised_executable", findings.first().id)
    }

    @Test
    fun `windows executable disguised as a video is flagged critical`() {
        val findings = FileDisguiseScanner.scan("funny_clip.mp4", bytes(0x4D, 0x5A, 0x90, 0x00))
        assertTrue(findings.any { it.id == "file.disguised_executable" })
    }

    @Test
    fun `zip archive disguised as a document is flagged high`() {
        val findings = FileDisguiseScanner.scan("invoice.pdf", bytes(0x50, 0x4B, 0x03, 0x04))
        assertEquals(1, findings.size)
        assertEquals(RiskLevel.HIGH, findings.first().severity)
        assertEquals("file.disguised_archive", findings.first().id)
    }

    @Test
    fun `docx is not flagged even though it is a real zip container`() {
        val findings = FileDisguiseScanner.scan("resume.docx", bytes(0x50, 0x4B, 0x03, 0x04))
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `apk claiming to be apk is not flagged`() {
        val findings = FileDisguiseScanner.scan("app-release.apk", bytes(0x50, 0x4B, 0x03, 0x04))
        assertTrue(findings.isEmpty())
    }

    @Test
    fun `unreadable or empty header produces no finding`() {
        assertTrue(FileDisguiseScanner.scan("mystery.dat", ByteArray(0)).isEmpty())
    }
}
