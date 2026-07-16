package com.iiros.scanner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.iiros.scanner.core.Finding

@Composable
fun FindingRow(finding: Finding, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Spacer(
            modifier = Modifier
                .padding(top = 6.dp, end = 10.dp)
                .size(10.dp)
                .background(riskColor(finding.severity), CircleShape)
        )
        Column {
            Text(text = finding.title, style = MaterialTheme.typography.bodyMedium)
            if (finding.detail.isNotBlank()) {
                Text(
                    text = finding.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
