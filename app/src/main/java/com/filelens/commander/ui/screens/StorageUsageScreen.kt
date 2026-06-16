package com.filelens.commander.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.usecase.FolderAnalysisUseCase
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
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.FlBg
import com.filelens.commander.ui.theme.FlInk
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.FlNavy
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.ui.theme.FlSurface
import com.filelens.commander.ui.theme.FlText
import com.filelens.commander.viewmodel.StorageUsageViewModel

@Composable
fun StorageUsageScreen(
    onBack: () -> Unit,
    viewModel: StorageUsageViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(FlBg)
    ) {
        Header(onBack = onBack, isRefreshing = state.isRefreshing)
        when {
            state.isLoading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp)
                )
            }
            state.error != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.error ?: "", color = MaterialTheme.colorScheme.error)
            }
            else -> StorageBody(state = state)
        }
    }
}

@Composable
private fun StorageBody(state: com.filelens.commander.viewmodel.StorageUsageState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SummaryCard(
                deviceLabel = state.deviceLabel,
                totalBytes = state.totalBytes,
                freeBytes = state.freeBytes,
                summary = state.summary
            )
        }
        val summary = state.summary
        val rows = summary?.categoryBytes?.entries
            ?.filter { it.value > 0 }
            ?.sortedByDescending { it.value }
            .orEmpty()
        if (rows.isNotEmpty()) {
            item {
                SectionLabel(stringResource(R.string.storage_section_categories))
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    rows.forEachIndexed { index, entry ->
                        CategoryRow(
                            category = entry.key,
                            bytes = entry.value,
                            fileCount = summary?.categoryCounts?.get(entry.key) ?: 0,
                            isFirst = index == 0,
                            isLast = index == rows.lastIndex
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit, isRefreshing: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlNavy)
            .padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.cd_back),
                tint = FlOnAccent
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.storage_usage_title),
            style = MaterialTheme.typography.titleLarge,
            color = FlOnAccent,
            modifier = Modifier.weight(1f)
        )
        if (isRefreshing) {
            CircularProgressIndicator(
                color = FlOnAccent,
                strokeWidth = 2.dp,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label.uppercase(),
        style = Eyebrow,
        color = FlInk3,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun SummaryCard(
    deviceLabel: String,
    totalBytes: Long,
    freeBytes: Long,
    summary: FolderAnalysisUseCase.FolderSummary?
) {
    val scannedBytes = summary?.totalSize ?: 0L
    val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)
    // Some device states under-report scanned bytes (system / app-private
    // space we can't reach); fall back to StatFs' used delta when bigger so
    // the headline stays honest.
    val displayUsed = maxOf(usedBytes, scannedBytes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FlSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = deviceLabel,
                style = MaterialTheme.typography.titleMedium,
                color = FlInk,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (totalBytes > 0)
                    stringResource(R.string.storage_used_of, formatSize(displayUsed), formatSize(totalBytes))
                else
                    formatSize(displayUsed),
                style = MaterialTheme.typography.bodySmall,
                color = FlText
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        CategoryStackBar(
            categoryBytes = summary?.categoryBytes.orEmpty(),
            scannedBytes = scannedBytes,
            usedBytes = displayUsed,
            totalBytes = if (totalBytes > 0) totalBytes else displayUsed
        )
        if (totalBytes > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.storage_free_label, formatSize(freeBytes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = FlText,
                    modifier = Modifier.weight(1f)
                )
                if (scannedBytes > 0) {
                    Text(
                        text = stringResource(R.string.storage_scanned_label, formatSize(scannedBytes)),
                        style = MaterialTheme.typography.bodySmall,
                        color = FlInk3
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Legend(summary?.categoryBytes.orEmpty())
    }
}

@Composable
private fun CategoryStackBar(
    categoryBytes: Map<FileCategory, Long>,
    scannedBytes: Long,
    usedBytes: Long,
    totalBytes: Long
) {
    val barShape = RoundedCornerShape(7.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(barShape)
            .background(Color(0xFFE3E5EA))
    ) {
        val ordered = categoryBytes.entries
            .filter { it.value > 0 }
            .sortedByDescending { it.value }
        val denom = totalBytes.coerceAtLeast(1L).toFloat()
        for ((cat, bytes) in ordered) {
            val fraction = (bytes.toFloat() / denom).coerceIn(0f, 1f)
            if (fraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(fraction)
                        .fillMaxHeight()
                        .background(categoryColor(cat))
                )
            }
        }
        // "System / other" wedge for bytes we can see used on the volume
        // but didn't scan (e.g. app-private, system partitions).
        if (totalBytes > scannedBytes && usedBytes > scannedBytes) {
            val sysFraction = ((usedBytes - scannedBytes).toFloat() / denom).coerceIn(0f, 1f)
            if (sysFraction > 0f) {
                Box(
                    modifier = Modifier
                        .weight(sysFraction)
                        .fillMaxHeight()
                        .background(CategoryUnknown)
                )
            }
        }
    }
}

@Composable
private fun Legend(categoryBytes: Map<FileCategory, Long>) {
    val top = categoryBytes.entries
        .filter { it.value > 0 }
        .sortedByDescending { it.value }
    if (top.isEmpty()) return
    // Two-column legend wrap. We chunk into rows of 4 since most phones
    // fit four short chips per line in this card width.
    top.chunked(4).forEach { rowItems ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowItems.forEach { entry ->
                LegendChip(category = entry.key)
            }
        }
    }
}

@Composable
private fun LegendChip(category: FileCategory) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(categoryColor(category))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = categoryLabel(category),
            style = MaterialTheme.typography.labelSmall,
            color = FlText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryRow(
    category: FileCategory,
    bytes: Long,
    fileCount: Int,
    isFirst: Boolean,
    isLast: Boolean
) {
    val shape = when {
        isFirst && isLast -> RoundedCornerShape(14.dp)
        isFirst -> RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
        isLast -> RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp, bottomStart = 14.dp, bottomEnd = 14.dp)
        else -> RoundedCornerShape(2.dp)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FlSurface)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(categoryColor(category).copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = null,
                tint = categoryColor(category),
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = categoryLabel(category),
                style = MaterialTheme.typography.titleSmall,
                color = FlInk,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = pluralizeFiles(fileCount),
                style = MaterialTheme.typography.bodySmall,
                color = FlInk3,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = formatSize(bytes),
            style = MaterialTheme.typography.titleSmall,
            color = FlText,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = FlInk3,
            modifier = Modifier.size(18.dp)
        )
    }
}

