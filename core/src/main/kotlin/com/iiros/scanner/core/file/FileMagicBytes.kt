package com.iiros.scanner.core.file

/**
 * Identifies a file's real format from its leading bytes ("magic numbers"),
 * ignoring whatever its name/extension claims. Only needs a small header
 * (a few dozen bytes), so callers can read just the start of each file
 * instead of the whole thing.
 */
object FileMagicBytes {

    fun detect(header: ByteArray): DetectedFileKind {
        return when {
            matches(header, 0x89, 0x50, 0x4E, 0x47) -> DetectedFileKind.IMAGE // PNG
            matches(header, 0xFF, 0xD8, 0xFF) -> DetectedFileKind.IMAGE // JPEG
            asciiAt(header, 0, "GIF8") -> DetectedFileKind.IMAGE
            matches(header, 0x42, 0x4D) -> DetectedFileKind.IMAGE // BMP
            asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "WEBP") -> DetectedFileKind.IMAGE
            asciiAt(header, 0, "%PDF") -> DetectedFileKind.PDF
            asciiAt(header, 0, "dex\n") -> DetectedFileKind.ANDROID_DEX
            matches(header, 0x50, 0x4B, 0x03, 0x04) ||
                matches(header, 0x50, 0x4B, 0x05, 0x06) ||
                matches(header, 0x50, 0x4B, 0x07, 0x08) -> DetectedFileKind.ZIP_OR_APK
            matches(header, 0x7F, 0x45, 0x4C, 0x46) -> DetectedFileKind.ELF
            matches(header, 0x4D, 0x5A) -> DetectedFileKind.WINDOWS_EXECUTABLE
            asciiAt(header, 4, "ftyp") -> DetectedFileKind.VIDEO // MP4/MOV family
            asciiAt(header, 0, "ID3") ||
                matches(header, 0xFF, 0xFB) ||
                matches(header, 0xFF, 0xF3) ||
                matches(header, 0xFF, 0xF2) -> DetectedFileKind.AUDIO
            asciiAt(header, 0, "OggS") -> DetectedFileKind.AUDIO
            else -> DetectedFileKind.UNKNOWN
        }
    }

    private fun matches(header: ByteArray, vararg bytes: Int): Boolean {
        if (header.size < bytes.size) return false
        for (i in bytes.indices) {
            if ((header[i].toInt() and 0xFF) != bytes[i]) return false
        }
        return true
    }

    private fun asciiAt(header: ByteArray, offset: Int, text: String): Boolean {
        if (header.size < offset + text.length) return false
        for (i in text.indices) {
            if (header[offset + i].toInt().toChar() != text[i]) return false
        }
        return true
    }
}
