package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.OperationNotifier
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.usecase.CleanCopyUseCase
import com.filelens.commander.domain.usecase.FileOperationResult
import com.filelens.commander.domain.usecase.FileOperationsUseCase
import com.filelens.commander.domain.usecase.TrashUseCase
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
    private val fileOperations: FileOperationsUseCase,
    private val trashUseCase: TrashUseCase,
    private val cleanCopyUseCase: CleanCopyUseCase,
    private val vaultRepository: com.filelens.commander.data.VaultRepository,
    private val notifier: OperationNotifier
) : ViewModel() {

    private var lastTrashed: List<TrashUseCase.TrashedItem> = emptyList()
    fun lastTrashedItems(): List<TrashUseCase.TrashedItem> = lastTrashed
    fun restoreLast() {
        if (lastTrashed.isEmpty()) return
        viewModelScope.launch {
            val restored = trashUseCase.restore(lastTrashed)
            _events.emit("Restored $restored item(s)")
            lastTrashed = emptyList()
        }
    }

    suspend fun createCleanedCopyAndShare(sourcePath: String, share: (String) -> Unit) {
        val source = java.io.File(sourcePath)
        val targetDir = source.parent ?: return
        when (val result = cleanCopyUseCase.createCleanedCopy(sourcePath, targetDir)) {
            is CleanCopyUseCase.CleanResult.Success -> {
                _events.emit("Cleaned copy created. Stripped: ${result.strippedFields.joinToString().ifEmpty { "nothing" }}")
                share(result.cleanPath)
            }
            is CleanCopyUseCase.CleanResult.Error -> _events.emit("Error: ${result.message}")
        }
    }

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
            try {
                lastTrashed = trashUseCase.moveToTrash(paths)
                _events.emit("${paths.size} item(s) moved to trash (Ctrl+Z to undo)")
                clearSelection()
                dismissDialog()
                onDone()
            } catch (e: Exception) {
                _events.emit("Error: ${e.message ?: "Failed to delete"}")
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    fun copySelected(targetDir: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            val paths = _state.value.selectedPaths.toList()
            // Only ping the notification shade for batches; one-offs are noise.
            if (paths.size >= NOTIFY_THRESHOLD) {
                notifier.showProgress(OperationNotifier.ID_COPY, "Copying", 0, paths.size)
            }
            when (val result = fileOperations.copy(paths, targetDir)) {
                is FileOperationResult.Success -> {
                    _events.emit("${paths.size} item(s) copied")
                    if (paths.size >= NOTIFY_THRESHOLD) {
                        notifier.showComplete(
                            OperationNotifier.ID_COPY,
                            "Copy complete",
                            "${paths.size} item(s) copied"
                        )
                    }
                    clearSelection()
                    dismissDialog()
                    onDone()
                }
                is FileOperationResult.Error -> {
                    _events.emit("Error: ${result.message}")
                    notifier.cancel(OperationNotifier.ID_COPY)
                }
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    fun moveSelected(targetDir: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            val paths = _state.value.selectedPaths.toList()
            if (paths.size >= NOTIFY_THRESHOLD) {
                notifier.showProgress(OperationNotifier.ID_MOVE, "Moving", 0, paths.size)
            }
            when (val result = fileOperations.move(paths, targetDir)) {
                is FileOperationResult.Success -> {
                    _events.emit("${paths.size} item(s) moved")
                    if (paths.size >= NOTIFY_THRESHOLD) {
                        notifier.showComplete(
                            OperationNotifier.ID_MOVE,
                            "Move complete",
                            "${paths.size} item(s) moved"
                        )
                    }
                    clearSelection()
                    dismissDialog()
                    onDone()
                }
                is FileOperationResult.Error -> {
                    _events.emit("Error: ${result.message}")
                    notifier.cancel(OperationNotifier.ID_MOVE)
                }
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    fun moveSelectedToVault(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isOperationInProgress = true)
            val paths = _state.value.selectedPaths.toList()
            try {
                val moved = vaultRepository.add(paths)
                if (moved == paths.size) {
                    _events.emit("$moved item(s) moved to vault")
                } else {
                    _events.emit("Moved $moved of ${paths.size} item(s) to vault")
                }
                clearSelection()
                onDone()
            } catch (e: Exception) {
                _events.emit("Error: ${e.message ?: "Failed to move to vault"}")
            }
            _state.value = _state.value.copy(isOperationInProgress = false)
        }
    }

    private companion object {
        // Batches smaller than this go through the snackbar only — keeps a
        // routine two-file copy from spawning a notification.
        const val NOTIFY_THRESHOLD = 5
    }
}
