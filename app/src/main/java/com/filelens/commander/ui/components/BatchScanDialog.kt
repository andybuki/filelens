package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.com.filelens.R
import com.filelens.commander.domain.usecase.BatchScanUseCase
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlInk
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskSafe
import com.filelens.commander.viewmodel.BatchScanState

private val RiskyTint = RiskDanger
private val SafeTint = RiskSafe
private val UnknownTint = Color(0xFF9CA3AF)

@Composable
fun BatchScanDialog(
    state: BatchScanState,
    onCleanAndShareRisky: () -> Unit = {},
    onShareCleanedZip: (String) -> Unit = {},
    onClearCleanedZip: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val risky = state.results.filter { it.bucket == BatchScanUseCase.Bucket.RISKY }
    val safe = state.results.filter { it.bucket == BatchScanUseCase.Bucket.SAFE }
    val unsupported = state.results.filter { it.bucket == BatchScanUseCase.Bucket.UNSUPPORTED }

    var bucketTab by remember { mutableStateOf(BatchScanUseCase.Bucket.RISKY) }
    val visible = when (bucketTab) {
        BatchScanUseCase.Bucket.RISKY -> risky
        BatchScanUseCase.Bucket.SAFE -> safe
        BatchScanUseCase.Bucket.UNSUPPORTED -> unsupported
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(FlAccentTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Security,
                        contentDescription = null,
                        tint = FlAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    if (state.isScanning) stringResource(R.string.batch_scan_title_scanning)
                    else stringResource(R.string.batch_scan_title_done)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.isScanning) {
                    Text(
                        stringResource(R.string.batch_scan_progress, state.current, state.total),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (state.total > 0) state.current.toFloat() / state.total else 0f },
                        modifier = Modifier.fillMaxWidth(),
                        color = FlAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = FlInk3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (state.error != null) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        text = stringResource(R.string.batch_scan_result_count, state.results.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = FlInk3
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BucketCard(
                            icon = Icons.Filled.Warning,
                            label = stringResource(R.string.risk_severity_danger),
                            count = risky.size,
                            tint = RiskyTint,
                            selected = bucketTab == BatchScanUseCase.Bucket.RISKY,
                            onClick = { bucketTab = BatchScanUseCase.Bucket.RISKY },
                            modifier = Modifier.weight(1f)
                        )
                        BucketCard(
                            icon = Icons.Filled.CheckCircle,
                            label = stringResource(R.string.risks_none_detected),
                            count = safe.size,
                            tint = SafeTint,
                            selected = bucketTab == BatchScanUseCase.Bucket.SAFE,
                            onClick = { bucketTab = BatchScanUseCase.Bucket.SAFE },
                            modifier = Modifier.weight(1f)
                        )
                        BucketCard(
                            icon = Icons.Filled.HelpOutline,
                            label = "Other",
                            count = unsupported.size,
                            tint = UnknownTint,
                            selected = bucketTab == BatchScanUseCase.Bucket.UNSUPPORTED,
                            onClick = { bucketTab = BatchScanUseCase.Bucket.UNSUPPORTED },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    if (visible.isEmpty()) {
                        Text(
                            stringResource(R.string.batch_scan_bucket_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = FlInk3
                        )
                    } else {
                        val accent = when (bucketTab) {
                            BatchScanUseCase.Bucket.RISKY -> RiskyTint
                            BatchScanUseCase.Bucket.SAFE -> SafeTint
                            BatchScanUseCase.Bucket.UNSUPPORTED -> UnknownTint
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(visible) { result ->
                                ScanResultRow(result, accent)
                            }
                        }
                    }
                    if (state.isCleaning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.batch_scan_creating_copies), style = MaterialTheme.typography.bodySmall)
                    }
                    state.cleanMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = FlAccent)
                    }
                    if (state.cleanedZipPath != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = {
                            onShareCleanedZip(state.cleanedZipPath)
                            onClearCleanedZip()
                        }) {
                            Text("Share cleaned ZIP")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!state.isScanning && state.error == null && risky.isNotEmpty()) {
                    TextButton(
                        onClick = onCleanAndShareRisky,
                        enabled = !state.isCleaning
                    ) {
                        Text(stringResource(R.string.batch_scan_action_batch_clean))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(if (state.isScanning) stringResource(R.string.action_cancel) else stringResource(R.string.action_close))
                }
            }
        }
    )
}

@Composable
private fun BucketCard(
    icon: ImageVector,
    label: String,
    count: Int,
    tint: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) tint.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                "$count",
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) tint else FlInk,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) tint else FlInk3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun ScanResultRow(result: BatchScanUseCase.ScanResult, accent: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(accent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = result.name,
                style = MaterialTheme.typography.titleSmall,
                color = FlInk,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            result.risks.forEach { risk ->
                Text(
                    text = "• ${risk.title}",
                    style = MaterialTheme.typography.bodySmall,
                    color = FlInk3,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
