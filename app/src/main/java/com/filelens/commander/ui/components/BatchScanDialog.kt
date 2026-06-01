package com.filelens.commander.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.usecase.BatchScanUseCase
import com.filelens.commander.viewmodel.BatchScanState

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
        title = { Text(if (state.isScanning) "Scanning folder..." else "Privacy audit") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (state.isScanning) {
                    Text("${state.current} / ${state.total}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { if (state.total > 0) state.current.toFloat() / state.total else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.currentFile,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (state.error != null) {
                    Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                } else {
                    Text(
                        text = "${state.results.size} file(s) scanned",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        BucketChip("Risky (${risky.size})", bucketTab == BatchScanUseCase.Bucket.RISKY) { bucketTab = BatchScanUseCase.Bucket.RISKY }
                        Spacer(modifier = Modifier.width(6.dp))
                        BucketChip("Safe (${safe.size})", bucketTab == BatchScanUseCase.Bucket.SAFE) { bucketTab = BatchScanUseCase.Bucket.SAFE }
                        Spacer(modifier = Modifier.width(6.dp))
                        BucketChip("Unsupported (${unsupported.size})", bucketTab == BatchScanUseCase.Bucket.UNSUPPORTED) { bucketTab = BatchScanUseCase.Bucket.UNSUPPORTED }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    if (visible.isEmpty()) {
                        Text(
                            "Nothing in this bucket.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                            items(visible) { result ->
                                ScanResultRow(result)
                            }
                        }
                    }
                    if (state.isCleaning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Creating cleaned copies…", style = MaterialTheme.typography.bodySmall)
                    }
                    state.cleanMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
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
                        Text("Batch clean")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(if (state.isScanning) "Cancel" else "Close")
                }
            }
        }
    )
}

@Composable
private fun BucketChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors()
    )
}

@Composable
private fun ScanResultRow(result: BatchScanUseCase.ScanResult) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = result.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        result.risks.forEach { risk ->
            Text(
                text = "• ${risk.title}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
