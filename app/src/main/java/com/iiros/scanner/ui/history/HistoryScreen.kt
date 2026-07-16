@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.iiros.scanner.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
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
import com.iiros.scanner.data.HistoryRepository
import com.iiros.scanner.data.db.ScanHistoryEntity
import com.iiros.scanner.data.db.ScanType
import com.iiros.scanner.ui.ViewModelFactory
import com.iiros.scanner.ui.components.RiskBadge
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(historyRepository: HistoryRepository) {
    val viewModel: HistoryViewModel = viewModel(factory = ViewModelFactory { HistoryViewModel(historyRepository) })
    val history by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                actions = {
                    TextButton(onClick = { viewModel.clear() }) {
                        Text(stringResource(R.string.history_clear))
                    }
                },
            )
        },
    ) { padding ->
        if (history.isEmpty()) {
            Text(
                stringResource(R.string.history_empty),
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(history, key = { it.id }) { entry ->
                    HistoryRow(entry)
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: ScanHistoryEntity) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${typeLabel(entry.type)} · ${DateFormat.getDateTimeInstance().format(Date(entry.scannedAtEpochMillis))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RiskBadge(runCatching { RiskLevel.valueOf(entry.riskLevel) }.getOrDefault(RiskLevel.SAFE))
    }
}

@Composable
private fun typeLabel(type: ScanType): String = when (type) {
    ScanType.APP -> stringResource(R.string.history_type_app)
    ScanType.URL -> stringResource(R.string.history_type_url)
}
