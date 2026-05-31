package com.filelens.commander.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorInfo

enum class InspectorTab(val label: String) {
    OVERVIEW("Overview"),
    PREVIEW("Preview"),
    STRUCTURE("Structure"),
    METADATA("Metadata"),
    RISKS("Risks")
}

@Composable
fun InspectorPanel(
    info: InspectorInfo?,
    onOpenWith: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (info == null) {
        EmptyInspector(modifier)
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = InspectorTab.entries.toList()

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info.category.displayName(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        when (tabs[selectedTab]) {
            InspectorTab.OVERVIEW -> OverviewTab(info, onOpenWith)
            InspectorTab.PREVIEW -> PlaceholderTab("Preview")
            InspectorTab.STRUCTURE -> PlaceholderTab("Structure")
            InspectorTab.METADATA -> MetadataTab(info)
            InspectorTab.RISKS -> PlaceholderTab("Risks")
        }
    }
}

@Composable
private fun OverviewTab(info: InspectorInfo, onOpenWith: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        InfoRow("File", info.name)
        InfoRow("Type", info.category.displayName())
        if (info.extension.isNotEmpty()) {
            InfoRow("Extension", ".${info.extension}")
        }
        info.mimeType?.let { InfoRow("MIME", it) }
        info.size?.let { InfoRow("Size", formatFileSize(it)) }
        info.lastModified?.let { InfoRow("Modified", formatDate(it)) }
        InfoRow("Path", info.path)
        InfoRow("Readable", if (info.isReadable) "Yes" else "No")

        info.details.forEach { (key, value) ->
            InfoRow(key, value)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onOpenWith,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Open with...")
        }
    }
}

@Composable
private fun MetadataTab(info: InspectorInfo) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (info.details.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No additional metadata",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            info.details.forEach { (key, value) ->
                InfoRow(key, value)
            }
        }
    }
}

@Composable
private fun PlaceholderTab(tabName: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Coming in Phase 2",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(0.65f),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmptyInspector(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Select a file to inspect",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap any file to see details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun FileCategory.displayName(): String = when (this) {
    FileCategory.DIRECTORY -> "Folder"
    FileCategory.IMAGE -> "Image"
    FileCategory.VIDEO -> "Video"
    FileCategory.AUDIO -> "Audio"
    FileCategory.DOCUMENT -> "Document"
    FileCategory.SPREADSHEET -> "Spreadsheet"
    FileCategory.PRESENTATION -> "Presentation"
    FileCategory.ARCHIVE -> "Archive"
    FileCategory.CODE -> "Source code"
    FileCategory.TEXT -> "Text file"
    FileCategory.PDF -> "PDF document"
    FileCategory.APK -> "Android package"
    FileCategory.DATABASE -> "Database"
    FileCategory.FONT -> "Font"
    FileCategory.EXECUTABLE -> "Executable"
    FileCategory.UNKNOWN -> "Unknown"
}
