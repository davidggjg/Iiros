package com.iiros.scanner.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.iiros.scanner.core.Finding
import com.iiros.scanner.core.RiskLevel
import com.iiros.scanner.core.apk.ApkStringScanner
import com.iiros.scanner.core.file.FileDisguiseScanner
import com.iiros.scanner.core.file.FileTypeClassifier
import com.iiros.scanner.core.malware.MalwareSignatureDatabase
import java.io.File
import java.io.FileInputStream

/**
 * Walks the device's shared storage (photos, videos, documents, downloads, …)
 * looking for files whose real content doesn't match their name, and for
 * stray APK files sitting on disk (which get the same static analysis as an
 * installed app). Everything stays on-device: files are only read locally,
 * never uploaded.
 */
class FileScanRepository(private val context: Context) {

    private val malwareSignatureDatabase = MalwareSignatureDatabase()

    fun hasFullAccess(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

    suspend fun scanAll(onProgress: suspend (FileScanProgress) -> Unit): List<ScannedFile> {
        val flagged = mutableListOf<ScannedFile>()
        var scanned = 0
        walk(Environment.getExternalStorageDirectory()) { file ->
            scanned++
            val result = scanFile(file)
            if (result != null) flagged += result
            if (scanned % 25 == 0 || result != null) {
                onProgress(FileScanProgress(scanned, file.path, newlyFlagged = result))
            }
        }
        onProgress(FileScanProgress(scanned, ""))
        return flagged
    }

    private suspend fun walk(dir: File, action: suspend (File) -> Unit) {
        // Android/data and Android/obb hold other apps' private cache/data —
        // not useful for malware triage and can be enormous.
        if (dir.name == "data" && dir.parentFile?.name == "Android") return
        if (dir.name == "obb" && dir.parentFile?.name == "Android") return

        val entries = try {
            dir.listFiles()
        } catch (e: SecurityException) {
            null
        } ?: return

        for (entry in entries) {
            if (entry.isDirectory) {
                walk(entry, action)
            } else if (entry.isFile) {
                action(entry)
            }
        }
    }

    private fun scanFile(file: File): ScannedFile? {
        val header = readHeader(file) ?: return null
        val disguiseFindings = FileDisguiseScanner.scan(file.name, header)
        val apkFindings = if (FileTypeClassifier.extensionOf(file.name) == "apk") analyzeAsApk(file) else emptyList()

        val allFindings = disguiseFindings + apkFindings
        if (allFindings.isEmpty()) return null

        return ScannedFile(
            path = file.path,
            name = file.name,
            category = FileTypeClassifier.categoryFor(file.name),
            sizeBytes = file.length(),
            findings = allFindings,
        )
    }

    private fun readHeader(file: File, maxBytes: Int = 64): ByteArray? = try {
        FileInputStream(file).use { input ->
            val buffer = ByteArray(maxBytes)
            val read = input.read(buffer)
            if (read <= 0) null else buffer.copyOf(read)
        }
    } catch (e: Exception) {
        null
    }

    private fun analyzeAsApk(file: File): List<Finding> {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val packageInfo = try {
            pm.getPackageArchiveInfo(file.path, flags)
        } catch (e: Exception) {
            null
        } ?: return listOf(
            Finding(
                id = "file.apk_unparseable",
                title = "Names itself an APK but couldn't be parsed as one",
                detail = file.path,
                severity = RiskLevel.MEDIUM,
                weight = 15,
            )
        )

        val appInfo = packageInfo.applicationInfo
        val label = if (appInfo != null) {
            appInfo.sourceDir = file.path
            appInfo.publicSourceDir = file.path
            try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                file.name
            }
        } else {
            file.name
        }

        val packageName = packageInfo.packageName ?: file.name
        val signingHashes = signingCertSha256(packageInfo)
        val malwareFindings = malwareSignatureDatabase.lookup(packageName, label, signingHashes)
        val dexFindings = try {
            ApkStringScanner.scanApkFile(file)
        } catch (e: Exception) {
            emptyList()
        }

        val presenceFinding = Finding(
            id = "file.apk_in_storage",
            title = "APK file sitting in storage (separate from any installed copy)",
            detail = "package=$packageName label=$label",
            severity = RiskLevel.LOW,
            weight = 5,
        )

        return listOf(presenceFinding) + malwareFindings + dexFindings
    }
}
