@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iiros.scanner.ui.applist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iiros.scanner.R
import com.iiros.scanner.core.RiskLevel
import com.iiros.scanner.data.AppScannerRepository
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.ui.ViewModelFactory
import com.iiros.scanner.ui.components.AppIcon
import com.iiros.scanner.ui.components.RiskBadge

@Composable
fun AppListScreen(
    repository: AppScannerRepository,
    historyRepository: HistoryRepository,
    onAppClick: (String) -> Unit,
) {
    val viewModel: AppListViewModel = viewModel(
        factory = ViewModelFactory { AppListViewModel(repository, historyRepository) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.apps_title)) },
                actions = {
                    if (!uiState.isLoading && uiState.apps.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.runDeepScanAll() },
                            enabled = !uiState.isDeepScanningAll,
                        ) {
                            Text(stringResource(R.string.apps_deep_scan_all))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading -> Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.apps_scanning),
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                uiState.apps.isEmpty() -> Text(
                    text = stringResource(R.string.apps_empty),
                    modifier = Modifier.padding(24.dp),
                )

                else -> Column {
                    val flaggedCount = uiState.apps.count { it.result.riskLevel != RiskLevel.SAFE }
                    Text(
                        text = stringResource(R.string.apps_count_format, uiState.apps.size, flaggedCount),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp),
                    )
                    if (uiState.isDeepScanningAll) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            LinearProgressIndicator(
                                progress = uiState.deepScanCompleted.toFloat() / uiState.deepScanTotal.coerceAtLeast(1),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = stringResource(
                                    R.string.apps_deep_scan_progress_format,
                                    uiState.deepScanCompleted,
                                    uiState.deepScanTotal,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                            )
                        }
                    }
                    LazyColumn {
                        items(uiState.apps, key = { it.info.packageName }) { scanned ->
                            AppRow(scanned, onClick = { onAppClick(scanned.info.packageName) })
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(scanned: ScannedApp, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(scanned.info.icon, modifier = Modifier.size(40.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Text(text = scanned.info.appLabel, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = scanned.info.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RiskBadge(scanned.result.riskLevel)
    }
}
