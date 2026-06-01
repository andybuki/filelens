package com.filelens.commander.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.viewmodel.FolderDashboardState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FolderDashboardDialog(
    state: FolderDashboardState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folder dashboard") },
        text = {
            when {
                state.isAnalyzing -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analyzing folder…", style = MaterialTheme.typography.bodySmall)
                }
                state.error != null -> Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                state.summary != null -> SummaryBody(state.summary)
                else -> Text("No data")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun SummaryBody(summary: com.filelens.commander.domain.usecase.FolderAnalysisUseCase.FolderSummary) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 500.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Folder", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            summary.folderPath,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("${summary.fileCount} file(s) · ${formatSize(summary.totalSize)} total", style = MaterialTheme.typography.bodyMedium)

        if (summary.categoryCounts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("By type")
            summary.categoryCounts.entries.sortedByDescending { it.value }.forEach { (cat, n) ->
                BulletRow("${cat.name.lowercase().replaceFirstChar { it.uppercase() }}: $n")
            }
        }

        if (summary.largest.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Largest files")
            summary.largest.take(5).forEach {
                BulletRow("${it.name} — ${formatSize(it.size)}")
            }
        }

        if (summary.newest.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Newest files")
            summary.newest.take(5).forEach {
                BulletRow("${it.name} — ${formatDate(it.lastModified)}")
            }
        }

        if (summary.unknown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Unknown / unclassified (${summary.unknown.size})")
            summary.unknown.take(5).forEach {
                BulletRow(it.name)
            }
        }

        if (summary.duplicateCandidates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Possible duplicates (same size)")
            summary.duplicateCandidates.take(5).forEach { group ->
                BulletRow("${group.size}× ${formatSize(group.first().size)}: ${group.joinToString(", ") { it.name }.take(160)}")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}

@Composable
private fun BulletRow(text: String) {
    Text(
        "• $text",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}

private fun formatDate(ts: Long): String {
    if (ts <= 0) return "?"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
