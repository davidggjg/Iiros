package com.iiros.scanner.data

import android.content.pm.PackageInfo
import android.os.Build
import java.security.MessageDigest

/** Shared by [AppScannerRepository] (installed apps) and [FileScanRepository] (APKs sitting on disk). */
@Suppress("DEPRECATION")
fun signingCertSha256(packageInfo: PackageInfo): List<String> {
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val signingInfo = packageInfo.signingInfo
        when {
            signingInfo == null -> emptyArray()
            signingInfo.hasMultipleSigners() -> signingInfo.apkContentsSigners
            else -> signingInfo.signingCertificateHistory
        }
    } else {
        packageInfo.signatures ?: emptyArray()
    }

    val digest = MessageDigest.getInstance("SHA-256")
    return signatures.map { signature ->
        digest.reset()
        digest.digest(signature.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
