package com.iiros.scanner.ui.urlscanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iiros.scanner.R
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.ui.ViewModelFactory
import com.iiros.scanner.ui.components.FindingRow
import com.iiros.scanner.ui.components.RiskBadge

@Composable
fun UrlScannerScreen(historyRepository: HistoryRepository) {
    val viewModel: UrlScannerViewModel = viewModel(factory = ViewModelFactory { UrlScannerViewModel(historyRepository) })
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.url_title)) }) },
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item {
                OutlinedTextField(
                    value = uiState.input,
                    onValueChange = viewModel::onInputChanged,
                    label = { Text(stringResource(R.string.url_input_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.scan() },
                    enabled = !uiState.isScanning && uiState.input.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.url_scan_button)) }
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.url_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }

            val result = uiState.result
            if (result != null) {
                item {
                    Column {
                        RiskBadge(result.riskLevel)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.score_format, result.score))
                        Spacer(Modifier.height(12.dp))
                        if (result.findings.isEmpty()) {
                            Text(stringResource(R.string.url_result_safe))
                        }
                    }
                }
                items(result.findings) { finding -> FindingRow(finding) }
            }
        }
    }
}