private fun pluralizeFiles(count: Int): String =
    if (count == 1) "1 file" else "$count files"

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

private fun categoryIcon(cat: FileCategory): ImageVector = when (cat) {
    FileCategory.IMAGE -> Icons.Filled.Image
    FileCategory.VIDEO -> Icons.Filled.Videocam
    FileCategory.AUDIO -> Icons.Filled.MusicNote
    FileCategory.DOCUMENT -> Icons.Filled.Description
    FileCategory.SPREADSHEET -> Icons.Filled.TableChart
    FileCategory.PRESENTATION -> Icons.Filled.Description
    FileCategory.ARCHIVE -> Icons.Filled.Archive
    FileCategory.CODE -> Icons.Filled.Code
    FileCategory.TEXT -> Icons.Filled.TextSnippet
    FileCategory.PDF -> Icons.Filled.PictureAsPdf
    FileCategory.APK -> Icons.Filled.Android
    FileCategory.DATABASE -> Icons.Filled.Storage
    FileCategory.FONT -> Icons.Filled.TextSnippet
    FileCategory.EXECUTABLE -> Icons.Filled.Memory
    FileCategory.CERTIFICATE -> Icons.Filled.VpnKey
    FileCategory.UNKNOWN, FileCategory.DIRECTORY -> Icons.Filled.Folder
}

private fun categoryLabel(cat: FileCategory): String = when (cat) {
    FileCategory.IMAGE -> "Images"
    FileCategory.VIDEO -> "Video"
    FileCategory.AUDIO -> "Audio"
    FileCategory.DOCUMENT -> "Documents"
    FileCategory.SPREADSHEET -> "Spreadsheets"
    FileCategory.PRESENTATION -> "Presentations"
    FileCategory.ARCHIVE -> "Archives"
    FileCategory.CODE -> "Code"
    FileCategory.TEXT -> "Text"
    FileCategory.PDF -> "PDFs"
    FileCategory.APK -> "APKs"
    FileCategory.DATABASE -> "Databases"
    FileCategory.FONT -> "Fonts"
    FileCategory.EXECUTABLE -> "Executables"
    FileCategory.CERTIFICATE -> "Certificates"
    FileCategory.UNKNOWN -> "Other"
    FileCategory.DIRECTORY -> "Folders"
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.2f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}
