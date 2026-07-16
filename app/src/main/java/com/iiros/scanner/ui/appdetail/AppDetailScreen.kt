@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iiros.scanner.ui.appdetail

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iiros.scanner.R
import com.iiros.scanner.core.permissions.DangerousPermissions
import com.iiros.scanner.data.AppScannerRepository
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.ui.ViewModelFactory
import com.iiros.scanner.ui.components.AppIcon
import com.iiros.scanner.ui.components.FindingRow
import com.iiros.scanner.ui.components.RiskBadge

@Composable
fun AppDetailScreen(
    packageName: String,
    repository: AppScannerRepository,
    historyRepository: HistoryRepository,
    onBack: () -> Unit,
) {
    val viewModel: AppDetailViewModel = viewModel(
        key = packageName,
        factory = ViewModelFactory { AppDetailViewModel(packageName, repository, historyRepository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.app?.appLabel ?: packageName) },
                navigationIcon = {
                    OutlinedButton(onClick = onBack) { Text("←") }
                },
            )
        },
    ) { padding ->
        val app = uiState.app
        val result = uiState.result
        when {
            uiState.isLoading -> Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            app == null || result == null -> Column(modifier = Modifier.padding(padding)) {
                Text(stringResource(R.string.apps_empty))
            }

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(app.icon, modifier = Modifier.size(56.dp))
                        Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(app.appLabel, style = MaterialTheme.typography.titleLarge)
                            Text(
                                app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RiskBadge(result.riskLevel)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.score_format, result.score))
                    if (app.isSystemApp) {
                        Text(
                            stringResource(R.string.detail_system_app),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Text(stringResource(R.string.detail_install_source), style = MaterialTheme.typography.titleSmall)
                    Text(app.installerPackageName ?: stringResource(R.string.detail_install_source_unknown))
                    Spacer(Modifier.height(16.dp))

                    Text(stringResource(R.string.detail_signing_cert), style = MaterialTheme.typography.titleSmall)
                    if (app.signingCertSha256.isEmpty()) {
                        Text(stringResource(R.string.detail_install_source_unknown))
                    } else {
                        app.signingCertSha256.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { viewModel.runDeepScan() },
                        enabled = !uiState.isDeepScanning,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (uiState.isDeepScanning) {
                                stringResource(R.string.detail_deep_scan_running)
                            } else {
                                stringResource(R.string.detail_deep_scan_button)
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    Text(stringResource(R.string.detail_findings), style = MaterialTheme.typography.titleMedium)
                    if (result.findings.isEmpty()) {
                        Text(stringResource(R.string.detail_no_findings))
                    }
                }
                items(result.findings) { finding -> FindingRow(finding) }

                item {
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.detail_permissions), style = MaterialTheme.typography.titleMedium)
                }
                items(app.permissions) { permission ->
                    val isDangerous = permission in DangerousPermissions.PRIVACY_SENSITIVE ||
                        permission in DangerousPermissions.CONTROL_ESCALATION
                    Text(
                        text = permission.substringAfterLast('.'),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDangerous) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", app.packageName, null),
                            )
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.detail_open_app_settings)) }
                }
            }
        }
    }
}
