package com.filelens.commander.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.SortOrder
import com.filelens.commander.domain.usecase.BrowseFilesUseCase
import com.filelens.commander.vfs.LocalFileNode
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
    val isRoot: Boolean = true
)

@HiltViewModel
class FileListViewModel @Inject constructor(
    private val browseFilesUseCase: BrowseFilesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FileListState())
    val state: StateFlow<FileListState> = _state.asStateFlow()

    private val navigationStack = mutableListOf<String>()

    init {
        val startPath = Environment.getExternalStorageDirectory().absolutePath
        navigateTo(startPath)
    }

    fun navigateTo(path: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val node = LocalFileNode(File(path))
                val files = browseFilesUseCase.listFiles(
                    node = node,
                    sortOrder = _state.value.sortOrder,
                    showHidden = _state.value.showHidden
                )
                if (navigationStack.lastOrNull() != path) {
                    navigationStack.add(path)
                }
                _state.value = _state.value.copy(
                    currentPath = path,
                    files = files,
                    isLoading = false,
                    isRoot = navigationStack.size <= 1
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to list files"
                )
            }
        }
    }

    fun navigateUp() {
        if (navigationStack.size > 1) {
            navigationStack.removeLast()
            val parentPath = navigationStack.removeLast()
            navigateTo(parentPath)
        }
    }

    fun onFileClick(item: FileItem): Boolean {
        return if (item.fileCategory == com.filelens.commander.domain.model.FileCategory.DIRECTORY) {
            navigateTo(item.path)
            true
        } else {
            false
        }
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
        val current = _state.value.currentPath
        if (current.isNotEmpty()) {
            navigationStack.removeLastOrNull()
            navigateTo(current)
        }
    }
}
