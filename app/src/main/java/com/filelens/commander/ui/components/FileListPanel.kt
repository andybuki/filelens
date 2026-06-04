package com.filelens.commander.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import com.filelens.commander.ui.theme.Eyebrow
import com.filelens.commander.ui.theme.FlAccent
import com.filelens.commander.ui.theme.FlInk2
import com.filelens.commander.ui.theme.FlInk3
import com.filelens.commander.ui.theme.FlOnAccent
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import com.filelens.commander.domain.model.SortOrder

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
    onFileClick: (FileItem) -> Unit,
    onFileLongClick: (FileItem) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onCompareSelection: () -> Unit = {},
    onBatchRenameSelection: () -> Unit = {},
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
    onBookmarkFolder: () -> Unit = {},
    onShowBookmarks: () -> Unit = {},
    onShowRecents: () -> Unit = {},
    onNavigateToPath: (String) -> Unit = {},
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(FlAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.TravelExplore,
                            contentDescription = null,
                            tint = FlOnAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(11.dp))
                    Text(
                        text = "FileLens",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            navigationIcon = {},
            actions = {
                IconButton(onClick = { onSearchActiveChanged(!isSearchActive) }) {
                    Icon(Icons.Filled.Search, contentDescription = "Search")
                }

                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options")
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
                                Text(if (showHidden) "Hide hidden files" else "Show hidden files")
                            }
                        },
                        onClick = {
                            onToggleHidden()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan folder for risks")
                            }
                        },
                        onClick = {
                            onBatchScan()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze folder")
                            }
                        },
                        onClick = {
                            onFolderDashboard()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Find duplicates here")
                            }
                        },
                        onClick = {
                            onFindDuplicates()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.BookmarkAdd, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Bookmark this folder")
                            }
                        },
                        onClick = {
                            onBookmarkFolder()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Bookmarks, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Show bookmarks")
                            }
                        },
                        onClick = {
                            onShowBookmarks()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.History, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Recent")
                            }
                        },
                        onClick = {
                            onShowRecents()
                            showMenu = false
                        }
                    )
                }

                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Tune, contentDescription = "Settings")
                }
            },
            // Scaffold above us already applies the status-bar inset, so disable
            // the TopAppBar's own to avoid a big empty band above the logo.
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        SelectionBar(
            selectedCount = selectedPaths.size,
            canCompare = canCompareSelection,
            onClear = onClearSelection,
            onCompare = onCompareSelection,
            canBatchRename = canBatchRename,
            onBatchRename = onBatchRenameSelection
        )

        // Single small-caps current folder name, à la "DCIM" in the design.
        // Subfolders below the home root render the relative crumb trail.
        Breadcrumb(
            currentPath = currentPath,
            homePath = homePath,
            isRoot = isRoot,
            onNavigateUp = onNavigateUp,
            onNavigateToPath = onNavigateToPath
        )

        // Search slides down with a spring; matches the snappier feel of the design.
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
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(isSearchActive) {
                if (isSearchActive) focusRequester.requestFocus()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search files...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchRecursive(searchQuery) }),
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = FlInk3)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(),
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { onSearchRecursive(searchQuery) }, enabled = searchQuery.isNotEmpty()) {
                    Text("Deep")
                }
            }
        }

        SortTabs(
            sortOrder = sortOrder,
            onSortChanged = onSortChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )

        // Tablet dual-pane needs a per-panel "active" indicator so it's clear
        // which side keyboard shortcuts and the inspector act on. On phone the
        // panel is the only thing on screen, so the colored bar would just be
        // visual noise — keep a hairline divider there instead.
        if (isActive) {
            HorizontalDivider(color = FlAccent, thickness = 1.5.dp)
        } else {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }

        when {
            errorMessage != null && files.isEmpty() -> {
                ErrorState(message = errorMessage, onRetry = onRefresh)
            }
            isSearching -> {
                LoadingState(label = "Searching…")
            }
            isLoading -> {
                FileListSkeleton()
            }
            files.isEmpty() -> {
                if (isSearchActive && searchQuery.isNotEmpty()) {
                    NoResultsState(
                        query = searchQuery,
                        onClear = { onSearchQueryChanged("") }
                    )
                } else {
                    EmptyState(
                        headline = "Folder is empty",
                        subtitle = "Nothing to inspect here."
                    )
                }
            }
            else -> {
                PullToRefreshBox(
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

    // Primary action FAB: scans the current folder for risks. Per the design
    // system this is the one violet square-rounded FAB, not Material's
    // default circle.
    FloatingActionButton(
        onClick = onBatchScan,
        containerColor = FlAccent,
        contentColor = FlOnAccent,
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 18.dp, bottom = 22.dp)
            .size(54.dp)
    ) {
        Icon(Icons.Filled.Upload, contentDescription = "Scan folder")
    }
    }
}

