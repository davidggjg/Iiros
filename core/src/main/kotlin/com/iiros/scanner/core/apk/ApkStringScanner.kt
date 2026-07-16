package com.iiros.scanner.core.apk

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel
import java.io.File
import java.util.zip.ZipFile

/**
 * Lightweight static analyzer that treats an APK as a plain zip archive and
 * looks for suspicious API/string signatures inside its dex files, without
 * doing full bytecode disassembly. `java.util.zip` is available both on the
 * JVM and on Android, so this class has no Android-framework dependency and
 * can run unmodified inside the app.
 */
object ApkStringScanner {

    /** Extracts printable ASCII runs of at least [minLength] characters, `strings(1)`-style. */
    fun extractPrintableStrings(bytes: ByteArray, minLength: Int = SuspiciousApiSignatures.MIN_STRING_LENGTH): List<String> {
        val results = mutableListOf<String>()
        val current = StringBuilder()
        for (b in bytes) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) {
                current.append(c.toChar())
            } else {
                if (current.length >= minLength) results += current.toString()
                current.setLength(0)
            }
        }
        if (current.length >= minLength) results += current.toString()
        return results
    }

    /**
     * Scans a set of raw file contents (entry name -> bytes, typically the
     * `classes*.dex` entries of an APK) for known suspicious signatures.
     */
    fun scanEntries(entries: Map<String, ByteArray>): List<Finding> {
        val dexEntries = entries.filterKeys { it.endsWith(".dex") }
        val haystackEntries = dexEntries.ifEmpty { entries }

        val matchedCategoryIds = mutableSetOf<String>()
        val findings = mutableListOf<Finding>()

        for (category in SuspiciousApiSignatures.CATEGORIES) {
            val matchedSignatures = linkedSetOf<String>()
            for ((_, bytes) in haystackEntries) {
                // Signatures appear as contiguous ASCII within the dex string pool,
                // so a raw Latin-1 decode (no re-encoding, 1 byte = 1 char) is enough
                // to substring-match them without extracting every string first.
                val haystack = String(bytes, Charsets.ISO_8859_1)
                for (signature in category.signatures) {
                    if (haystack.contains(signature)) {
                        matchedSignatures += signature
                    }
                }
            }
            if (matchedSignatures.isNotEmpty()) {
                matchedCategoryIds += category.id
                findings += Finding(
                    id = category.id,
                    title = category.title,
                    detail = matchedSignatures.joinToString(", "),
                    severity = RiskLevel.fromScore(category.weight),
                    weight = category.weight,
                )
            }
        }

        if ("apk.dynamic_code_loading" in matchedCategoryIds && "apk.crypto_payload" in matchedCategoryIds) {
            findings += Finding(
                id = "apk.combo.encrypted_dropper",
                title = "Loads code dynamically after decrypting bundled assets (encrypted dropper pattern)",
                detail = "dynamic class loading + runtime decryption observed together",
                severity = RiskLevel.HIGH,
                weight = 25,
            )
        }
        if ("apk.accessibility_abuse" in matchedCategoryIds && "apk.sms_abuse" in matchedCategoryIds) {
            findings += Finding(
                id = "apk.combo.overlay_otp_theft",
                title = "Combines screen automation with SMS access (OTP-theft pattern)",
                detail = "accessibility automation + SMS access observed together",
                severity = RiskLevel.CRITICAL,
                weight = 30,
            )
        }

        return findings
    }

    /**
     * Opens [apkFile] as a zip and scans its dex entries. Entries larger than
     * [maxEntryBytes] are skipped to keep memory bounded (dex files in
     * malware are almost never larger than this; huge entries are more
     * likely game assets misnamed or multi-dex bloat we don't need in full).
     */
    fun scanApkFile(apkFile: File, maxEntryBytes: Long = 32L * 1024 * 1024): List<Finding> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipFile(apkFile).use { zip ->
            val zipEntries = zip.entries()
            while (zipEntries.hasMoreElements()) {
                val entry = zipEntries.nextElement()
                if (!entry.name.endsWith(".dex")) continue
                if (entry.size in 0..maxEntryBytes) {
                    entries[entry.name] = zip.getInputStream(entry).readBytes()
                }
            }
        }
        return scanEntries(entries)
    }
}
