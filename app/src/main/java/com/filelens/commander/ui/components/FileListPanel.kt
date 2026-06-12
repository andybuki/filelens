package com.filelens.commander.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.com.filelens.R
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import com.filelens.commander.domain.model.SortOrder
import com.filelens.commander.ui.theme.FlMuted
import com.filelens.commander.ui.theme.FlNavy
import com.filelens.commander.ui.theme.FlOnAccent
import com.filelens.commander.ui.theme.FlSurface
import com.filelens.commander.ui.theme.FlSurface3
import com.filelens.commander.ui.theme.FlText
import com.filelens.commander.ui.theme.FlText2

private val HeaderInk = FlOnAccent
private val HeaderInk2 = Color(0xFFE5E7EB)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileListPanel(
    currentPath: String,
    files: List<FileItem>,
    isLoading: Boolean,
    isRoot: Boolean,
    selectedFilePath: String?,
    sortOrder: SortOrder,
    showHidden: Boolean,
    errorMessage: String? = null,
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    isSearching: Boolean = false,
    homePath: String = "",
    selectedPaths: Set<String> = emptySet(),
    canCompareSelection: Boolean = false,
    canBatchRename: Boolean = false,
    canMoveToVault: Boolean = false,
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onCompareSelection: () -> Unit = {},
    onBatchRenameSelection: () -> Unit = {},
    onMoveToVaultSelection: () -> Unit = {},
    onNavigateUp: () -> Unit,
    onSortChanged: (SortOrder) -> Unit,
    onToggleHidden: () -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSearchRecursive: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onBatchScan: () -> Unit = {},
    onFolderDashboard: () -> Unit = {},
    onFindDuplicates: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenHome: () -> Unit = {},
    onBookmarkFolder: () -> Unit = {},
    onShowBookmarks: () -> Unit = {},
    onShowRecents: () -> Unit = {},
    onNavigateToPath: (String) -> Unit = {},
    isActive: Boolean = true,
    isRemote: Boolean = false,
    remoteServerLabel: String? = null,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(FlSurface)) {
        Column(modifier = Modifier.fillMaxSize()) {
            MinimalHeader(
                showHidden = showHidden,
                isSearchActive = isSearchActive,
                isRoot = isRoot,
                currentPath = currentPath,
                homePath = homePath,
                isRemote = isRemote,
                remoteServerLabel = remoteServerLabel,
                onToggleSearch = { onSearchActiveChanged(!isSearchActive) },
                onToggleHidden = onToggleHidden,
                onBatchScan = onBatchScan,
                onFolderDashboard = onFolderDashboard,
                onFindDuplicates = onFindDuplicates,
                onBookmarkFolder = onBookmarkFolder,
                onShowBookmarks = onShowBookmarks,
                onShowRecents = onShowRecents,
                onOpenSettings = onOpenSettings,
                onOpenHome = onOpenHome,
                onNavigateUp = onNavigateUp,
                onNavigateToPath = onNavigateToPath
            )

            SelectionBar(
                selectedCount = selectedPaths.size,
                canCompare = canCompareSelection,
                onClear = onClearSelection,
                onCompare = onCompareSelection,
                canBatchRename = canBatchRename,
                onBatchRename = onBatchRenameSelection,
                canMoveToVault = canMoveToVault,
                onMoveToVault = onMoveToVaultSelection
            )

            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                MinimalSearchBar(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChanged,
                    onDeepSearch = { onSearchRecursive(searchQuery) }
                )
            }

            SortTabs(
                sortOrder = sortOrder,
                onSortChanged = onSortChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Active-panel indicator (dual-pane only). Minimal design is single
            // pane, so on phone this stays a hairline divider.
            if (isActive) {
                HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 1.5.dp)
            } else {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }

            val listStateKey = when {
                errorMessage != null && files.isEmpty() -> "error"
                isSearching -> "searching"
                isLoading -> "loading"
                files.isEmpty() && isSearchActive && searchQuery.isNotEmpty() -> "no-matches"
                files.isEmpty() -> "empty"
                else -> "content"
            }
            // Crossfade between list states so loading→content and
            // empty→content don't snap. Default Crossfade duration is 300ms;
            // tighten to 180ms so the panel feels responsive but still soft.
            Crossfade(
                targetState = listStateKey,
                animationSpec = tween(durationMillis = 180),
                label = "file-list-state"
            ) { key ->
                when (key) {
                    "error" -> ErrorState(message = errorMessage ?: "", onRetry = onRefresh)
                    "searching" -> LoadingState(label = stringResource(R.string.searching_label))
                    "loading" -> FileListSkeleton()
                    "no-matches" -> NoResultsState(
                        query = searchQuery,
                        onClear = { onSearchQueryChanged("") }
                    )
                    "empty" -> EmptyState(
                        headline = stringResource(R.string.empty_folder_headline),
                        subtitle = stringResource(R.string.empty_folder_subtitle),
                        actionLabel = stringResource(R.string.empty_folder_action),
                        actionIcon = Icons.Filled.Upload,
                        onAction = onBatchScan
                    )
                    else -> PullToRefreshBox(
                        isRefreshing = false,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(files, key = { it.path }) { file ->
                                val multiSelected = file.path in selectedPaths
                                FileListItem(
                                    item = file,
                                    isSelected = multiSelected || file.path == selectedFilePath,
                                    onClick = { onFileClick(file) },
                                    onLongClick = { onFileLongClick(file) }
                                )
                            }
                            item("fab-spacer") {
                                Spacer(modifier = Modifier.height(96.dp))
                            }
                        }
                    }
                }
            }
        }

        // Circular blue FAB — primary import/scan action.
        if (files.isNotEmpty()) {
            FloatingActionButton(
                onClick = onBatchScan,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = FlOnAccent,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 22.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Filled.Upload, contentDescription = stringResource(R.string.menu_scan_folder))
            }
        }
    }
}