@Composable
private fun Breadcrumb(
    currentPath: String,
    homePath: String,
    isRoot: Boolean,
    onNavigateUp: () -> Unit,
    onNavigateToPath: (String) -> Unit
) {
    if (currentPath.isEmpty()) return

    // The shared/emulated storage root literally lives at ".../0" — surface
    // it as "Phone" so the label means something to the user.
    val homeDisplayName = "Phone"

    // Render the path relative to the home root so the user can't navigate
    // above their starting folder. At home, that's a single label like "Phone".
    val relativeParts: List<String> = when {
        homePath.isEmpty() -> currentPath.split("/").filter { it.isNotEmpty() }
        currentPath == homePath -> listOf(homeDisplayName)
        currentPath.startsWith("$homePath/") -> {
            listOf(homeDisplayName) + currentPath.removePrefix("$homePath/").split("/").filter { it.isNotEmpty() }
        }
        else -> currentPath.split("/").filter { it.isNotEmpty() }
    }

    // Keep the trail short on deep paths.
    val maxShown = 4
    val visible = if (relativeParts.size <= maxShown) relativeParts else relativeParts.takeLast(maxShown)
    val truncated = relativeParts.size > visible.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 6.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRoot) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateUp)
                    .semantics {
                        role = androidx.compose.ui.semantics.Role.Button
                        contentDescription = "Navigate up"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.NorthWest,
                    contentDescription = null,
                    tint = FlInk3,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
        if (truncated) {
            Text(
                text = "…",
                style = MaterialTheme.typography.bodySmall,
                color = FlInk3
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = FlInk3,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        visible.forEachIndexed { index, segment ->
            val isLast = index == visible.lastIndex
            // Resolve the absolute path for this crumb so navigating to a
            // mid-trail segment stays anchored under homePath.
            val absoluteIndex = relativeParts.size - visible.size + index
            val targetPath = if (homePath.isNotEmpty() && relativeParts.firstOrNull() == homeDisplayName) {
                val belowHome = relativeParts.subList(1, absoluteIndex + 1).joinToString("/")
                if (belowHome.isEmpty()) homePath else "$homePath/$belowHome"
            } else {
                "/" + relativeParts.take(absoluteIndex + 1).joinToString("/")
            }
            if (isLast) {
                Text(
                    text = segment.uppercase(),
                    style = Eyebrow,
                    color = FlInk2
                )
            } else {
                Text(
                    text = segment,
                    style = MaterialTheme.typography.bodySmall,
                    color = FlInk3,
                    modifier = Modifier.clickable { onNavigateToPath(targetPath) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = FlInk3,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SortField.entries.forEach { field ->
            val label = when (field) {
                SortField.NAME -> "Name"
                SortField.SIZE -> "Size"
                SortField.DATE -> "Date"
                SortField.TYPE -> "Type"
            }
            val selected = sortOrder.field == field
            val directionLabel = if (sortOrder.direction == SortDirection.ASCENDING) "ascending" else "descending"
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) FlAccent else Color.Transparent)
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
                        this.role = androidx.compose.ui.semantics.Role.Tab
                        this.selected = selected
                        contentDescription = if (selected) "Sort by $label, $directionLabel" else "Sort by $label"
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) FlOnAccent else FlInk2
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
