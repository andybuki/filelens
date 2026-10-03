package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.com.filelens.R
import com.filelens.commander.domain.model.FrequentFolder
import com.filelens.commander.domain.model.RecentFile
import com.filelens.commander.ui.theme.CategoryDirectory
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.MonoSmall

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentsSheet(
    recentFiles: List<RecentFile>,
    frequentFolders: List<FrequentFolder>,
    onOpenFile: (RecentFile) -> Unit,
    onOpenFolder: (FrequentFolder) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            if (recentFiles.isEmpty() && frequentFolders.isEmpty()) {
                EmptyRecents()
            } else {
                if (frequentFolders.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.recents_section_frequent))
                    frequentFolders.take(10).forEach { folder ->
                        FrequentFolderRow(folder = folder, onOpen = { onOpenFolder(folder) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (recentFiles.isNotEmpty()) {
                    SectionHeader(stringResource(R.string.recents_section_recent))
                    recentFiles.take(10).forEach { file ->
                        RecentFileRow(file = file, onOpen = { onOpenFile(file) })
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    TextButton(onClick = onClearAll) {
                        Text(stringResource(R.string.recents_clear_history), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label.uppercase(),
        style = Eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun RecentFileRow(file: RecentFile, onOpen: () -> Unit) {
    BaseRow(
        title = file.displayName,
        subtitle = compactPath(file.path),
        icon = Icons.AutoMirrored.Filled.InsertDriveFile,
        iconTint = MaterialTheme.colorScheme.primary,
        iconBackground = MaterialTheme.colorScheme.primaryContainer,
        onClick = onOpen
    )
}

@Composable
private fun FrequentFolderRow(folder: FrequentFolder, onOpen: () -> Unit) {
    val visits = folder.visitCount
    val visitsLabel = pluralStringResource(R.plurals.plural_visit_count, visits, visits)
    BaseRow(
        title = folder.displayName,
        subtitle = "${compactPath(folder.path)} · $visitsLabel",
        icon = Icons.Filled.Folder,
        iconTint = CategoryDirectory,
        iconBackground = CategoryDirectory.copy(alpha = 0.14f),
        onClick = onOpen
    )
}

@Composable
private fun BaseRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MonoSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyRecents() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(stringResource(R.string.recents_empty_headline), style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.recents_empty_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun compactPath(path: String): String {
    val parts = path.split("/").filter { it.isNotEmpty() }
    if (parts.size <= 3) return path
    return ".../" + parts.takeLast(3).joinToString("/")
}
