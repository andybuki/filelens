package com.filelens.commander.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.HorizontalDivider
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
import com.filelens.commander.ui.theme.FlBlue
import com.filelens.commander.ui.theme.FlBlue50
import com.filelens.commander.ui.theme.FlBorder
import com.filelens.commander.ui.theme.FlInk
import com.filelens.commander.ui.theme.FlMuted
import com.filelens.commander.ui.theme.FlSurface3
import com.filelens.commander.ui.theme.FlText2
import com.filelens.commander.ui.theme.MonoSmall
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskInfo
import com.filelens.commander.ui.theme.RiskWarning
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
    val isDir = item.fileCategory == FileCategory.DIRECTORY
    val icon = iconForCategory(item.fileCategory)
    val tileBg = if (isDir) FlBlue50 else FlSurface3
    val tileTint = if (isDir) FlBlue else FlText2
    val rowAlpha = if (item.isHidden) 0.55f else 1f
    val sizeUnits = LocalSizeUnits.current

    val a11y = buildA11yDescription(item, sizeUnits, riskSeverity, isSelected)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) FlBlue50 else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .alpha(rowAlpha)
            .semantics(mergeDescendants = true) {
                contentDescription = a11y
                role = Role.Button
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tileBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.fileCategory.name,
                    tint = tileTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall.copy(color = FlInk),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildSubtitle(item, sizeUnits)
                if (subtitle.isNotEmpty()) {
                    val subtitleStyle = if (isDir) {
                        MaterialTheme.typography.bodySmall.copy(color = FlMuted)
                    } else {
                        MonoSmall.copy(color = FlMuted)
                    }
                    Text(
                        text = subtitle,
                        style = subtitleStyle,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            if (riskSeverity != null) {
                Spacer(modifier = Modifier.width(8.dp))
                SeverityDot(severity = riskSeverity)
            }
            if (isDir) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FlMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (!isSelected) {
            HorizontalDivider(
                color = FlBorder,
                modifier = Modifier.padding(start = 68.dp, end = 12.dp)
            )
        }
    }
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
            .size(9.dp)
            .clip(RoundedCornerShape(50))
            .background(color)
            .semantics { contentDescription = label }
    )
}

private fun iconForCategory(category: FileCategory): ImageVector {
    return when (category) {
        FileCategory.DIRECTORY -> Icons.Filled.Folder
        FileCategory.IMAGE -> Icons.Filled.Image
        FileCategory.VIDEO -> Icons.Filled.VideoFile
        FileCategory.AUDIO -> Icons.Filled.AudioFile
        FileCategory.DOCUMENT -> Icons.Filled.Description
        FileCategory.SPREADSHEET -> Icons.Filled.TableChart
        FileCategory.PRESENTATION -> Icons.Filled.Slideshow
        FileCategory.ARCHIVE -> Icons.Filled.Archive
        FileCategory.CODE -> Icons.Filled.Code
        FileCategory.TEXT -> Icons.Filled.Description
        FileCategory.PDF -> Icons.Filled.PictureAsPdf
        FileCategory.APK -> Icons.Filled.Android
        FileCategory.DATABASE -> Icons.Filled.Storage
        FileCategory.FONT -> Icons.Filled.FontDownload
        FileCategory.EXECUTABLE -> Icons.Filled.Terminal
        FileCategory.UNKNOWN -> Icons.AutoMirrored.Filled.InsertDriveFile
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
