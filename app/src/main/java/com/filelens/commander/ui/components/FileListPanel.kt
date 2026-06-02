package com.filelens.commander.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
    isActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentPath.substringAfterLast('/').ifEmpty { "/" },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = buildBreadcrumb(currentPath),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            },
            navigationIcon = {
                if (!isRoot || isSearchActive) {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            },
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
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = if (isActive) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
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

        if (isActive) {
            HorizontalDivider(color = MaterialTheme.colorScheme.primary, thickness = 2.dp)
        } else {
            HorizontalDivider()
        }

        when {
            errorMessage != null && files.isEmpty() -> {
                ErrorState(message = errorMessage, onRetry = onRefresh)
            }
            isLoading || isSearching -> {
                LoadingState(label = if (isSearching) "Searching…" else null)
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
                    }
                }
            }
        }
    }
}

private fun buildBreadcrumb(path: String): String {
    val parts = path.split("/").filter { it.isNotEmpty() }
    if (parts.size <= 3) return path
    return ".../" + parts.takeLast(3).joinToString("/")
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
