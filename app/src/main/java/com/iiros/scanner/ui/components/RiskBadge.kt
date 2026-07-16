package com.iiros.scanner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.iiros.scanner.R
import com.iiros.scanner.core.RiskLevel
import com.iiros.scanner.ui.theme.RiskCritical
import com.iiros.scanner.ui.theme.RiskHigh
import com.iiros.scanner.ui.theme.RiskLow
import com.iiros.scanner.ui.theme.RiskMedium
import com.iiros.scanner.ui.theme.RiskSafe

fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.SAFE -> RiskSafe
    RiskLevel.LOW -> RiskLow
    RiskLevel.MEDIUM -> RiskMedium
    RiskLevel.HIGH -> RiskHigh
    RiskLevel.CRITICAL -> RiskCritical
}

@Composable
fun riskLabel(level: RiskLevel): String = stringResource(
    when (level) {
        RiskLevel.SAFE -> R.string.risk_safe
        RiskLevel.LOW -> R.string.risk_low
        RiskLevel.MEDIUM -> R.string.risk_medium
        RiskLevel.HIGH -> R.string.risk_high
        RiskLevel.CRITICAL -> R.string.risk_critical
    }
)

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    val color = riskColor(level)
    Text(
        text = riskLabel(level),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
