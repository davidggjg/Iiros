package com.iiros.scanner.core.scoring

/**
 * Everything the risk engine needs about one installed app. Deliberately
 * framework-free (plain strings/booleans) so the Android layer builds this
 * from `PackageManager` and the core module never has to import `android.*`.
 */
data class AppScanInput(
    val packageName: String,
    val appLabel: String,
    val permissions: List<String>,
    val isSystemApp: Boolean,
    val installerPackageName: String?,
    val signingCertSha256: List<String>,
    val targetSdkVersion: Int,
)
