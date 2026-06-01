package com.filelens.commander.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.SortOrder
import com.filelens.commander.domain.usecase.BrowseFilesUseCase
import com.filelens.commander.vfs.LocalFileNode
import com.filelens.commander.vfs.NodeType
import com.filelens.commander.vfs.VirtualFileNode
import com.filelens.commander.vfs.ZipEntryNode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FileListState(
    val currentPath: String = "",
    val files: List<FileItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val sortOrder: SortOrder = SortOrder(),
    val showHidden: Boolean = false,
    val isRoot: Boolean = true,
    val isInsideArchive: Boolean = false
)

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val browseFilesUseCase: BrowseFilesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FileListState())
    val state: StateFlow<FileListState> = _state.asStateFlow()

    private val nodeStack = mutableListOf<VirtualFileNode>()

    init {
        val startPath = Environment.getExternalStorageDirectory().absolutePath
        navigateTo(startPath)
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            val node = LocalFileNode(File(path))
            navigateToNode(node, replaceStack = true)
        }
    }

    private suspend fun navigateToNode(node: VirtualFileNode, replaceStack: Boolean = false) {
        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            val files = browseFilesUseCase.listFiles(
                node = node,
                sortOrder = _state.value.sortOrder,
                showHidden = _state.value.showHidden
            )
            if (replaceStack) {
                nodeStack.clear()
                nodeStack.add(node)
            } else if (nodeStack.lastOrNull()?.path != node.path) {
                nodeStack.add(node)
            }
            _state.value = _state.value.copy(
                currentPath = node.path,
                files = files,
                isLoading = false,
                isRoot = nodeStack.size <= 1,
                isInsideArchive = node is ZipEntryNode
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to list files"
            )
        }
    }

    fun navigateUp() {
        if (nodeStack.size > 1) {
            nodeStack.removeAt(nodeStack.lastIndex)
            viewModelScope.launch {
                navigateToNode(nodeStack.removeAt(nodeStack.lastIndex))
            }
        }
    }

    fun onFileClick(item: FileItem): Boolean {
        if (item.fileCategory == FileCategory.DIRECTORY || item.nodeType == NodeType.DIRECTORY) {
            val current = nodeStack.lastOrNull()
            if (current is ZipEntryNode) {
                viewModelScope.launch {
                    val child = current.children().find { it.name == item.name }
                    if (child != null) navigateToNode(child)
                }
            } else {
                navigateTo(item.path)
            }
            return true
        }
        if (item.fileCategory == FileCategory.ARCHIVE && item.extension.lowercase() in setOf("zip", "jar", "apk", "war", "ear", "aab", "docx", "xlsx", "pptx")) {
            viewModelScope.launch {
                val archive = ZipEntryNode.fromFile(File(item.path), nodeStack.lastOrNull())
                if (archive != null) navigateToNode(archive)
            }
            return true
        }
        return false
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _state.value = _state.value.copy(sortOrder = sortOrder)
        refreshCurrentDirectory()
    }

    fun toggleHiddenFiles() {
        _state.value = _state.value.copy(showHidden = !_state.value.showHidden)
        refreshCurrentDirectory()
    }

    private fun refreshCurrentDirectory() {
        val current = nodeStack.lastOrNull() ?: return
        viewModelScope.launch {
            if (nodeStack.isNotEmpty()) nodeStack.removeAt(nodeStack.lastIndex)
            navigateToNode(current)
        }
    }
}
