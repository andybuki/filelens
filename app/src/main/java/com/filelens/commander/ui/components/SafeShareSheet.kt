package com.filelens.commander.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskInfo
import com.filelens.commander.ui.theme.RiskWarning

@Composable
fun SafeShareSheet(
    fileName: String,
    risks: List<RiskItem>,
    onShareOriginal: () -> Unit,
    onShareCleaned: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Check before sharing",
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = fileName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        if (risks.isEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "No risks detected. Safe to share.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Text(
                "This file may reveal:",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            risks.forEach { risk ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    val (color, icon) = when (risk.severity) {
                        RiskSeverity.DANGER -> RiskDanger to Icons.Filled.Error
                        RiskSeverity.WARNING -> RiskWarning to Icons.Filled.Warning
                        RiskSeverity.INFO -> RiskInfo to Icons.Filled.Info
                    }
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(risk.title, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            risk.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onShareOriginal,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Share original")
        }

        if (onShareCleaned != null && risks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onShareCleaned,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create cleaned copy")
            }
        }
    }
}
