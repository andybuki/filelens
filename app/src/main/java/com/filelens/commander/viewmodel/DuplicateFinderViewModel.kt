package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.usecase.FindDuplicatesUseCase
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

data class DuplicateFinderState(
    val rootPath: String = "",
    val isScanning: Boolean = false,
    val filesSeen: Int = 0,
    val hashCurrent: Int = 0,
    val hashTotal: Int = 0,
    val groups: List<FindDuplicatesUseCase.DuplicateGroup> = emptyList(),
    val totalFiles: Int = 0,
    val totalWastedBytes: Long = 0,
    val selectedForDeletion: Set<String> = emptySet()
)

@HiltViewModel
class DuplicateFinderViewModel @Inject constructor(
    private val findDuplicates: FindDuplicatesUseCase,
    private val trashUseCase: TrashUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(DuplicateFinderState())
    val state: StateFlow<DuplicateFinderState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun start(rootPath: String) {
        if (_state.value.rootPath == rootPath && _state.value.groups.isNotEmpty()) return
        _state.value = DuplicateFinderState(rootPath = rootPath, isScanning = true)
        viewModelScope.launch {
            findDuplicates.find(rootPath).collect { p ->
                _state.value = when (p) {
                    is FindDuplicatesUseCase.Progress.Scanning -> _state.value.copy(filesSeen = p.filesSeen)
                    is FindDuplicatesUseCase.Progress.Hashing -> _state.value.copy(
                        hashCurrent = p.current,
                        hashTotal = p.total
                    )
                    is FindDuplicatesUseCase.Progress.Done -> _state.value.copy(
                        isScanning = false,
                        groups = p.groups,
                        totalFiles = p.totalFiles,
                        totalWastedBytes = p.totalWastedBytes
                    )
                }
            }
        }
    }

    fun toggleSelected(path: String) {
        val sel = _state.value.selectedForDeletion
        _state.value = _state.value.copy(
            selectedForDeletion = if (path in sel) sel - path else sel + path
        )
    }

    /**
     * Convenience selector: in each duplicate group, mark every file except the
     * newest one. The user can untick anything they want to spare.
     */
    fun selectOldExtras() {
        val toDelete = _state.value.groups.flatMap { group ->
            val newest = group.files.maxByOrNull { it.lastModified } ?: return@flatMap emptyList()
            group.files.filter { it.path != newest.path }.map { it.path }
        }
        _state.value = _state.value.copy(selectedForDeletion = toDelete.toSet())
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedForDeletion = emptySet())
    }

    fun deleteSelected() {
        val paths = _state.value.selectedForDeletion.toList()
        if (paths.isEmpty()) return
        viewModelScope.launch {
            try {
                val trashed = trashUseCase.moveToTrash(paths)
                _events.emit("Moved ${trashed.size} duplicate(s) to trash")
                // Drop them from the visible groups so the user can see progress.
                val deletedSet = paths.toSet()
                val refreshed = _state.value.groups
                    .map { g -> g.copy(files = g.files.filter { it.path !in deletedSet }) }
                    .filter { it.files.size > 1 }
                _state.value = _state.value.copy(
                    groups = refreshed,
                    selectedForDeletion = emptySet(),
                    totalWastedBytes = refreshed.sumOf { it.wastedBytes }
                )
            } catch (e: Exception) {
                _events.emit("Delete failed: ${e.message}")
            }
        }
    }

    fun reset() {
        _state.value = DuplicateFinderState()
    }
}
