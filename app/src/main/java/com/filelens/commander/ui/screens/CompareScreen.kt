package com.filelens.commander.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.com.filelens.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.domain.model.DiffKind
import com.filelens.commander.domain.model.FileCompareContent
import com.filelens.commander.domain.model.FileCompareResult
import com.filelens.commander.domain.model.FolderCompareResult
import com.filelens.commander.domain.model.FolderEntryDiff
import com.filelens.commander.domain.model.FolderEntryDiffKind
import com.filelens.commander.domain.model.MetadataDiffEntry
import com.filelens.commander.domain.model.StructuralDiffEntry
import com.filelens.commander.domain.model.TextDiffLine
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlInk2
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.ui.theme.RiskDanger
import com.filelens.commander.ui.theme.RiskDangerTint
import com.filelens.commander.ui.theme.RiskInfo
import com.filelens.commander.ui.theme.RiskInfoTint
import com.filelens.commander.ui.theme.RiskSafe
import com.filelens.commander.ui.theme.RiskSafeTint
import com.filelens.commander.ui.theme.RiskWarning
import com.filelens.commander.ui.theme.RiskWarningTint
import com.filelens.commander.ui.util.LocalSizeUnits
import com.filelens.commander.ui.util.formatFileSize
import com.filelens.commander.viewmodel.CompareViewModel
import com.filelens.commander.viewmodel.SyncDirection
import com.filelens.commander.ui.theme.Roboto
import com.filelens.commander.ui.theme.RobotoMono
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    leftPath: String,
    rightPath: String,
    onBack: () -> Unit,
    viewModel: CompareViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(leftPath, rightPath) {
        if (state.leftPath != leftPath || state.rightPath != rightPath) {
            viewModel.start(leftPath, rightPath)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.compare_title), style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                }
            },
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        PathHeader(leftPath = leftPath, rightPath = rightPath)

        when {
            state.errorMessage != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RiskDanger
                    )
                }
            }
            state.isComparing -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FlAccent, strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (state.isFolderMode) stringResource(R.string.compare_progress_folders) else stringResource(R.string.compare_progress_files),
                            style = MaterialTheme.typography.bodyMedium,
                            color = FlInk2
                        )
                    }
                }
            }
            state.fileResult != null -> FileCompareView(result = state.fileResult!!)
            state.folderResult != null -> FolderCompareView(
                result = state.folderResult!!,
                syncInProgress = state.syncInProgress,
                deepVerify = state.deepVerify,
                onDeepVerifyChanged = viewModel::setDeepVerify,
                onCopyMissing = viewModel::copyMissing,
                onOverwriteChanged = viewModel::overwriteChanged
            )
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun PathHeader(leftPath: String, rightPath: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.compare_panel_left),
                style = MaterialTheme.typography.labelMedium,
                color = FlInk3
            )
            Text(
                leftPath,
                style = MaterialTheme.typography.bodySmall,
                color = FlInk2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = FlInk3)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(R.string.compare_panel_right),
                style = MaterialTheme.typography.labelMedium,
                color = FlInk3
            )
            Text(
                rightPath,
                style = MaterialTheme.typography.bodySmall,
                color = FlInk2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private enum class FileTab { OVERVIEW, METADATA, CONTENT }

@Composable
private fun FileCompareView(result: FileCompareResult) {
    var tab by remember { mutableStateOf(FileTab.OVERVIEW) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            FileTab.entries.forEach { entry ->
                val selected = tab == entry
                val tabTitle = when (entry) {
                    FileTab.OVERVIEW -> stringResource(R.string.compare_tab_overview)
                    FileTab.METADATA -> stringResource(R.string.compare_tab_metadata)
                    FileTab.CONTENT -> stringResource(R.string.compare_tab_content)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (selected) FlAccent else Color.Transparent)
                        .clickable { tab = entry }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) FlOnAccent else FlInk2
                    )
                }
            }
        }

        when (tab) {
            FileTab.OVERVIEW -> OverviewSection(result)
            FileTab.METADATA -> MetadataSection(result.metadata)
            FileTab.CONTENT -> ContentSection(result.content)
        }
    }
}

