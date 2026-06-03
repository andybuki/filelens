package com.filelens.commander.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.ui.components.BatchScanDialog
import com.filelens.commander.ui.components.FolderDashboardDialog
import com.filelens.commander.ui.components.CommanderToolbar
import com.filelens.commander.ui.components.CopyMoveDialog
import com.filelens.commander.ui.components.DeleteConfirmDialog
import com.filelens.commander.ui.components.FileListPanel
import com.filelens.commander.ui.components.InspectorPanel
import com.filelens.commander.ui.components.NewFolderDialog
import com.filelens.commander.ui.components.RenameDialog
import com.filelens.commander.ui.components.SearchInsideDialog
import com.filelens.commander.ui.components.SelectionBar
import com.filelens.commander.viewmodel.BatchScanViewModel
import com.filelens.commander.viewmodel.FolderDashboardViewModel
import com.filelens.commander.viewmodel.CommanderViewModel
import com.filelens.commander.viewmodel.DialogType
import com.filelens.commander.viewmodel.FileListState
import com.filelens.commander.viewmodel.FileListViewModel
import com.filelens.commander.viewmodel.InspectorState
import com.filelens.commander.viewmodel.InspectorViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
    onOpenSettings: () -> Unit = {},
    onCompare: (String, String) -> Unit = { _, _ -> },
    fileListViewModel: FileListViewModel = hiltViewModel(),
    inspectorViewModel: InspectorViewModel = hiltViewModel(),
    commanderViewModel: CommanderViewModel = hiltViewModel(),
    batchScanViewModel: BatchScanViewModel = hiltViewModel(),
    folderDashboardViewModel: FolderDashboardViewModel = hiltViewModel(),
    bookmarksViewModel: com.filelens.commander.viewmodel.BookmarksViewModel = hiltViewModel(),
    recentsViewModel: com.filelens.commander.viewmodel.RecentsViewModel = hiltViewModel()
) {
    val fileListState by fileListViewModel.state.collectAsStateWithLifecycle()
    val inspectorState by inspectorViewModel.state.collectAsStateWithLifecycle()
    val commanderState by commanderViewModel.state.collectAsStateWithLifecycle()
    val batchScanState by batchScanViewModel.state.collectAsStateWithLifecycle()
    val folderDashboardState by folderDashboardViewModel.state.collectAsStateWithLifecycle()
    val bookmarks by bookmarksViewModel.bookmarks.collectAsStateWithLifecycle()
    val recentFiles by recentsViewModel.recentFiles.collectAsStateWithLifecycle()
    val frequentFolders by recentsViewModel.frequentFolders.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val mainScope = rememberCoroutineScope()
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showRecentsSheet by remember { mutableStateOf(false) }

    val bookmarkCurrentFolder: () -> Unit = {
        val path = fileListState.currentPath
        if (path.isNotEmpty()) {
            bookmarksViewModel.add(
                path = path,
                displayName = path.substringAfterLast('/').ifEmpty { "/" }
            )
            mainScope.launch { snackbarHostState.showSnackbar("Bookmark added") }
        }
    }

    if (showBookmarksSheet) {
        com.filelens.commander.ui.components.BookmarksSheet(
            bookmarks = bookmarks,
            onOpen = { bookmark ->
                showBookmarksSheet = false
                fileListViewModel.navigateTo(bookmark.path)
            },
            onRemove = { bookmarksViewModel.remove(it.path) },
            onDismiss = { showBookmarksSheet = false }
        )
    }

    if (showRecentsSheet) {
        com.filelens.commander.ui.components.RecentsSheet(
            recentFiles = recentFiles,
            frequentFolders = frequentFolders,
            onOpenFile = { recent ->
                showRecentsSheet = false
                val parent = recent.path.substringBeforeLast('/', "")
                if (parent.isNotEmpty()) fileListViewModel.navigateTo(parent)
                val item = com.filelens.commander.domain.model.FileItem(
                    name = recent.displayName,
                    path = recent.path,
                    nodeType = com.filelens.commander.vfs.NodeType.FILE,
                    size = null,
                    lastModified = null,
                    isReadable = true,
                    fileCategory = com.filelens.commander.domain.model.classifyByExtension(
                        recent.displayName.substringAfterLast('.', "")
                    ),
                    extension = recent.displayName.substringAfterLast('.', "")
                )
                inspectorViewModel.selectFile(item)
            },
            onOpenFolder = { folder ->
                showRecentsSheet = false
                fileListViewModel.navigateTo(folder.path)
            },
            onClearAll = { recentsViewModel.clear() },
            onDismiss = { showRecentsSheet = false }
        )
    }

    if (batchScanState.active) {
        BatchScanDialog(
            state = batchScanState,
            onCleanAndShareRisky = { batchScanViewModel.cleanAndShareRisky() },
            onShareCleanedZip = { zipPath ->
                val zipFile = File(zipPath)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share cleaned files"))
            },
            onClearCleanedZip = { batchScanViewModel.clearCleanedZip() },
            onDismiss = { batchScanViewModel.dismiss() }
        )
    }

    if (folderDashboardState.active) {
        FolderDashboardDialog(
            state = folderDashboardState,
            onDismiss = { folderDashboardViewModel.dismiss() }
        )
    }

    var showSearchInsideDialog by remember { mutableStateOf(false) }
    if (showSearchInsideDialog) {
        SearchInsideDialog(
            initialQuery = inspectorState.searchQuery,
            hits = inspectorState.searchHits,
            isSearching = inspectorState.isSearching,
            onSearch = { q -> inspectorViewModel.searchInside(q) },
            onDismiss = {
                showSearchInsideDialog = false
                inspectorViewModel.clearSearch()
            }
        )
    }

    LaunchedEffect(Unit) {
        commanderViewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val activity = context as? android.app.Activity
    val windowSizeClass = activity?.let { calculateWindowSizeClass(it) }
    val isExpandedScreen = windowSizeClass?.widthSizeClass == WindowWidthSizeClass.Expanded

    val onOpenWith: () -> Unit = {
        inspectorState.info?.let { info ->
            val file = File(info.path)
            if (file.exists()) {
                try {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, info.mimeType ?: "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                } catch (_: Exception) {
                }
            }
        }
    }

    val onViewOnMap: (Double, Double) -> Unit = { lat, lon ->
        try {
            val geoUri = Uri.parse("geo:$lat,$lon?q=$lat,$lon")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            context.startActivity(mapIntent)
        } catch (_: Exception) {
        }
    }

    val refreshCurrentDir = { fileListViewModel.refresh() }
    val onBatchScan = { batchScanViewModel.startScan(fileListState.currentPath) }
    val onFolderDashboard = { folderDashboardViewModel.analyze(fileListState.currentPath) }

    val coroutineScope = rememberCoroutineScope()
    val onShareCleaned: () -> Unit = {
        val path = inspectorState.info?.path
        if (path != null) {
            coroutineScope.launch {
                commanderViewModel.createCleanedCopyAndShare(path) { cleanedPath ->
                    val file = File(cleanedPath)
                    try {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val mime = android.webkit.MimeTypeMap.getSingleton()
                            .getMimeTypeFromExtension(file.extension) ?: "*/*"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = mime
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share cleaned copy"))
                    } catch (_: Exception) {}
                }
            }
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val selectedNames = fileListState.files
        .filter { it.path in commanderState.selectedPaths }
        .map { it.name }

    when (commanderState.activeDialog) {
        DialogType.NEW_FOLDER -> NewFolderDialog(
            onConfirm = { name -> commanderViewModel.createFolder(fileListState.currentPath, name, refreshCurrentDir) },
            onDismiss = { commanderViewModel.dismissDialog() }
        )
        DialogType.RENAME -> {
            val selected = commanderState.selectedPaths.firstOrNull()
            val currentName = fileListState.files.find { it.path == selected }?.name ?: ""
            if (selected != null) {
                RenameDialog(
                    currentName = currentName,
                    onConfirm = { newName -> commanderViewModel.rename(selected, newName, refreshCurrentDir) },
                    onDismiss = { commanderViewModel.dismissDialog() }
                )
            }
        }
        DialogType.DELETE -> DeleteConfirmDialog(
            fileNames = selectedNames,
            onConfirm = { commanderViewModel.deleteSelected(refreshCurrentDir) },
            onDismiss = { commanderViewModel.dismissDialog() }
        )
        DialogType.COPY, DialogType.MOVE -> {
            val op = if (commanderState.activeDialog == DialogType.COPY) "Copy" else "Move"
            CopyMoveDialog(
                operation = op,
                fileNames = selectedNames,
                targetPath = fileListState.currentPath,
                onConfirm = {
                    if (commanderState.activeDialog == DialogType.COPY) {
                        commanderViewModel.copySelected(fileListState.currentPath, refreshCurrentDir)
                    } else {
                        commanderViewModel.moveSelected(fileListState.currentPath, refreshCurrentDir)
                    }
                },
                onDismiss = { commanderViewModel.dismissDialog() }
            )
        }
        DialogType.NONE -> {}
    }

    BackHandler(enabled = !fileListState.isRoot || commanderState.selectedPaths.isNotEmpty() || fileListState.isSearchActive) {
        if (commanderState.selectedPaths.isNotEmpty()) {
            commanderViewModel.clearSelection()
        } else {
            fileListViewModel.navigateUp()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.F3 -> {
                        val first = commanderState.selectedPaths.firstOrNull()
                        val item = fileListState.files.find { it.path == first }
                        if (item != null) inspectorViewModel.selectFile(item)
                        true
                    }
                    event.key == Key.F5 -> { commanderViewModel.showDialog(DialogType.COPY); true }
                    event.key == Key.F6 -> { commanderViewModel.showDialog(DialogType.MOVE); true }
                    event.key == Key.F7 -> { commanderViewModel.showDialog(DialogType.NEW_FOLDER); true }
                    event.key == Key.F8 || event.key == Key.Delete -> {
                        if (commanderState.selectedPaths.isNotEmpty()) {
                            commanderViewModel.showDialog(DialogType.DELETE)
                        }
                        true
                    }
                    event.key == Key.F2 -> { commanderViewModel.showDialog(DialogType.RENAME); true }
                    event.key == Key.Backspace -> { fileListViewModel.navigateUp(); true }
                    event.isCtrlPressed && event.key == Key.A -> {
                        commanderViewModel.selectAll(fileListState.files)
                        true
                    }
                    event.isCtrlPressed && event.key == Key.R -> { refreshCurrentDir(); true }
                    event.isCtrlPressed && event.key == Key.Z -> {
                        commanderViewModel.restoreLast()
                        refreshCurrentDir()
                        true
                    }
                    event.key == Key.Tab -> { true }
                    else -> false
                }
            }
    ) {
        if (isExpandedScreen) {
            ExpandedLayout(
                fileListState = fileListState,
                inspectorState = inspectorState,
                commanderState = commanderState,
                fileListViewModel = fileListViewModel,
                inspectorViewModel = inspectorViewModel,
                commanderViewModel = commanderViewModel,
                onOpenWith = onOpenWith,
                onViewOnMap = onViewOnMap,
                onShareCleaned = onShareCleaned,
                onBatchScan = onBatchScan,
                onFolderDashboard = onFolderDashboard,
                onOpenSettings = onOpenSettings,
                onBookmarkFolder = bookmarkCurrentFolder,
                onShowBookmarks = { showBookmarksSheet = true },
                onShowRecents = { showRecentsSheet = true },
                onRefresh = refreshCurrentDir,
                onSearchInside = { showSearchInsideDialog = true },
                onExportReport = { isJson -> exportAndShareReport(isJson, inspectorViewModel, context) },
                onCompare = onCompare
            )
        } else {
            CompactLayout(
                fileListState = fileListState,
                inspectorState = inspectorState,
                commanderState = commanderState,
                fileListViewModel = fileListViewModel,
                inspectorViewModel = inspectorViewModel,
                commanderViewModel = commanderViewModel,
                onOpenWith = onOpenWith,
                onViewOnMap = onViewOnMap,
                onShareCleaned = onShareCleaned,
                onBatchScan = onBatchScan,
                onFolderDashboard = onFolderDashboard,
                onOpenSettings = onOpenSettings,
                onBookmarkFolder = bookmarkCurrentFolder,
                onShowBookmarks = { showBookmarksSheet = true },
                onShowRecents = { showRecentsSheet = true },
                onSearchInside = { showSearchInsideDialog = true },
                onExportReport = { isJson -> exportAndShareReport(isJson, inspectorViewModel, context) },
                onCompare = onCompare,
                onRefresh = refreshCurrentDir
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactLayout(
    fileListState: FileListState,
    inspectorState: InspectorState,
    commanderState: com.filelens.commander.viewmodel.CommanderState,
    fileListViewModel: FileListViewModel,
    inspectorViewModel: InspectorViewModel,
    commanderViewModel: CommanderViewModel,
    onOpenWith: () -> Unit,
    onViewOnMap: (Double, Double) -> Unit,
    onShareCleaned: () -> Unit,
    onBatchScan: () -> Unit,
    onFolderDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onBookmarkFolder: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowRecents: () -> Unit,
    onSearchInside: () -> Unit,
    onExportReport: (Boolean) -> Unit,
    onCompare: (String, String) -> Unit,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

    val selectedItems = fileListState.files.filter { it.path in commanderState.selectedPaths }
    val canCompare = selectedItems.size == 2 && selectedItems.map { it.nodeType }.toSet().size == 1
    val triggerCompare = {
        if (selectedItems.size == 2) {
            onCompare(selectedItems[0].path, selectedItems[1].path)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = 0.dp,
            sheetContent = {
                InspectorPanel(
                    info = inspectorState.info,
                    result = inspectorState.result,
                    isAnalyzing = inspectorState.isAnalyzing,
                    onOpenWith = onOpenWith,
                    onViewOnMap = onViewOnMap,
                    onShareCleaned = onShareCleaned,
                    onDismiss = {
                        scope.launch { bottomSheetState.hide() }
                        inspectorViewModel.clearSelection()
                    },
                    ignoredRiskIds = ignoredIdsFor(inspectorState),
                    onIgnoreRisk = { risk ->
                        inspectorState.selectedFile?.path?.let { path ->
                            inspectorViewModel.ignoreRisk(path, risk.id)
                        }
                    },
                    onRestoreIgnored = { inspectorViewModel.restoreIgnoredRisks() },
                    onOpenAs = { cat -> inspectorViewModel.openAs(cat) },
                    onReinspect = { inspectorViewModel.reinspect() },
                    onSearchInside = onSearchInside,
                    onExportReport = onExportReport,
                    forcedCategory = inspectorState.forcedCategory,
                    detectedCategory = inspectorState.detectedCategory,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            FileListPanel(
                currentPath = fileListState.currentPath,
                homePath = fileListState.homePath,
                files = fileListState.files,
                isLoading = fileListState.isLoading,
                isRoot = fileListState.isRoot,
                selectedFilePath = inspectorState.selectedFile?.path,
                selectedPaths = commanderState.selectedPaths,
                sortOrder = fileListState.sortOrder,
                showHidden = fileListState.showHidden,
                errorMessage = fileListState.error,
                isSearchActive = fileListState.isSearchActive,
                searchQuery = fileListState.searchQuery,
                isSearching = fileListState.isSearching,
                onFileClick = { item ->
                    if (commanderState.selectedPaths.isNotEmpty()) {
                        commanderViewModel.toggleSelection(item.path)
                    } else {
                        val navigated = fileListViewModel.onFileClick(item)
                        if (!navigated) {
                            inspectorViewModel.selectFile(item)
                            scope.launch { bottomSheetState.expand() }
                        }
                    }
                },
                onFileLongClick = { item -> commanderViewModel.toggleSelection(item.path) },
                canCompareSelection = canCompare,
                onClearSelection = { commanderViewModel.clearSelection() },
                onCompareSelection = triggerCompare,
                onNavigateUp = { fileListViewModel.navigateUp() },
                onSortChanged = { fileListViewModel.setSortOrder(it) },
                onToggleHidden = { fileListViewModel.toggleHiddenFiles() },
                onSearchActiveChanged = { fileListViewModel.setSearchActive(it) },
                onSearchQueryChanged = { fileListViewModel.onSearchQueryChanged(it) },
                onSearchRecursive = { fileListViewModel.searchRecursive(it) },
                onRefresh = onRefresh,
                onBatchScan = onBatchScan,
                onFolderDashboard = onFolderDashboard,
                onOpenSettings = onOpenSettings,
                onBookmarkFolder = onBookmarkFolder,
                onShowBookmarks = onShowBookmarks,
                onShowRecents = onShowRecents,
                onNavigateToPath = { path -> fileListViewModel.navigateTo(path) },
                modifier = Modifier.fillMaxSize()
            )
        }

        CommanderToolbar(
            hasSelection = commanderState.selectedPaths.isNotEmpty(),
            canCompare = canCompare,
            onView = {
                val firstSelected = commanderState.selectedPaths.firstOrNull()
                val item = fileListState.files.find { it.path == firstSelected }
                if (item != null) {
                    inspectorViewModel.selectFile(item)
                    scope.launch { bottomSheetState.expand() }
                }
            },
            onCopy = { commanderViewModel.showDialog(DialogType.COPY) },
            onMove = { commanderViewModel.showDialog(DialogType.MOVE) },
            onNewFolder = { commanderViewModel.showDialog(DialogType.NEW_FOLDER) },
            onDelete = { commanderViewModel.showDialog(DialogType.DELETE) },
            onRename = { commanderViewModel.showDialog(DialogType.RENAME) },
            onCompare = triggerCompare
        )
    }
}

private fun exportAndShareReport(
    asJson: Boolean,
    inspectorViewModel: InspectorViewModel,
    context: android.content.Context
) {
    inspectorViewModel.exportReport(asJson) { file ->
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = if (asJson) "application/json" else "text/markdown"
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share report"))
    }
}

private fun ignoredIdsFor(state: InspectorState): Set<String> {
    val path = state.selectedFile?.path ?: return emptySet()
    val prefix = "$path::"
    return state.ignoredRiskKeys
        .asSequence()
        .filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix) }
        .toSet()
}

@Composable
private fun ExpandedLayout(
    fileListState: FileListState,
    inspectorState: InspectorState,
    commanderState: com.filelens.commander.viewmodel.CommanderState,
    fileListViewModel: FileListViewModel,
    inspectorViewModel: InspectorViewModel,
    commanderViewModel: CommanderViewModel,
    onOpenWith: () -> Unit,
    onViewOnMap: (Double, Double) -> Unit,
    onShareCleaned: () -> Unit,
    onBatchScan: () -> Unit,
    onFolderDashboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onBookmarkFolder: () -> Unit,
    onShowBookmarks: () -> Unit,
    onShowRecents: () -> Unit,
    onRefresh: () -> Unit,
    onSearchInside: () -> Unit,
    onExportReport: (Boolean) -> Unit,
    onCompare: (String, String) -> Unit
) {
    var dualPanel by remember { mutableStateOf(false) }
    var activeLeft by remember { mutableStateOf(true) }
    val rightViewModel: FileListViewModel = hiltViewModel(key = "rightPanel")
    val rightState by rightViewModel.state.collectAsStateWithLifecycle()

    val activeFiles = if (activeLeft) fileListState.files else rightState.files
    val selectedItems = activeFiles.filter { it.path in commanderState.selectedPaths }
    val canCompare = selectedItems.size == 2 && selectedItems.map { it.nodeType }.toSet().size == 1
    val triggerCompare = {
        if (selectedItems.size == 2) {
            onCompare(selectedItems[0].path, selectedItems[1].path)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            FileListPanel(
                currentPath = fileListState.currentPath,
                homePath = fileListState.homePath,
                files = fileListState.files,
                isLoading = fileListState.isLoading,
                isRoot = fileListState.isRoot,
                selectedFilePath = inspectorState.selectedFile?.path,
                selectedPaths = commanderState.selectedPaths,
                sortOrder = fileListState.sortOrder,
                showHidden = fileListState.showHidden,
                errorMessage = fileListState.error,
                isSearchActive = fileListState.isSearchActive,
                searchQuery = fileListState.searchQuery,
                isSearching = fileListState.isSearching,
                onFileClick = { item ->
                    activeLeft = true
                    if (commanderState.selectedPaths.isNotEmpty()) {
                        commanderViewModel.toggleSelection(item.path)
                    } else {
                        val navigated = fileListViewModel.onFileClick(item)
                        if (!navigated) {
                            inspectorViewModel.selectFile(item)
                        }
                    }
                },
                onFileLongClick = { item -> commanderViewModel.toggleSelection(item.path) },
                canCompareSelection = canCompare && activeLeft,
                onClearSelection = { commanderViewModel.clearSelection() },
                onCompareSelection = triggerCompare,
                onNavigateUp = { fileListViewModel.navigateUp() },
                onSortChanged = { fileListViewModel.setSortOrder(it) },
                onToggleHidden = { fileListViewModel.toggleHiddenFiles() },
                onSearchActiveChanged = { fileListViewModel.setSearchActive(it) },
                onSearchQueryChanged = { fileListViewModel.onSearchQueryChanged(it) },
                onSearchRecursive = { fileListViewModel.searchRecursive(it) },
                onRefresh = onRefresh,
                onBatchScan = onBatchScan,
                onFolderDashboard = onFolderDashboard,
                onOpenSettings = onOpenSettings,
                onBookmarkFolder = onBookmarkFolder,
                onShowBookmarks = onShowBookmarks,
                onShowRecents = onShowRecents,
                onNavigateToPath = { path -> fileListViewModel.navigateTo(path) },
                isActive = activeLeft || !dualPanel,
                modifier = Modifier.weight(1f)
            )

            VerticalDivider()

            if (dualPanel) {
                FileListPanel(
                    currentPath = rightState.currentPath,
                    homePath = rightState.homePath,
                    files = rightState.files,
                    isLoading = rightState.isLoading,
                    isRoot = rightState.isRoot,
                    selectedFilePath = inspectorState.selectedFile?.path,
                    selectedPaths = commanderState.selectedPaths,
                    sortOrder = rightState.sortOrder,
                    showHidden = rightState.showHidden,
                    errorMessage = rightState.error,
                    isSearchActive = rightState.isSearchActive,
                    searchQuery = rightState.searchQuery,
                    isSearching = rightState.isSearching,
                    onFileClick = { item ->
                        activeLeft = false
                        if (commanderState.selectedPaths.isNotEmpty()) {
                            commanderViewModel.toggleSelection(item.path)
                        } else {
                            val navigated = rightViewModel.onFileClick(item)
                            if (!navigated) {
                                inspectorViewModel.selectFile(item)
                            }
                        }
                    },
                    onFileLongClick = { item -> commanderViewModel.toggleSelection(item.path) },
                    canCompareSelection = canCompare && !activeLeft,
                    onClearSelection = { commanderViewModel.clearSelection() },
                    onCompareSelection = triggerCompare,
                    onNavigateUp = { rightViewModel.navigateUp() },
                    onSortChanged = { rightViewModel.setSortOrder(it) },
                    onToggleHidden = { rightViewModel.toggleHiddenFiles() },
                    onSearchActiveChanged = { rightViewModel.setSearchActive(it) },
                    onSearchQueryChanged = { rightViewModel.onSearchQueryChanged(it) },
                    onSearchRecursive = { rightViewModel.searchRecursive(it) },
                    onRefresh = { rightViewModel.refresh() },
                    onOpenSettings = onOpenSettings,
                    onBookmarkFolder = onBookmarkFolder,
                    onShowBookmarks = onShowBookmarks,
                    onShowRecents = onShowRecents,
                    onNavigateToPath = { path -> rightViewModel.navigateTo(path) },
                    isActive = !activeLeft,
                    modifier = Modifier.weight(1f)
                )
            } else {
                InspectorPanel(
                    info = inspectorState.info,
                    result = inspectorState.result,
                    isAnalyzing = inspectorState.isAnalyzing,
                    onOpenWith = onOpenWith,
                    onViewOnMap = onViewOnMap,
                    onShareCleaned = onShareCleaned,
                    ignoredRiskIds = ignoredIdsFor(inspectorState),
                    onIgnoreRisk = { risk ->
                        inspectorState.selectedFile?.path?.let { path ->
                            inspectorViewModel.ignoreRisk(path, risk.id)
                        }
                    },
                    onRestoreIgnored = { inspectorViewModel.restoreIgnoredRisks() },
                    onOpenAs = { cat -> inspectorViewModel.openAs(cat) },
                    onReinspect = { inspectorViewModel.reinspect() },
                    onSearchInside = onSearchInside,
                    onExportReport = onExportReport,
                    forcedCategory = inspectorState.forcedCategory,
                    detectedCategory = inspectorState.detectedCategory,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        androidx.compose.material3.HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            androidx.compose.material3.FilterChip(
                selected = dualPanel,
                onClick = { dualPanel = !dualPanel },
                label = { androidx.compose.material3.Text(if (dualPanel) "Dual panel" else "Inspector") }
            )
        }

        CommanderToolbar(
            hasSelection = commanderState.selectedPaths.isNotEmpty(),
            canCompare = canCompare,
            onView = {
                val firstSelected = commanderState.selectedPaths.firstOrNull()
                val item = fileListState.files.find { it.path == firstSelected }
                if (item != null) inspectorViewModel.selectFile(item)
            },
            onCopy = { commanderViewModel.showDialog(DialogType.COPY) },
            onMove = { commanderViewModel.showDialog(DialogType.MOVE) },
            onNewFolder = { commanderViewModel.showDialog(DialogType.NEW_FOLDER) },
            onDelete = { commanderViewModel.showDialog(DialogType.DELETE) },
            onRename = { commanderViewModel.showDialog(DialogType.RENAME) },
            onCompare = triggerCompare
        )
    }
}
