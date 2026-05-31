package com.filelens.commander.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.ui.theme.*

@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = iconForCategory(item.fileCategory)
    val alpha = if (item.isHidden) 0.5f else 1f

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .alpha(alpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = item.fileCategory.name,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else tint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (item.size != null || item.lastModified != null) {
                Text(
                    text = buildSubtitle(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

private fun iconForCategory(category: FileCategory): Pair<ImageVector, Color> {
    return when (category) {
        FileCategory.DIRECTORY -> Icons.Filled.Folder to CategoryDirectory
        FileCategory.IMAGE -> Icons.Filled.Image to CategoryImage
        FileCategory.VIDEO -> Icons.Filled.VideoFile to CategoryVideo
        FileCategory.AUDIO -> Icons.Filled.AudioFile to CategoryAudio
        FileCategory.DOCUMENT -> Icons.Filled.Description to CategoryDocument
        FileCategory.SPREADSHEET -> Icons.Filled.TableChart to CategorySpreadsheet
        FileCategory.PRESENTATION -> Icons.Filled.Slideshow to CategoryPresentation
        FileCategory.ARCHIVE -> Icons.Filled.Archive to CategoryArchive
        FileCategory.CODE -> Icons.Filled.Code to CategoryCode
        FileCategory.TEXT -> Icons.Filled.TextSnippet to CategoryText
        FileCategory.PDF -> Icons.Filled.PictureAsPdf to CategoryPdf
        FileCategory.APK -> Icons.Filled.Android to CategoryApk
        FileCategory.DATABASE -> Icons.Filled.Storage to CategoryDatabase
        FileCategory.FONT -> Icons.Filled.FontDownload to CategoryDirectory
        FileCategory.EXECUTABLE -> Icons.Filled.Article to CategoryUnknown
        FileCategory.UNKNOWN -> Icons.Filled.InsertDriveFile to CategoryUnknown
    }
}

private fun buildSubtitle(item: FileItem): String {
    val parts = mutableListOf<String>()
    item.size?.let { parts.add(formatFileSize(it)) }
    item.lastModified?.let { parts.add(formatDate(it)) }
    return parts.joinToString(" · ")
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
