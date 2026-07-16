@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iiros.scanner.ui.filescanner

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iiros.scanner.R
import com.iiros.scanner.data.FileScanRepository
import com.iiros.scanner.data.ScannedFile
import com.iiros.scanner.ui.ViewModelFactory
import com.iiros.scanner.ui.components.FindingRow
import com.iiros.scanner.ui.components.RiskBadge

@Composable
fun FileScannerScreen(repository: FileScanRepository) {
    val viewModel: FileScannerViewModel = viewModel(factory = ViewModelFactory { FileScannerViewModel(repository) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshAccessState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshAccessState() }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.files_title)) }) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            when {
                !uiState.hasAccess -> {
                    Text(stringResource(R.string.files_permission_explanation))
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                requestAllFilesAccess(context)
                            } else {
                                legacyPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.files_grant_access)) }
                }

                uiState.isScanning -> Column(modifier = Modifier.fillMaxSize()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 12.dp))
                        Text(stringResource(R.string.files_scanning_progress_format, uiState.scannedCount, uiState.flagged.size))
                    }
                    FlaggedFileList(uiState.flagged, modifier = Modifier.weight(1f))
                }

                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Button(onClick = { viewModel.startScan() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.files_scan_button))
                    }
                    Spacer(Modifier.height(8.dp))
                    if (uiState.hasScannedOnce) {
                        Text(
                            stringResource(R.string.files_scan_result_format, uiState.scannedCount, uiState.flagged.size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    FlaggedFileList(uiState.flagged, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FlaggedFileList(flagged: List<ScannedFile>, modifier: Modifier = Modifier) {
    if (flagged.isEmpty()) return
    LazyColumn(modifier = modifier.fillMaxWidth().padding(top = 8.dp)) {
        items(flagged, key = { it.path }) { file ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(file.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(end = 8.dp))
                    RiskBadge(file.riskLevel)
                }
                Text(
                    file.path,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                file.findings.forEach { finding -> FindingRow(finding) }
            }
            Divider()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.R)
private fun requestAllFilesAccess(context: Context) {
    try {
        val intent = Intent(
            Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
    }
}
