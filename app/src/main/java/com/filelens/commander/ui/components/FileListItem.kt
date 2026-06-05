package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
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
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.ui.theme.*
import com.filelens.commander.ui.util.LocalSizeUnits
import com.filelens.commander.ui.util.formatFileSize

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
    item: FileItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    riskSeverity: RiskSeverity? = null
) {
    val (icon, tint) = iconForCategory(item.fileCategory)
    val rowAlpha = if (item.isHidden) 0.55f else 1f
    val isDir = item.fileCategory == FileCategory.DIRECTORY
    val showExtBadge = !isDir && item.fileCategory != FileCategory.UNKNOWN && item.extension.isNotEmpty()
    val sizeUnits = LocalSizeUnits.current

    val a11y = buildA11yDescription(item, sizeUnits, riskSeverity, isSelected)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .clip(RoundedCornerShape(11.dp))
            .background(if (isSelected) FlAccentTint2 else Color.Transparent)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .alpha(rowAlpha)
            // Merge child Text/Icon descendants so TalkBack reads one row at a
            // time instead of stepping through name, size, badge, dot.
            .semantics(mergeDescendants = true) {
                contentDescription = a11y
                role = Role.Button
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(FlAccent)
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.fileCategory.name,
                    tint = tint,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isSelected) FlAccentBright else MaterialTheme.colorScheme.onSurface
                )
                val subtitle = buildSubtitle(item, sizeUnits)
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MonoSmall,
                        color = FlInk3,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (showExtBadge) {
                Spacer(modifier = Modifier.width(8.dp))
                ExtBadge(extension = item.extension, tint = tint)
            }
            if (riskSeverity != null) {
                Spacer(modifier = Modifier.width(8.dp))
                SeverityDot(severity = riskSeverity)
            }
        }
    }
}

@Composable
private fun ExtBadge(extension: String, tint: Color) {
    Text(
        text = extension.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = tint,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

@Composable
private fun SeverityDot(severity: RiskSeverity) {
    val (color, label) = when (severity) {
        RiskSeverity.DANGER -> RiskDanger to "Danger risk"
        RiskSeverity.WARNING -> RiskWarning to "Warning risk"
        RiskSeverity.INFO -> RiskInfo to "Info notice"
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
            .semantics { contentDescription = label }
    )
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
        FileCategory.TEXT -> Icons.Filled.Description to CategoryText
        FileCategory.PDF -> Icons.Filled.PictureAsPdf to CategoryPdf
        FileCategory.APK -> Icons.Filled.Android to CategoryApk
        FileCategory.DATABASE -> Icons.Filled.Storage to CategoryDatabase
        FileCategory.FONT -> Icons.Filled.FontDownload to CategoryFont
        FileCategory.EXECUTABLE -> Icons.Filled.Terminal to CategoryExecutable
        FileCategory.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile to CategoryUnknown
    }
}

private fun buildA11yDescription(
    item: FileItem,
    units: SizeUnits,
    severity: RiskSeverity?,
    isSelected: Boolean
): String {
    val parts = mutableListOf<String>()
    val kind = if (item.fileCategory == FileCategory.DIRECTORY) "Folder" else "File"
    parts += "$kind ${item.name}"
    if (item.fileCategory != FileCategory.DIRECTORY) {
        item.size?.let { parts += formatFileSize(it, units) }
    }
    item.lastModified?.let { parts += "modified ${formatDate(it)}" }
    if (item.extension.isNotEmpty() && item.fileCategory != FileCategory.DIRECTORY) {
        parts += "${item.extension.uppercase()} file"
    }
    severity?.let {
        parts += when (it) {
            RiskSeverity.DANGER -> "danger risk"
            RiskSeverity.WARNING -> "warning risk"
            RiskSeverity.INFO -> "info notice"
        }
    }
    if (isSelected) parts += "selected"
    return parts.joinToString(", ")
}

private fun buildSubtitle(item: FileItem, units: SizeUnits): String {
    if (item.fileCategory == FileCategory.DIRECTORY) {
        return item.lastModified?.let { formatDate(it) } ?: ""
    }
    val parts = mutableListOf<String>()
    item.size?.let { parts.add(formatFileSize(it, units)) }
    item.lastModified?.let { parts.add(formatDate(it)) }
    return parts.joinToString(" · ")
}

fun formatDate(timestamp: Long): String {
    val sdf = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(timestamp))
}
