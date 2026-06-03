package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.ui.theme.CategoryApk
import com.filelens.commander.ui.theme.CategoryArchive
import com.filelens.commander.ui.theme.CategoryAudio
import com.filelens.commander.ui.theme.CategoryCode
import com.filelens.commander.ui.theme.CategoryDatabase
import com.filelens.commander.ui.theme.CategoryDocument
import com.filelens.commander.ui.theme.CategoryExecutable
import com.filelens.commander.ui.theme.CategoryFont
import com.filelens.commander.ui.theme.CategoryImage
import com.filelens.commander.ui.theme.CategoryPdf
import com.filelens.commander.ui.theme.CategoryPresentation
import com.filelens.commander.ui.theme.CategorySpreadsheet
import com.filelens.commander.ui.theme.CategoryText
import com.filelens.commander.ui.theme.CategoryUnknown
import com.filelens.commander.ui.theme.CategoryVideo
import com.filelens.commander.ui.theme.FlInk3
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

        if (summary.categoryBytes.isNotEmpty() && summary.totalSize > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle("Disk usage by type")
            CategoryStackBar(summary.categoryBytes, summary.totalSize)
            Spacer(modifier = Modifier.height(6.dp))
            summary.categoryBytes.entries
                .sortedByDescending { it.value }
                .take(8)
                .forEach { (cat, bytes) ->
                    val count = summary.categoryCounts[cat] ?: 0
                    CategoryLegendRow(
                        category = cat,
                        bytes = bytes,
                        fileCount = count,
                        totalBytes = summary.totalSize
                    )
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
                BulletRow("${it.name} — ${formatDateShort(it.lastModified)}")
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

@Composable
private fun CategoryStackBar(bytes: Map<FileCategory, Long>, totalBytes: Long) {
    val ordered = bytes.entries.sortedByDescending { it.value }.filter { it.value > 0 }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        for ((cat, b) in ordered) {
            val fraction = (b.toFloat() / totalBytes.coerceAtLeast(1).toFloat()).coerceAtLeast(0.005f)
            Box(
                modifier = Modifier
                    .weight(fraction)
                    .fillMaxHeight()
                    .background(categoryColor(cat))
            )
        }
    }
}

@Composable
private fun CategoryLegendRow(
    category: FileCategory,
    bytes: Long,
    fileCount: Int,
    totalBytes: Long
) {
    val pct = if (totalBytes > 0) bytes * 100.0 / totalBytes else 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(categoryColor(category))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.name.lowercase().replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$fileCount · ${formatSize(bytes)} · %.1f%%".format(pct),
            style = MaterialTheme.typography.labelSmall,
            color = FlInk3
        )
    }
}

private fun categoryColor(cat: FileCategory): Color = when (cat) {
    FileCategory.IMAGE -> CategoryImage
    FileCategory.VIDEO -> CategoryVideo
    FileCategory.AUDIO -> CategoryAudio
    FileCategory.DOCUMENT -> CategoryDocument
    FileCategory.SPREADSHEET -> CategorySpreadsheet
    FileCategory.PRESENTATION -> CategoryPresentation
    FileCategory.ARCHIVE -> CategoryArchive
    FileCategory.CODE -> CategoryCode
    FileCategory.TEXT -> CategoryText
    FileCategory.PDF -> CategoryPdf
    FileCategory.APK -> CategoryApk
    FileCategory.DATABASE -> CategoryDatabase
    FileCategory.FONT -> CategoryFont
    FileCategory.EXECUTABLE -> CategoryExecutable
    FileCategory.UNKNOWN, FileCategory.DIRECTORY -> CategoryUnknown
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    return "%.2f GB".format(mb / 1024.0)
}

private fun formatDateShort(ts: Long): String {
    if (ts <= 0) return "?"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
}
