package com.filelens.commander.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.com.filelens.R
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.ui.theme.CategoryApk
import com.filelens.commander.ui.theme.CategoryArchive
import com.filelens.commander.ui.theme.CategoryCertificate
import com.filelens.commander.ui.theme.CategoryAudio
import com.filelens.commander.ui.theme.CategoryCode
import com.filelens.commander.ui.theme.CategoryDirectory
import com.filelens.commander.ui.theme.CategoryDocument
import com.filelens.commander.ui.theme.CategoryImage
import com.filelens.commander.ui.theme.CategoryPdf
import com.filelens.commander.ui.theme.CategoryText
import com.filelens.commander.ui.theme.CategoryVideo
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlAccentTint
import com.filelens.commander.ui.theme.FlBg
import com.filelens.commander.ui.theme.FlInk
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.FlNavy
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.ui.theme.FlSurface
import com.filelens.commander.ui.theme.FlText
import com.filelens.commander.ui.theme.MonoSmall
import com.filelens.commander.ui.util.formatFileSize
import com.filelens.commander.viewmodel.GlobalSearchViewModel

private data class CategoryChip(
    val category: FileCategory,
    val labelRes: Int,
    val color: Color
)

private val CategoryChips = listOf(
    CategoryChip(FileCategory.IMAGE, R.string.home_category_images, CategoryImage),
    CategoryChip(FileCategory.VIDEO, R.string.home_category_video, CategoryVideo),
    CategoryChip(FileCategory.AUDIO, R.string.home_category_audio, CategoryAudio),
    CategoryChip(FileCategory.DOCUMENT, R.string.home_category_docs, CategoryDocument),
    CategoryChip(FileCategory.PDF, R.string.search_chip_pdf, CategoryPdf),
    CategoryChip(FileCategory.ARCHIVE, R.string.home_category_archives, CategoryArchive),
    CategoryChip(FileCategory.APK, R.string.home_category_apks, CategoryApk),
    CategoryChip(FileCategory.CODE, R.string.search_chip_code, CategoryCode),
    CategoryChip(FileCategory.TEXT, R.string.search_chip_text, CategoryText)
)

@Composable
fun GlobalSearchScreen(
    onBack: () -> Unit,
    onOpenFile: (FileItem) -> Unit,
    viewModel: GlobalSearchViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize().background(FlBg)) {
        SearchHeader(
            query = state.query,
            isSearching = state.isSearching,
            scannedFolders = state.scannedFolders,
            resultCount = state.results.size,
            focusRequester = focusRequester,
            onBack = onBack,
            onQueryChanged = viewModel::onQueryChanged,
            onSubmit = {
                keyboard?.hide()
                viewModel.runSearch()
            },
            onCancel = viewModel::cancelSearch,
            onClear = viewModel::clear
        )

        CategoryChipRow(
            selected = state.categoryFilter,
            onSelect = { viewModel.setCategoryFilter(it) }
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.results.isNotEmpty() -> ResultsList(
                    results = state.results,
                    onOpen = onOpenFile
                )
                state.isSearching -> SearchingPlaceholder(state.scannedFolders)
                state.didFinish && state.query.isNotBlank() ->
                    EmptyStateMessage(stringResource(R.string.search_no_matches))
                state.didCancel -> EmptyStateMessage(stringResource(R.string.search_cancelled))
                else -> IdleHint()
            }
        }
    }
}

@Composable
private fun SearchHeader(
    query: String,
    isSearching: Boolean,
    scannedFolders: Int,
    resultCount: Int,
    focusRequester: FocusRequester,
    onBack: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlNavy)
            .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = FlOnAccent
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x33000000))
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = FlOnAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = query,
                        onValueChange = onQueryChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = FlOnAccent),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(FlOnAccent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        decorationBox = { inner ->
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(R.string.search_hint_global),
                                    color = Color(0xFFCBD0D8),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            inner()
                        }
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.action_clear),
                            tint = FlOnAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (isSearching) {
                IconButton(onClick = onCancel) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.search_cancel),
                        tint = FlOnAccent
                    )
                }
            }
        }
        if (isSearching || resultCount > 0) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.6.dp,
                        color = FlOnAccent
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(
                        R.string.search_progress_status,
                        resultCount,
                        scannedFolders
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFE5E7EB)
                )
            }
        }
    }
}

@Composable
private fun CategoryChipRow(
    selected: FileCategory?,
    onSelect: (FileCategory?) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(FlSurface),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.search_chip_all)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FlAccent,
                    selectedLabelColor = FlOnAccent
                )
            )
        }
        items(CategoryChips) { chip ->
            FilterChip(
                selected = selected == chip.category,
                onClick = { onSelect(if (selected == chip.category) null else chip.category) },
                label = { Text(stringResource(chip.labelRes)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = chip.color,
                    selectedLabelColor = FlOnAccent
                )
            )
        }
    }
}

@Composable
private fun ResultsList(
    results: List<FileItem>,
    onOpen: (FileItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(results, key = { it.path }) { item ->
            ResultRow(item = item, onClick = { onOpen(item) })
        }
    }
}

@Composable
private fun ResultRow(item: FileItem, onClick: () -> Unit) {
    val isDir = item.nodeType == com.filelens.commander.vfs.NodeType.DIRECTORY
    val tint = colorFor(item.fileCategory)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FlSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDir) Icons.Filled.Folder
                    else Icons.AutoMirrored.Filled.InsertDriveFile,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.titleSmall,
                color = FlInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val sub = buildString {
                val parent = item.path.substringBeforeLast('/', "")
                if (parent.isNotEmpty()) append(compactPath(parent))
                val size = item.size
                if (size != null && size > 0) {
                    if (isNotEmpty()) append(" · ")
                    append(formatFileSize(size))
                }
            }
            Text(
                text = sub,
                style = MonoSmall,
                color = FlText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun SearchingPlaceholder(scannedFolders: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = FlAccent)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                stringResource(R.string.search_scanning_label, scannedFolders),
                style = MaterialTheme.typography.bodyMedium,
                color = FlInk3
            )
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = FlInk3,
            modifier = Modifier.padding(24.dp)
        )
    }
}

@Composable
private fun IdleHint() {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(FlAccentTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = FlAccent,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                stringResource(R.string.search_idle_headline),
                style = MaterialTheme.typography.titleMedium,
                color = FlInk
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                stringResource(R.string.search_idle_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = FlInk3
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                stringResource(R.string.search_section_filter).uppercase(),
                style = Eyebrow,
                color = FlInk3
            )
        }
    }
}

private fun colorFor(category: FileCategory): Color = when (category) {
    FileCategory.DIRECTORY -> CategoryDirectory
    FileCategory.IMAGE -> CategoryImage
    FileCategory.VIDEO -> CategoryVideo
    FileCategory.AUDIO -> CategoryAudio
    FileCategory.DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> CategoryDocument
    FileCategory.PDF -> CategoryPdf
    FileCategory.ARCHIVE -> CategoryArchive
    FileCategory.APK -> CategoryApk
    FileCategory.CODE -> CategoryCode
    FileCategory.TEXT -> CategoryText
    FileCategory.CERTIFICATE -> CategoryCertificate
    else -> FlAccent
}

private fun compactPath(path: String): String {
    val parts = path.split("/").filter { it.isNotEmpty() }
    if (parts.size <= 3) return path
    return ".../" + parts.takeLast(3).joinToString("/")
}
