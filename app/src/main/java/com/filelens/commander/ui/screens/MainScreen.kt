package com.filelens.commander.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.filelens.commander.ui.components.FileListPanel
import com.filelens.commander.ui.components.InspectorPanel
import com.filelens.commander.viewmodel.FileListViewModel
import com.filelens.commander.viewmodel.InspectorViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainScreen(
    fileListViewModel: FileListViewModel = hiltViewModel(),
    inspectorViewModel: InspectorViewModel = hiltViewModel()
) {
    val fileListState by fileListViewModel.state.collectAsStateWithLifecycle()
    val inspectorState by inspectorViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

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
                    // Could not open file
                }
            }
        }
    }

    BackHandler(enabled = !fileListState.isRoot) {
        fileListViewModel.navigateUp()
    }

    if (isExpandedScreen) {
        ExpandedLayout(
            fileListState = fileListState,
            inspectorState = inspectorState,
            fileListViewModel = fileListViewModel,
            inspectorViewModel = inspectorViewModel,
            onOpenWith = onOpenWith
        )
    } else {
        CompactLayout(
            fileListState = fileListState,
            inspectorState = inspectorState,
            fileListViewModel = fileListViewModel,
            inspectorViewModel = inspectorViewModel,
            onOpenWith = onOpenWith
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactLayout(
    fileListState: com.filelens.commander.viewmodel.FileListState,
    inspectorState: com.filelens.commander.viewmodel.InspectorState,
    fileListViewModel: FileListViewModel,
    inspectorViewModel: InspectorViewModel,
    onOpenWith: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(bottomSheetState = bottomSheetState)

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
        }
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
                val navigated = fileListViewModel.onFileClick(item)
                if (!navigated) {
                    inspectorViewModel.selectFile(item)
                    scope.launch { bottomSheetState.expand() }
                }
            },
            onNavigateUp = { fileListViewModel.navigateUp() },
            onSortChanged = { fileListViewModel.setSortOrder(it) },
            onToggleHidden = { fileListViewModel.toggleHiddenFiles() },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ExpandedLayout(
    fileListState: com.filelens.commander.viewmodel.FileListState,
    inspectorState: com.filelens.commander.viewmodel.InspectorState,
    fileListViewModel: FileListViewModel,
    inspectorViewModel: InspectorViewModel,
    onOpenWith: () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        FileListPanel(
            currentPath = fileListState.currentPath,
            files = fileListState.files,
            isLoading = fileListState.isLoading,
            isRoot = fileListState.isRoot,
            selectedFilePath = inspectorState.selectedFile?.path,
            sortOrder = fileListState.sortOrder,
            showHidden = fileListState.showHidden,
            onFileClick = { item ->
                val navigated = fileListViewModel.onFileClick(item)
                if (!navigated) {
                    inspectorViewModel.selectFile(item)
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
}
