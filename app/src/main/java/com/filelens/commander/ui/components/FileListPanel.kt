package com.filelens.commander.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
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
    onFileClick: (FileItem) -> Unit,
    onNavigateUp: () -> Unit,
    onSortChanged: (SortOrder) -> Unit,
    onToggleHidden: () -> Unit,
    onSearchActiveChanged: (Boolean) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSearchRecursive: (String) -> Unit = {},
    onRefresh: () -> Unit = {},
    onBatchScan: () -> Unit = {},
    onFolderDashboard: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onBookmarkFolder: () -> Unit = {},
    onShowBookmarks: () -> Unit = {},
    onShowRecents: () -> Unit = {},
    onNavigateToPath: (String) -> Unit = {},
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
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

                IconButton(onClick = { showSortMenu = true }) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                SortDropdownMenu(
                    expanded = showSortMenu,
                    currentSort = sortOrder,
                    onDismiss = { showSortMenu = false },
                    onSortSelected = {
                        onSortChanged(it)
                        showSortMenu = false
                    }
                )

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
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Settings")
                            }
                        },
                        onClick = {
                            onOpenSettings()
                            showMenu = false
                        }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )

        Breadcrumb(
            currentPath = currentPath,
            isRoot = isRoot,
            onNavigateUp = onNavigateUp,
            onNavigateToPath = onNavigateToPath
        )

        AnimatedVisibility(visible = isSearchActive) {
            val focusRequester = remember { FocusRequester() }
            LaunchedEffect(isSearchActive) {
                if (isSearchActive) focusRequester.requestFocus()
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search files...") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchRecursive(searchQuery) }),
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
                        headline = "Empty folder",
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
                            FileListItem(
                                item = file,
                                isSelected = file.path == selectedFilePath,
                                onClick = { onFileClick(file) }
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
    isRoot: Boolean,
    onNavigateUp: () -> Unit,
    onNavigateToPath: (String) -> Unit
) {
    val parts = currentPath.split("/").filter { it.isNotEmpty() }
    if (parts.isEmpty()) return

    // Keep the breadcrumb to the last few segments so a deep path still
    // reads at a glance; the up-arrow handles further escaping.
    val maxShown = 4
    val visible = if (parts.size <= maxShown) parts else parts.takeLast(maxShown)
    val truncated = parts.size > visible.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRoot) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateUp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.NorthWest,
                    contentDescription = "Up",
                    tint = FlInk3,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
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
            val absoluteIndex = parts.size - visible.size + index
            val targetPath = "/" + parts.take(absoluteIndex + 1).joinToString("/")
            Text(
                text = segment,
                style = if (isLast) MaterialTheme.typography.bodySmall
                    else MaterialTheme.typography.bodySmall,
                color = if (isLast) FlInk2 else FlInk3,
                modifier = if (isLast) Modifier else Modifier.clickable {
                    onNavigateToPath(targetPath)
                }
            )
            if (!isLast) {
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
private fun SortDropdownMenu(
    expanded: Boolean,
    currentSort: SortOrder,
    onDismiss: () -> Unit,
    onSortSelected: (SortOrder) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        SortField.entries.forEach { field ->
            val label = when (field) {
                SortField.NAME -> "Name"
                SortField.SIZE -> "Size"
                SortField.DATE -> "Date"
                SortField.TYPE -> "Type"
            }
            val isActive = currentSort.field == field
            DropdownMenuItem(
                text = {
                    Text(
                        text = if (isActive) {
                            val arrow = if (currentSort.direction == SortDirection.ASCENDING) " ↑" else " ↓"
                            "$label$arrow"
                        } else label,
                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = {
                    val newDirection = if (isActive && currentSort.direction == SortDirection.ASCENDING) {
                        SortDirection.DESCENDING
                    } else {
                        SortDirection.ASCENDING
                    }
                    onSortSelected(currentSort.copy(field = field, direction = newDirection))
                }
            )
        }
    }
}
