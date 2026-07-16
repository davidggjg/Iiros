package com.iiros.scanner.core.file

/** Broad kind of a file as claimed by its name/extension. */
enum class FileCategory { IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, OTHER }

/** What a file's leading bytes actually look like, independent of its name. */
enum class DetectedFileKind { IMAGE, VIDEO, AUDIO, PDF, ZIP_OR_APK, ANDROID_DEX, ELF, WINDOWS_EXECUTABLE, UNKNOWN }

object FileTypeClassifier {

    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "heif")
    private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "mkv", "avi", "3gp", "webm", "m4v")
    private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
    private val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")
    private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "jar", "aar")

    /** Extensions whose *legitimate* on-disk format is itself a zip container. */
    private val ZIP_BASED_EXTENSIONS = setOf("docx", "xlsx", "pptx", "zip", "jar", "aar", "apk")

    fun extensionOf(fileName: String): String = fileName.substringAfterLast('.', "").lowercase()

    fun categoryFor(fileName: String): FileCategory {
        val ext = extensionOf(fileName)
        return when {
            ext == "apk" -> FileCategory.APK
            ext in IMAGE_EXTENSIONS -> FileCategory.IMAGE
            ext in VIDEO_EXTENSIONS -> FileCategory.VIDEO
            ext in AUDIO_EXTENSIONS -> FileCategory.AUDIO
            ext in DOCUMENT_EXTENSIONS -> FileCategory.DOCUMENT
            ext in ARCHIVE_EXTENSIONS -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }

    fun isZipBasedExtension(fileName: String): Boolean = extensionOf(fileName) in ZIP_BASED_EXTENSIONS
}
