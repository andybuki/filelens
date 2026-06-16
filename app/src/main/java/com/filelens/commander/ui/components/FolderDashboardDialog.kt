package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.com.filelens.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.ui.theme.CategoryApk
import com.filelens.commander.ui.theme.CategoryArchive
import com.filelens.commander.ui.theme.CategoryAudio
import com.filelens.commander.ui.theme.CategoryCertificate
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
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlInk
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
                        Icons.Filled.Analytics,
                        contentDescription = null,
                        tint = FlAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(stringResource(R.string.folder_dashboard_title))
            }
        },
        text = {
            when {
                state.isAnalyzing -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = FlAccent)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(stringResource(R.string.folder_dashboard_analyzing), style = MaterialTheme.typography.bodySmall)
                }
                state.error != null -> Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error)
                state.summary != null -> SummaryBody(state.summary)
                else -> Text(stringResource(R.string.folder_dashboard_no_data))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
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
        FolderHeaderCard(
            path = summary.folderPath,
            fileCount = summary.fileCount,
            totalSize = summary.totalSize
        )

        if (summary.categoryBytes.isNotEmpty() && summary.totalSize > 0) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(icon = Icons.Filled.PieChart, label = stringResource(R.string.folder_dashboard_section_disk_usage))
            Spacer(modifier = Modifier.height(8.dp))
            CategoryStackBar(summary.categoryBytes, summary.totalSize)
            Spacer(modifier = Modifier.height(8.dp))
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
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(icon = Icons.Filled.Storage, label = stringResource(R.string.folder_dashboard_section_largest))
            Spacer(modifier = Modifier.height(4.dp))
            summary.largest.take(5).forEach {
                NameValueRow(it.name, formatSize(it.size))
            }
        }

        if (summary.newest.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(icon = Icons.Filled.Schedule, label = stringResource(R.string.folder_dashboard_section_newest))
            Spacer(modifier = Modifier.height(4.dp))
            summary.newest.take(5).forEach {
                NameValueRow(it.name, formatDateShort(it.lastModified))
            }
        }

        if (summary.unknown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(icon = Icons.Filled.HelpOutline, label = stringResource(R.string.folder_dashboard_section_unknown, summary.unknown.size))
            Spacer(modifier = Modifier.height(4.dp))
            summary.unknown.take(5).forEach {
                NameValueRow(it.name, "")
            }
        }

        if (summary.duplicateCandidates.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            SectionHeader(icon = Icons.Filled.FileCopy, label = stringResource(R.string.folder_dashboard_section_duplicates))
            Spacer(modifier = Modifier.height(4.dp))
            summary.duplicateCandidates.take(5).forEach { group ->
                NameValueRow(
                    title = group.joinToString(", ") { it.name }.take(80),
                    value = "${group.size}× ${formatSize(group.first().size)}"
                )
            }
        }
    }
}

@Composable
private fun FolderHeaderCard(path: String, fileCount: Int, totalSize: Long) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FlAccentTint)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = FlAccent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                path,
                style = MaterialTheme.typography.labelSmall,
                color = FlAccent,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatPill(label = "Files", value = "$fileCount")
            StatPill(label = "Total", value = formatSize(totalSize))
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = FlInk3)
        Text(value, style = MaterialTheme.typography.titleMedium, color = FlInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = FlAccent, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = FlAccent,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun NameValueRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = FlInk,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (value.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                color = FlInk3
            )
        }
    }
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
            .padding(vertical = 3.dp),
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
            color = FlInk,
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
    FileCategory.CERTIFICATE -> CategoryCertificate
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