@Composable
private fun OverviewSection(result: com.filelens.commander.domain.model.FileCompareResult) {
    val units = LocalSizeUnits.current
    val s = result.summary
    val df = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (s.identical) {
            Pill(text = stringResource(R.string.compare_identical), background = RiskSafeTint, foreground = RiskSafe)
        } else {
            Pill(text = stringResource(R.string.compare_different), background = RiskWarningTint, foreground = RiskWarning)
        }
        Spacer(modifier = Modifier.height(12.dp))

        // When both files are images, show them side by side so the metadata diff
        // below is anchored to what the user actually sees.
        if (isImage(s.leftName) && isImage(s.rightName)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = s.leftPath,
                    contentDescription = stringResource(R.string.cd_image_preview_left),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
                AsyncImage(
                    model = s.rightPath,
                    contentDescription = stringResource(R.string.cd_image_preview_right),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.FillWidth
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        CompareRow("Name", s.leftName, s.rightName)
        CompareRow("Size", formatFileSize(s.leftSize, units), formatFileSize(s.rightSize, units))
        CompareRow(
            "Modified",
            if (s.leftModified > 0) df.format(Date(s.leftModified)) else "—",
            if (s.rightModified > 0) df.format(Date(s.rightModified)) else "—"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(stringResource(R.string.compare_section_checksums), style = MaterialTheme.typography.labelMedium, color = FlInk3)
        Spacer(modifier = Modifier.height(4.dp))
        CompareRow(
            "SHA-256",
            s.leftChecksum.sha256 ?: "—",
            s.rightChecksum.sha256 ?: "—",
            mono = true
        )
        CompareRow(
            "MD5",
            s.leftChecksum.md5 ?: "—",
            s.rightChecksum.md5 ?: "—",
            mono = true
        )
    }
}

@Composable
private fun MetadataSection(entries: List<MetadataDiffEntry>) {
    val visible = entries.filter { it.kind != DiffKind.UNCHANGED }
    if (entries.isEmpty()) {
        EmptyMessage(stringResource(R.string.compare_metadata_empty))
        return
    }
    if (visible.isEmpty()) {
        EmptyMessage(stringResource(R.string.compare_metadata_identical))
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(visible, key = { it.key }) { entry ->
            CompareRow(
                label = entry.key,
                left = entry.leftValue ?: "—",
                right = entry.rightValue ?: "—",
                kind = entry.kind
            )
        }
    }
}

@Composable
private fun ContentSection(content: FileCompareContent) {
    when (content) {
        FileCompareContent.None -> EmptyMessage(stringResource(R.string.compare_content_empty))
        is FileCompareContent.Binary -> EmptyMessage(content.message)
        is FileCompareContent.Structural -> StructuralDiffList(content.diff.entries, format = content.diff.format)
        is FileCompareContent.Text -> TextDiffList(content.diff.lines, truncated = content.diff.truncated)
    }
}

@Composable
private fun StructuralDiffList(entries: List<StructuralDiffEntry>, format: String) {
    if (entries.isEmpty()) {
        EmptyMessage(stringResource(R.string.compare_structurally_identical, format))
        return
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(entries, key = { it.path + it.kind }) { entry ->
            CompareRow(
                label = entry.path,
                left = entry.leftValue ?: "—",
                right = entry.rightValue ?: "—",
                kind = entry.kind,
                mono = true
            )
        }
    }
}

@Composable
private fun TextDiffList(lines: List<TextDiffLine>, truncated: Boolean) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (truncated) {
            item {
                Pill(
                    text = stringResource(R.string.compare_diff_truncated),
                    background = RiskWarningTint,
                    foreground = RiskWarning
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        items(lines) { line ->
            DiffLineRow(line)
        }
    }
}

@Composable
private fun DiffLineRow(line: TextDiffLine) {
    val (bg, fg, prefix) = when (line.kind) {
        DiffKind.ADDED -> Triple(RiskSafeTint, RiskSafe, "+")
        DiffKind.REMOVED -> Triple(RiskDangerTint, RiskDanger, "−")
        DiffKind.UNCHANGED -> Triple(Color.Transparent, FlInk3, " ")
        DiffKind.CHANGED -> Triple(RiskWarningTint, RiskWarning, "~")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = (line.leftLineNumber?.toString() ?: "").padStart(4),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = RobotoMono,
            color = FlInk3,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = (line.rightLineNumber?.toString() ?: "").padStart(4),
            style = MaterialTheme.typography.labelSmall,
            fontFamily = RobotoMono,
            color = FlInk3,
            modifier = Modifier.width(36.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = prefix,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = RobotoMono,
            color = fg
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = line.content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = RobotoMono,
            fontSize = 12.sp,
            color = if (line.kind == DiffKind.UNCHANGED) FlInk2 else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompareRow(
    label: String,
    left: String,
    right: String,
    kind: DiffKind = if (left == right) DiffKind.UNCHANGED else DiffKind.CHANGED,
    mono: Boolean = false
) {
    val tint = when (kind) {
        DiffKind.ADDED -> RiskSafeTint
        DiffKind.REMOVED -> RiskDangerTint
        DiffKind.CHANGED -> RiskWarningTint
        DiffKind.UNCHANGED -> Color.Transparent
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(tint, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = FlInk3)
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                text = left,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (mono) RobotoMono else Roboto,
                color = FlInk2,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = right,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = if (mono) RobotoMono else Roboto,
                color = FlInk2,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private fun isImage(name: String): Boolean {
    val ext = name.substringAfterLast('.', "").lowercase()
    return ext in setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "heic", "heif")
}

@Composable
private fun Pill(text: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = foreground)
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = FlInk3)
    }
}

private enum class FolderFilter(val kind: FolderEntryDiffKind?) {
    ALL(null),
    LEFT_ONLY(FolderEntryDiffKind.LEFT_ONLY),
    RIGHT_ONLY(FolderEntryDiffKind.RIGHT_ONLY),
    CHANGED(FolderEntryDiffKind.BOTH_CHANGED),
    SAME(FolderEntryDiffKind.BOTH_SAME)
}

@Composable
private fun FolderSummaryBanner(
    counts: Map<FolderEntryDiffKind, Int>,
    deepVerify: Boolean
) {
    val same = counts[FolderEntryDiffKind.BOTH_SAME] ?: 0
    val changed = counts[FolderEntryDiffKind.BOTH_CHANGED] ?: 0
    val leftOnly = counts[FolderEntryDiffKind.LEFT_ONLY] ?: 0
    val rightOnly = counts[FolderEntryDiffKind.RIGHT_ONLY] ?: 0
    val total = same + changed + leftOnly + rightOnly
    val matchText = if (total > 0) stringResource(R.string.compare_folder_match_count, same, total) else stringResource(R.string.compare_folder_both_empty)
    val suffix = if (deepVerify) stringResource(R.string.compare_folder_checksum_verified) else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = matchText + suffix,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = stringResource(R.string.compare_folder_diff_summary, changed, leftOnly, rightOnly),
            style = MaterialTheme.typography.labelMedium,
            color = FlInk3
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderCompareView(
    result: FolderCompareResult,
    syncInProgress: Boolean,
    deepVerify: Boolean,
    onDeepVerifyChanged: (Boolean) -> Unit,
    onCopyMissing: (SyncDirection) -> Unit,
    onOverwriteChanged: (SyncDirection) -> Unit
) {
    var filter by remember { mutableStateOf(FolderFilter.ALL) }
    val counts = remember(result) {
        result.entries.groupingBy { it.kind }.eachCount()
    }
    val visible = remember(result, filter) {
        if (filter.kind == null) {
            result.entries.filter { it.kind != FolderEntryDiffKind.BOTH_SAME }
        } else {
            result.entries.filter { it.kind == filter.kind }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FolderSummaryBanner(counts = counts, deepVerify = deepVerify)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.compare_folder_verify_checksum),
                style = MaterialTheme.typography.labelMedium,
                color = FlInk2,
                modifier = Modifier.weight(1f)
            )
            androidx.compose.material3.Switch(
                checked = deepVerify,
                onCheckedChange = onDeepVerifyChanged,
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = FlOnAccent,
                    checkedTrackColor = FlAccent
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FolderFilter.entries.forEach { f ->
                val count = if (f == FolderFilter.ALL) {
                    result.entries.count { it.kind != FolderEntryDiffKind.BOTH_SAME }
                } else counts[f.kind] ?: 0
                val filterTitle = when (f) {
                    FolderFilter.ALL -> stringResource(R.string.compare_filter_all)
                    FolderFilter.LEFT_ONLY -> stringResource(R.string.compare_filter_left_only)
                    FolderFilter.RIGHT_ONLY -> stringResource(R.string.compare_filter_right_only)
                    FolderFilter.CHANGED -> stringResource(R.string.compare_filter_changed)
                    FolderFilter.SAME -> stringResource(R.string.compare_filter_same)
                }
                Spacer(modifier = Modifier.width(4.dp))
                FilterChip(
                    selected = filter == f,
                    onClick = { filter = f },
                    label = { Text("$filterTitle · $count") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FlAccentTint,
                        selectedLabelColor = FlAccent
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = { onCopyMissing(SyncDirection.LEFT_TO_RIGHT) },
                enabled = !syncInProgress
            ) { Text(stringResource(R.string.compare_folder_copy_to_right)) }
            TextButton(
                onClick = { onCopyMissing(SyncDirection.RIGHT_TO_LEFT) },
                enabled = !syncInProgress
            ) { Text(stringResource(R.string.compare_folder_copy_to_left)) }
            TextButton(
                onClick = { onOverwriteChanged(SyncDirection.LEFT_TO_RIGHT) },
                enabled = !syncInProgress
            ) { Text(stringResource(R.string.compare_folder_overwrite_right)) }
            TextButton(
                onClick = { onOverwriteChanged(SyncDirection.RIGHT_TO_LEFT) },
                enabled = !syncInProgress
            ) { Text(stringResource(R.string.compare_folder_overwrite_left)) }
        }

        if (result.renamedCandidates.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(
                    stringResource(R.string.compare_renamed_candidates, result.renamedCandidates.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = FlInk3
                )
                result.renamedCandidates.take(5).forEach { pair ->
                    Text(
                        "${pair.leftRelativePath}  ↔  ${pair.rightRelativePath}",
                        style = MaterialTheme.typography.bodySmall,
                        color = FlInk2,
                        fontFamily = RobotoMono,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (result.renamedCandidates.size > 5) {
                    Text(
                        stringResource(R.string.compare_and_more, result.renamedCandidates.size - 5),
                        style = MaterialTheme.typography.labelSmall,
                        color = FlInk3
                    )
                }
            }
        }

        if (visible.isEmpty()) {
            EmptyMessage(stringResource(R.string.compare_no_entries))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                items(visible, key = { it.relativePath + it.kind }) { entry ->
                    FolderEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun FolderEntryRow(entry: FolderEntryDiff) {
    val units = LocalSizeUnits.current
    val (tint, label) = when (entry.kind) {
        FolderEntryDiffKind.LEFT_ONLY -> RiskInfoTint to stringResource(R.string.compare_diff_label_left_only)
        FolderEntryDiffKind.RIGHT_ONLY -> RiskSafeTint to stringResource(R.string.compare_diff_label_right_only)
        FolderEntryDiffKind.BOTH_CHANGED -> RiskWarningTint to stringResource(R.string.compare_diff_label_changed)
        FolderEntryDiffKind.BOTH_SAME -> Color.Transparent to stringResource(R.string.compare_diff_label_same)
    }
    val fg = when (entry.kind) {
        FolderEntryDiffKind.LEFT_ONLY -> RiskInfo
        FolderEntryDiffKind.RIGHT_ONLY -> RiskSafe
        FolderEntryDiffKind.BOTH_CHANGED -> RiskWarning
        FolderEntryDiffKind.BOTH_SAME -> FlInk3
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(tint, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.relativePath.ifEmpty { "(root)" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = RobotoMono,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.isDirectory) {
                val leftSize = entry.leftSize?.let { formatFileSize(it, units) } ?: "—"
                val rightSize = entry.rightSize?.let { formatFileSize(it, units) } ?: "—"
                Text(
                    stringResource(R.string.compare_sizes_lr, leftSize, rightSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = FlInk3
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(tint)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
        }
    }
}
