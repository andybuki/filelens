package com.filelens.commander.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.ui.components.CommanderToolbar
import com.filelens.commander.ui.components.CopyMoveDialog
import com.filelens.commander.ui.components.DeleteConfirmDialog
import com.filelens.commander.ui.components.FileListPanel
import com.filelens.commander.ui.components.InspectorPanel
import com.filelens.commander.ui.components.NewFolderDialog
import com.filelens.commander.ui.components.RenameDialog
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
    fileListViewModel: FileListViewModel = hiltViewModel(),
    inspectorViewModel: InspectorViewModel = hiltViewModel(),
    commanderViewModel: CommanderViewModel = hiltViewModel()
) {
    val fileListState by fileListViewModel.state.collectAsStateWithLifecycle()
    val inspectorState by inspectorViewModel.state.collectAsStateWithLifecycle()
    val commanderState by commanderViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

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

    val refreshCurrentDir = { fileListViewModel.navigateTo(fileListState.currentPath) }

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

    BackHandler(enabled = !fileListState.isRoot || commanderState.selectedPaths.isNotEmpty()) {
        if (commanderState.selectedPaths.isNotEmpty()) {
            commanderViewModel.clearSelection()
        } else {
            fileListViewModel.navigateUp()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isExpandedScreen) {
            ExpandedLayout(
                fileListState = fileListState,
                inspectorState = inspectorState,
                commanderState = commanderState,
                fileListViewModel = fileListViewModel,
                inspectorViewModel = inspectorViewModel,
                commanderViewModel = commanderViewModel,
                onOpenWith = onOpenWith
            )
        } else {
            CompactLayout(
                fileListState = fileListState,
                inspectorState = inspectorState,
                commanderState = commanderState,
                fileListViewModel = fileListViewModel,
                inspectorViewModel = inspectorViewModel,
                commanderViewModel = commanderViewModel,
                onOpenWith = onOpenWith
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
    onOpenWith: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

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
                    onDismiss = {
                        scope.launch { bottomSheetState.hide() }
                        inspectorViewModel.clearSelection()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            FileListPanel(
                currentPath = fileListState.currentPath,
                files = fileListState.files,
                isLoading = fileListState.isLoading,
                isRoot = fileListState.isRoot,
                selectedFilePath = inspectorState.selectedFile?.path,
                sortOrder = fileListState.sortOrder,
                showHidden = fileListState.showHidden,
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
                onNavigateUp = { fileListViewModel.navigateUp() },
                onSortChanged = { fileListViewModel.setSortOrder(it) },
                onToggleHidden = { fileListViewModel.toggleHiddenFiles() },
                modifier = Modifier.fillMaxSize()
            )
        }

        CommanderToolbar(
            hasSelection = commanderState.selectedPaths.isNotEmpty(),
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
            onRename = { commanderViewModel.showDialog(DialogType.RENAME) }
        )
    }
}

@Composable
private fun ExpandedLayout(
    fileListState: FileListState,
    inspectorState: InspectorState,
    commanderState: com.filelens.commander.viewmodel.CommanderState,
    fileListViewModel: FileListViewModel,
    inspectorViewModel: InspectorViewModel,
    commanderViewModel: CommanderViewModel,
    onOpenWith: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(1f)) {
            FileListPanel(
                currentPath = fileListState.currentPath,
                files = fileListState.files,
                isLoading = fileListState.isLoading,
                isRoot = fileListState.isRoot,
                selectedFilePath = inspectorState.selectedFile?.path,
                sortOrder = fileListState.sortOrder,
                showHidden = fileListState.showHidden,
                onFileClick = { item ->
                    if (commanderState.selectedPaths.isNotEmpty()) {
                        commanderViewModel.toggleSelection(item.path)
                    } else {
                        val navigated = fileListViewModel.onFileClick(item)
                        if (!navigated) {
                            inspectorViewModel.selectFile(item)
                        }
                    }
                },
                onNavigateUp = { fileListViewModel.navigateUp() },
                onSortChanged = { fileListViewModel.setSortOrder(it) },
                onToggleHidden = { fileListViewModel.toggleHiddenFiles() },
                modifier = Modifier.weight(1f)
            )

            VerticalDivider()

            InspectorPanel(
                info = inspectorState.info,
                result = inspectorState.result,
                isAnalyzing = inspectorState.isAnalyzing,
                onOpenWith = onOpenWith,
                modifier = Modifier.weight(1f)
            )
        }

        CommanderToolbar(
            hasSelection = commanderState.selectedPaths.isNotEmpty(),
            onView = {
                val firstSelected = commanderState.selectedPaths.firstOrNull()
                val item = fileListState.files.find { it.path == firstSelected }
                if (item != null) inspectorViewModel.selectFile(item)
            },
            onCopy = { commanderViewModel.showDialog(DialogType.COPY) },
            onMove = { commanderViewModel.showDialog(DialogType.MOVE) },
            onNewFolder = { commanderViewModel.showDialog(DialogType.NEW_FOLDER) },
            onDelete = { commanderViewModel.showDialog(DialogType.DELETE) },
            onRename = { commanderViewModel.showDialog(DialogType.RENAME) }
        )
    }
}
