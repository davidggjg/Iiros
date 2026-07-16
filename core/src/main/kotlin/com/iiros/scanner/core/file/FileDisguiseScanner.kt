package com.iiros.scanner.core.file

import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel

/**
 * Flags files whose real content doesn't match what their name/extension
 * claims — the classic "photo.jpg" that is actually an APK or native
 * executable. Only looks at a short header, so it's cheap enough to run
 * across every file on a device.
 */
object FileDisguiseScanner {

    private val EXECUTABLE_LIKE = setOf(
        DetectedFileKind.ANDROID_DEX,
        DetectedFileKind.ELF,
        DetectedFileKind.WINDOWS_EXECUTABLE,
    )

    fun scan(fileName: String, header: ByteArray): List<Finding> {
        val detected = FileMagicBytes.detect(header)
        if (detected == DetectedFileKind.UNKNOWN) return emptyList()

        val claimed = FileTypeClassifier.categoryFor(fileName)

        if (detected in EXECUTABLE_LIKE) {
            return listOf(
                Finding(
                    id = "file.disguised_executable",
                    title = "File extension doesn't match its content — looks like executable code",
                    detail = "\"$fileName\" claims to be ${claimed.name.lowercase()} but starts with ${detected.name.lowercase()} bytes",
                    severity = RiskLevel.CRITICAL,
                    weight = 60,
                )
            )
        }

        val claimsNonArchiveMedia = claimed == FileCategory.IMAGE ||
            claimed == FileCategory.VIDEO ||
            claimed == FileCategory.AUDIO ||
            claimed == FileCategory.DOCUMENT
        if (detected == DetectedFileKind.ZIP_OR_APK &&
            claimsNonArchiveMedia &&
            !FileTypeClassifier.isZipBasedExtension(fileName)
        ) {
            return listOf(
                Finding(
                    id = "file.disguised_archive",
                    title = "File extension doesn't match its content — looks like an archive or APK",
                    detail = "\"$fileName\" claims to be ${claimed.name.lowercase()} but is actually a zip/APK-format file",
                    severity = RiskLevel.HIGH,
                    weight = 40,
                )
            )
        }

        return emptyList()
    }
}
