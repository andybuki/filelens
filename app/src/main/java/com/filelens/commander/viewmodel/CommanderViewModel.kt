package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.usecase.FileOperationResult
import com.filelens.commander.domain.usecase.FileOperationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DialogType {
    NONE,
    NEW_FOLDER,
    RENAME,
    DELETE,
    COPY,
    MOVE
}

data class CommanderState(
    val selectedPaths: Set<String> = emptySet(),
    val activeDialog: DialogType = DialogType.NONE,
    val isOperationInProgress: Boolean = false
)

@HiltViewModel
class CommanderViewModel @Inject constructor(
    private val fileOperations: FileOperationsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CommanderState())
    val state: StateFlow<CommanderState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun toggleSelection(path: String) {
        val current = _state.value.selectedPaths
        _state.value = _state.value.copy(
            selectedPaths = if (path in current) current - path else current + path
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedPaths = emptySet())
    }

    fun selectAll(files: List<FileItem>) {
        _state.value = _state.value.copy(
            selectedPaths = files.map { it.path }.toSet()
        )
    }

    fun showDialog(type: DialogType) {
        _state.value = _state.value.copy(activeDialog = type)
    }

    fun dismissDialog() {
        _state.value = _state.value.copy(activeDialog = DialogType.NONE)
    }

    fun createFolder(parentPath: String, name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            when (val result = fileOperations.createFolder(parentPath, name)) {
                is FileOperationResult.Success -> {
                    _events.emit("Folder created")
                    dismissDialog()
                    onDone()
                }
                is FileOperationResult.Error -> _events.emit("Error: ${result.message}")
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    fun rename(path: String, newName: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            when (val result = fileOperations.rename(path, newName)) {
                is FileOperationResult.Success -> {
                    _events.emit("File renamed")
                    clearSelection()
                    dismissDialog()
                    onDone()
                }
                is FileOperationResult.Error -> _events.emit("Error: ${result.message}")
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    fun deleteSelected(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            val paths = _state.value.selectedPaths.toList()
            when (val result = fileOperations.delete(paths)) {
                is FileOperationResult.Success -> {
                    _events.emit("${paths.size} item(s) deleted")
                    clearSelection()
                    dismissDialog()
                    onDone()
                }
                is FileOperationResult.Error -> _events.emit("Error: ${result.message}")
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    fun copySelected(targetDir: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            val paths = _state.value.selectedPaths.toList()
            when (val result = fileOperations.copy(paths, targetDir)) {
                is FileOperationResult.Success -> {
                    _events.emit("${paths.size} item(s) copied")
                    clearSelection()
                    dismissDialog()
                    onDone()
                }
                is FileOperationResult.Error -> _events.emit("Error: ${result.message}")
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    fun moveSelected(targetDir: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            val paths = _state.value.selectedPaths.toList()
            when (val result = fileOperations.move(paths, targetDir)) {
                is FileOperationResult.Success -> {
                    _events.emit("${paths.size} item(s) moved")
                    clearSelection()
                    dismissDialog()
                    onDone()
                }
                is FileOperationResult.Error -> _events.emit("Error: ${result.message}")
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }
}