@Composable
private fun MinimalHeader(
    showHidden: Boolean,
    isSearchActive: Boolean,
    isRoot: Boolean,
    currentPath: String,
    homePath: String,
    isRemote: Boolean,
    remoteServerLabel: String?,
    onToggleSearch: () -> Unit,
    onToggleHidden: () -> Unit,
    onBatchScan: () -> Unit,
    onFolderDashboard: () -> Unit,
    onFindDuplicates: () -> Unit,
    onBookmarkFolder: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowRecents: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHome: () -> Unit,
    onNavigateUp: () -> Unit,
    onNavigateToPath: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FlNavy)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_brand_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.brand_name),
                    style = MaterialTheme.typography.titleLarge,
                    color = HeaderInk
                )
                if (isRemote && !remoteServerLabel.isNullOrBlank()) {
                    Text(
                        text = "Remote · $remoteServerLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }
            HeaderIconButton(onClick = onOpenHome) {
                Icon(
                    Icons.Filled.Home,
                    contentDescription = stringResource(R.string.cd_open_home),
                    tint = HeaderInk2
                )
            }
            HeaderIconButton(onClick = onToggleSearch) {
                Icon(
                    if (isSearchActive) Icons.Filled.Close else Icons.Filled.Search,
                    contentDescription = if (isSearchActive) stringResource(R.string.cd_close_search) else stringResource(R.string.cd_search),
                    tint = HeaderInk2
                )
            }
            HeaderIconButton(onClick = { showMenu = true }) {
                Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_more_options), tint = HeaderInk2)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (showHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (showHidden) stringResource(R.string.menu_hide_hidden_files) else stringResource(R.string.menu_show_hidden_files))
                        }
                    },
                    onClick = { onToggleHidden(); showMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Security, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.menu_scan_folder))
                        }
                    },
                    onClick = { onBatchScan(); showMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Analytics, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.menu_analyze_folder))
                        }
                    },
                    onClick = { onFolderDashboard(); showMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FileCopy, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.menu_find_duplicates))
                        }
                    },
                    onClick = { onFindDuplicates(); showMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.BookmarkAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.menu_bookmark_folder))
                        }
                    },
                    onClick = { onBookmarkFolder(); showMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Bookmarks, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.menu_show_bookmarks))
                        }
                    },
                    onClick = { onShowBookmarks(); showMenu = false }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.menu_recents))
                        }
                    },
                    onClick = { onShowRecents(); showMenu = false }
                )
            }
            HeaderIconButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Tune, contentDescription = stringResource(R.string.cd_settings), tint = HeaderInk2)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        MinimalBreadcrumb(
            currentPath = currentPath,
            homePath = homePath,
            isRoot = isRoot,
            isRemote = isRemote,
            onNavigateUp = onNavigateUp,
            onNavigateToPath = onNavigateToPath
        )
    }
}

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    // Outer 48dp box honours the Material touch-target minimum; the visible
    // 38dp pill is centered inside so the chrome doesn't change.
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun MinimalBreadcrumb(
    currentPath: String,
    homePath: String,
    isRoot: Boolean,
    isRemote: Boolean = false,
    onNavigateUp: () -> Unit,
    onNavigateToPath: (String) -> Unit
) {
    if (currentPath.isEmpty()) return
    val homeDisplayName = stringResource(R.string.home_folder_label)
    val navigateUpDesc = stringResource(R.string.cd_navigate_up)

    val relativeParts: List<String> = if (isRemote) {
        val stripped = currentPath.replace(Regex("^[a-zA-Z]+://[^/]*"), "")
        stripped.split("/").filter { it.isNotEmpty() }.ifEmpty { listOf("/") }
    } else when {
        homePath.isEmpty() -> currentPath.split("/").filter { it.isNotEmpty() }
        currentPath == homePath -> listOf(homeDisplayName)
        currentPath.startsWith("$homePath/") -> {
            listOf(homeDisplayName) + currentPath.removePrefix("$homePath/").split("/").filter { it.isNotEmpty() }
        }
        else -> currentPath.split("/").filter { it.isNotEmpty() }
    }

    val maxShown = 4
    val visible = if (relativeParts.size <= maxShown) relativeParts else relativeParts.takeLast(maxShown)
    val truncated = relativeParts.size > visible.size

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isRoot) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(onClick = onNavigateUp)
                    .semantics {
                        role = Role.Button
                        contentDescription = navigateUpDesc
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.NorthWest,
                    contentDescription = null,
                    tint = HeaderInk2,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (truncated) {
            Text("…", style = MaterialTheme.typography.bodySmall, color = FlMuted)
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = FlMuted,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        visible.forEachIndexed { index, segment ->
            val isLast = index == visible.lastIndex
            if (isLast) {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.bodySmall,
                    color = HeaderInk2
                )
            } else if (isRemote) {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.bodySmall,
                    color = FlMuted,
                    modifier = Modifier.clickable(onClick = onNavigateUp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FlMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            } else {
                val absoluteIndex = relativeParts.size - visible.size + index
                val targetPath = if (homePath.isNotEmpty() && relativeParts.firstOrNull() == homeDisplayName) {
                    val belowHome = relativeParts.subList(1, absoluteIndex + 1).joinToString("/")
                    if (belowHome.isEmpty()) homePath else "$homePath/$belowHome"
                } else {
                    "/" + relativeParts.take(absoluteIndex + 1).joinToString("/")
                }
                Text(
                    text = segment,
                    style = MaterialTheme.typography.bodySmall,
                    color = FlMuted,
                    modifier = Modifier.clickable { onNavigateToPath(targetPath) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FlMuted,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MinimalSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onDeepSearch: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(FlSurface3)
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = FlText2)
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.search_hint), color = FlText2) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDeepSearch() }),
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_search), tint = FlText2)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        TextButton(onClick = onDeepSearch, enabled = query.isNotEmpty()) {
            Text("Deep")
        }
    }
}

