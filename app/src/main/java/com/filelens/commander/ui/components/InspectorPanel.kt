package com.filelens.commander.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorInfo
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskCategory
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskLearnMore
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import com.filelens.commander.domain.model.TrustLevel
import com.filelens.commander.domain.model.TrustScore
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskInfo
import com.filelens.commander.ui.theme.RiskWarning

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
    result: InspectorResult? = null,
    isAnalyzing: Boolean = false,
    onOpenWith: () -> Unit,
    onViewOnMap: ((Double, Double) -> Unit)? = null,
    onShareCleaned: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    ignoredRiskIds: Set<String> = emptySet(),
    onIgnoreRisk: ((RiskItem) -> Unit)? = null,
    onRestoreIgnored: (() -> Unit)? = null,
    onOpenAs: ((FileCategory) -> Unit)? = null,
    onReinspect: (() -> Unit)? = null,
    forcedCategory: FileCategory? = null,
    detectedCategory: FileCategory? = null,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = info.category.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAnalyzing) {
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                    }
                    result?.trustScore?.let { score ->
                        Spacer(modifier = Modifier.width(8.dp))
                        TrustChip(score)
                    }
                    val visibleRisks = result?.risks?.filter { it.id.isEmpty() || it.id !in ignoredRiskIds } ?: emptyList()
                    val riskCount = visibleRisks.size
                    if (riskCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val hasWarning = visibleRisks.any { it.severity == RiskSeverity.WARNING || it.severity == RiskSeverity.DANGER }
                        Text(
                            text = "$riskCount risk${if (riskCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (hasWarning) RiskWarning else RiskInfo,
                            modifier = Modifier
                                .background(
                                    color = (if (hasWarning) RiskWarning else RiskInfo).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            if (onOpenAs != null) {
                OpenAsMenu(onOpenAs, onReinspect, forcedCategory, detectedCategory)
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
            InspectorTab.OVERVIEW -> OverviewTab(
                info = info,
                result = result,
                onOpenWith = onOpenWith,
                onViewOnMap = onViewOnMap,
                onShareCleaned = onShareCleaned,
                onSwitchTab = { tab -> selectedTab = tabs.indexOf(tab) }
            )
            InspectorTab.PREVIEW -> PreviewTab(result)
            InspectorTab.STRUCTURE -> StructureTab(result)
            InspectorTab.METADATA -> MetadataTab(result)
            InspectorTab.RISKS -> RisksTab(result, ignoredRiskIds, onIgnoreRisk, onRestoreIgnored)
        }
    }
}

@Composable
private fun OverviewTab(
    info: InspectorInfo,
    result: InspectorResult?,
    onOpenWith: () -> Unit,
    onViewOnMap: ((Double, Double) -> Unit)? = null,
    onShareCleaned: (() -> Unit)? = null,
    onSwitchTab: ((InspectorTab) -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        result?.trustScore?.let {
            TrustScoreCard(it)
            Spacer(modifier = Modifier.height(12.dp))
        }
        InfoRow("File", info.name)
        InfoRow("Type", info.category.displayName())
        if (info.extension.isNotEmpty()) {
            InfoRow("Extension", ".${info.extension}")
        }
        info.mimeType?.let { InfoRow("MIME", it) }
        info.size?.let { InfoRow("Size", formatFileSize(it)) }
        info.lastModified?.let { InfoRow("Modified", formatDate(it)) }

        result?.overview?.forEach { (key, value) ->
            InfoRow(key, value)
        }

        Spacer(modifier = Modifier.height(16.dp))

        val gpsLat = result?.metadata?.get("GPS latitude")?.toDoubleOrNull()
        val gpsLon = result?.metadata?.get("GPS longitude")?.toDoubleOrNull()
        if (gpsLat != null && gpsLon != null && onViewOnMap != null) {
            Button(
                onClick = { onViewOnMap(gpsLat, gpsLon) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("View on map")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = "Safe actions",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (onSwitchTab != null) {
            val hasPreview = result?.previewContent != null
            if (hasPreview) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { onSwitchTab(InspectorTab.PREVIEW) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Preview only")
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            val hasMetadata = (result?.metadata?.isNotEmpty() == true)
            if (hasMetadata) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { onSwitchTab(InspectorTab.METADATA) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Inspect metadata")
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            val hasStructure = result?.structure != null
            if (hasStructure) {
                androidx.compose.material3.OutlinedButton(
                    onClick = { onSwitchTab(InspectorTab.STRUCTURE) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (info.category) {
                            FileCategory.ARCHIVE, FileCategory.APK -> "Browse contents (no extract)"
                            FileCategory.DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> "Inspect document structure"
                            else -> "Inspect structure"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (onShareCleaned != null) {
            androidx.compose.material3.OutlinedButton(
                onClick = onShareCleaned,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Share cleaned copy")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "External",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Button(
            onClick = onOpenWith,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text("Open with...")
        }
    }
}

@Composable
private fun PreviewTab(result: InspectorResult?) {
    val previewContent = result?.previewContent
    if (previewContent == null) {
        CenteredMessage("No preview available")
        return
    }

    when (val preview = previewContent) {
        is PreviewContent.TextContent -> {
            var showRaw by remember { mutableStateOf(false) }
            val displayed = if (showRaw && preview.rawContent != null) preview.rawContent else preview.content
            val highlighted = remember(displayed, preview.language) {
                highlightSyntax(displayed.take(50_000), preview.language)
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (preview.language != null) {
                        Text(
                            text = preview.language,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (preview.rawContent != null) {
                        androidx.compose.material3.TextButton(onClick = { showRaw = !showRaw }) {
                            Text(if (showRaw) "Pretty" else "Raw", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                ) {
                    Text(
                        text = highlighted,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
        is PreviewContent.ImageContent -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AsyncImage(
                    model = preview.path,
                    contentDescription = "Image preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (preview.width != null && preview.height != null) {
                    Text(
                        text = "${preview.width} × ${preview.height}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        is PreviewContent.HexContent -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = "Hex view (${formatFileSize(preview.totalSize)} total)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = formatHex(preview.bytes),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        is PreviewContent.TreeContent -> {
            StructureTreeView(preview.root)
        }
        else -> {
            CenteredMessage("No preview available")
        }
    }
}

@Composable
private fun StructureTab(result: InspectorResult?) {
    if (result?.structure == null) {
        CenteredMessage("No structure available")
        return
    }

    StructureTreeView(result.structure)
}

@Composable
private fun StructureTreeView(root: StructureNode) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        StructureNodeItem(root, depth = 0)
    }
}

@Composable
private fun StructureNodeItem(node: StructureNode, depth: Int) {
    when (node) {
        is StructureNode.Leaf -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 16).dp, top = 2.dp, bottom = 2.dp)
            ) {
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (node.value != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.value,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        is StructureNode.Branch -> {
            var expanded by remember { mutableStateOf(depth < 2) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = (depth * 16).dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = node.name,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (node.details != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = node.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (expanded) {
                node.children.forEach { child ->
                    StructureNodeItem(child, depth + 1)
                }
            }
        }
    }
}

@Composable
private fun MetadataTab(result: InspectorResult?) {
    val metadata = result?.metadata
    if (metadata.isNullOrEmpty()) {
        CenteredMessage("No additional metadata")
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        metadata.forEach { (key, value) ->
            InfoRow(key, value)
        }
    }
}

private val openAsOptions = listOf(
    "Text" to FileCategory.TEXT,
    "Binary / Hex" to FileCategory.UNKNOWN,
    "Archive (ZIP)" to FileCategory.ARCHIVE,
    "SQLite" to FileCategory.DATABASE,
    "Image" to FileCategory.IMAGE,
    "APK" to FileCategory.APK,
    "Office (ZIP)" to FileCategory.DOCUMENT,
    "PDF" to FileCategory.PDF,
    "Code" to FileCategory.CODE,
    "Font" to FileCategory.FONT,
    "Audio / Video" to FileCategory.AUDIO
)

@Composable
private fun OpenAsMenu(
    onOpenAs: (FileCategory) -> Unit,
    onReinspect: (() -> Unit)?,
    forcedCategory: FileCategory?,
    detectedCategory: FileCategory?
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Inspector actions")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Text(
                text = "Open as…",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            openAsOptions.forEach { (label, cat) ->
                DropdownMenuItem(
                    text = {
                        val isCurrent = cat == forcedCategory
                        Text(
                            text = if (isCurrent) "$label  ✓" else label,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {
                        expanded = false
                        onOpenAs(cat)
                    }
                )
            }
            if (forcedCategory != null && detectedCategory != null) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Restore detected (${detectedCategory.displayName()})",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {
                        expanded = false
                        onOpenAs(detectedCategory)
                    }
                )
            }
            if (onReinspect != null) {
                DropdownMenuItem(
                    text = { Text(text = "Re-run inspector", style = MaterialTheme.typography.bodySmall) },
                    onClick = {
                        expanded = false
                        onReinspect()
                    }
                )
            }
        }
    }
}

private fun trustColor(level: TrustLevel): androidx.compose.ui.graphics.Color = when (level) {
    TrustLevel.TRUSTED -> androidx.compose.ui.graphics.Color(0xFF1E8E3E)
    TrustLevel.LOW_RISK -> RiskInfo
    TrustLevel.MEDIUM_RISK -> RiskWarning
    TrustLevel.HIGH_RISK -> RiskDanger
}

@Composable
private fun TrustChip(score: TrustScore) {
    val color = trustColor(score.level)
    Text(
        text = score.level.displayName,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun TrustScoreCard(score: TrustScore) {
    val color = trustColor(score.level)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = score.level.displayName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${score.numericScore}/100",
                style = MaterialTheme.typography.labelMedium,
                color = color
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Heuristic trust score. Not antivirus.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (score.topReasons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Top reasons",
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
            score.topReasons.forEach { reason ->
                Text(
                    text = "• $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun RisksTab(
    result: InspectorResult?,
    ignoredRiskIds: Set<String>,
    onIgnoreRisk: ((RiskItem) -> Unit)?,
    onRestoreIgnored: (() -> Unit)?
) {
    val allRisks = result?.risks.orEmpty()
    val visible = allRisks.filter { it.id.isEmpty() || it.id !in ignoredRiskIds }
    val hiddenCount = allRisks.size - visible.size

    if (allRisks.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No risks detected",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (visible.isEmpty()) {
            Text(
                "All risks ignored for this session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            visible.forEach { risk ->
                RiskCard(risk, onIgnoreRisk)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        if (hiddenCount > 0 && onRestoreIgnored != null) {
            Spacer(modifier = Modifier.height(4.dp))
            androidx.compose.material3.TextButton(onClick = onRestoreIgnored) {
                Text(
                    "$hiddenCount ignored — show",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun RiskCard(
    risk: RiskItem,
    onIgnoreRisk: ((RiskItem) -> Unit)? = null
) {
    val (color, icon) = when (risk.severity) {
        RiskSeverity.DANGER -> RiskDanger to Icons.Filled.Error
        RiskSeverity.WARNING -> RiskWarning to Icons.Filled.Warning
        RiskSeverity.INFO -> RiskInfo to Icons.Filled.Info
    }

    var showEvidence by remember { mutableStateOf(false) }
    var showLearnMore by remember { mutableStateOf(false) }
    val learnMore = RiskLearnMore[risk.learnMoreKey]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = color.copy(alpha = 0.08f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                icon,
                contentDescription = risk.severity.name,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = risk.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (risk.category != RiskCategory.OTHER) {
                        Text(
                            text = risk.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            modifier = Modifier
                                .background(
                                    color = color.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = risk.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (risk.whyItMatters != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Why it matters",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                    Text(
                        text = risk.whyItMatters,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (risk.recommendedAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recommended action",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                    Text(
                        text = risk.recommendedAction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (showEvidence && (risk.evidence.isNotEmpty() || risk.detector != null)) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp)
            ) {
                if (risk.detector != null) {
                    Text(
                        text = "Detector: ${risk.detector}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (risk.id.isNotEmpty()) {
                    Text(
                        text = "Rule: ${risk.id}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                risk.evidence.forEach { (key, value) ->
                    Text(
                        text = "$key: $value",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (showLearnMore && learnMore != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = learnMore,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (risk.evidence.isNotEmpty() || risk.detector != null) {
                androidx.compose.material3.TextButton(
                    onClick = { showEvidence = !showEvidence },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (showEvidence) "Hide evidence" else "Show evidence",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            if (learnMore != null) {
                androidx.compose.material3.TextButton(
                    onClick = { showLearnMore = !showLearnMore },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(
                        if (showLearnMore) "Hide learn more" else "Learn more",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (onIgnoreRisk != null && risk.id.isNotEmpty()) {
                androidx.compose.material3.TextButton(
                    onClick = { onIgnoreRisk(risk) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Ignore", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
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
            maxLines = 5,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CenteredMessage(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatHex(bytes: ByteArray): String {
    val sb = StringBuilder()
    for (i in bytes.indices step 16) {
        sb.append("%08X  ".format(i))
        for (j in 0 until 16) {
            if (i + j < bytes.size) {
                sb.append("%02X ".format(bytes[i + j]))
            } else {
                sb.append("   ")
            }
            if (j == 7) sb.append(" ")
        }
        sb.append(" |")
        for (j in 0 until 16) {
            if (i + j < bytes.size) {
                val b = bytes[i + j].toInt() and 0xFF
                sb.append(if (b in 0x20..0x7E) b.toChar() else '.')
            }
        }
        sb.append("|\n")
    }
    return sb.toString()
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