@Composable
private fun SortTabs(
    sortOrder: SortOrder,
    onSortChanged: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(FlSurface3)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortField.entries.forEach { field ->
            val label = when (field) {
                SortField.NAME -> stringResource(R.string.sort_name)
                SortField.SIZE -> stringResource(R.string.sort_size)
                SortField.DATE -> stringResource(R.string.sort_date)
                SortField.TYPE -> stringResource(R.string.sort_type)
            }
            val selected = sortOrder.field == field
            val directionLabel = if (sortOrder.direction == SortDirection.ASCENDING) "ascending" else "descending"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable {
                        val newDirection = if (selected && sortOrder.direction == SortDirection.ASCENDING) {
                            SortDirection.DESCENDING
                        } else {
                            SortDirection.ASCENDING
                        }
                        onSortChanged(sortOrder.copy(field = field, direction = newDirection))
                    }
                    .padding(horizontal = 10.dp, vertical = 7.dp)
                    .semantics(mergeDescendants = true) {
                        this.role = Role.Tab
                        this.selected = selected
                        contentDescription = if (selected) "Sort by $label, $directionLabel" else "Sort by $label"
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) FlOnAccent else FlText
                )
                if (selected) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.Filled.SwapVert,
                        contentDescription = if (sortOrder.direction == SortDirection.ASCENDING) "Ascending" else "Descending",
                        tint = FlOnAccent,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}
