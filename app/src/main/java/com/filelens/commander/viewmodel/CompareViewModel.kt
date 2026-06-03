package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.model.FileCompareResult
import com.filelens.commander.domain.model.FolderCompareResult
import com.filelens.commander.domain.model.FolderEntryDiffKind
import com.filelens.commander.domain.usecase.CompareFilesUseCase
import com.filelens.commander.domain.usecase.CompareFoldersUseCase
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
import java.io.File
import javax.inject.Inject

data class CompareState(
    val leftPath: String = "",
    val rightPath: String = "",
    val isComparing: Boolean = false,
    val isFolderMode: Boolean = false,
    val fileResult: FileCompareResult? = null,
    val folderResult: FolderCompareResult? = null,
    val errorMessage: String? = null,
    val syncInProgress: Boolean = false,
    val deepVerify: Boolean = false
)

@HiltViewModel
class CompareViewModel @Inject constructor(
    private val compareFiles: CompareFilesUseCase,
    private val compareFolders: CompareFoldersUseCase,
    private val fileOperations: FileOperationsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(CompareState())
    val state: StateFlow<CompareState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun start(leftPath: String, rightPath: String, deepVerify: Boolean = _state.value.deepVerify) {
        val left = File(leftPath)
        val right = File(rightPath)
        if (!left.exists() || !right.exists()) {
            _state.value = CompareState(
                leftPath = leftPath,
                rightPath = rightPath,
                errorMessage = "One or both items don't exist"
            )
            return
        }

        val bothFolders = left.isDirectory && right.isDirectory
        val bothFiles = left.isFile && right.isFile
        if (!bothFolders && !bothFiles) {
            _state.value = CompareState(
                leftPath = leftPath,
                rightPath = rightPath,
                errorMessage = "Can only compare two files or two folders, not mixed"
            )
            return
        }

        _state.value = CompareState(
            leftPath = leftPath,
            rightPath = rightPath,
            isComparing = true,
            isFolderMode = bothFolders,
            deepVerify = deepVerify
        )

        viewModelScope.launch {
            try {
                if (bothFolders) {
                    val result = compareFolders.compare(leftPath, rightPath, deepVerify = deepVerify)
                    _state.value = _state.value.copy(
                        isComparing = false,
                        folderResult = result,
                        errorMessage = if (result == null) "Couldn't read one of the folders" else null
                    )
                } else {
                    val result = compareFiles.compare(leftPath, rightPath)
                    _state.value = _state.value.copy(
                        isComparing = false,
                        fileResult = result,
                        errorMessage = if (result == null) "Couldn't read one of the files" else null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isComparing = false,
                    errorMessage = e.message ?: "Compare failed"
                )
            }
        }
    }

    fun setDeepVerify(value: Boolean) {
        if (_state.value.isFolderMode && _state.value.deepVerify != value) {
            start(_state.value.leftPath, _state.value.rightPath, deepVerify = value)
        } else {
            _state.value = _state.value.copy(deepVerify = value)
        }
    }

    fun copyMissing(direction: SyncDirection) {
        val folderResult = _state.value.folderResult ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(syncInProgress = true)
            val sourceRoot: String
            val targetRoot: String
            val missingKind: FolderEntryDiffKind
            when (direction) {
                SyncDirection.LEFT_TO_RIGHT -> {
                    sourceRoot = folderResult.leftPath
                    targetRoot = folderResult.rightPath
                    missingKind = FolderEntryDiffKind.LEFT_ONLY
                }
                SyncDirection.RIGHT_TO_LEFT -> {
                    sourceRoot = folderResult.rightPath
                    targetRoot = folderResult.leftPath
                    missingKind = FolderEntryDiffKind.RIGHT_ONLY
                }
            }

            val missing = folderResult.entries.filter { !it.isDirectory && it.kind == missingKind }
            if (missing.isEmpty()) {
                _events.emit("Nothing missing to copy")
                _state.value = _state.value.copy(syncInProgress = false)
                return@launch
            }

            var copied = 0
            var failed = 0
            for (entry in missing) {
                val sourceFile = File(sourceRoot, entry.relativePath)
                val targetFile = File(targetRoot, entry.relativePath)
                targetFile.parentFile?.mkdirs()
                when (fileOperations.copy(listOf(sourceFile.absolutePath), targetFile.parent ?: targetRoot)) {
                    is FileOperationResult.Success -> copied++
                    is FileOperationResult.Error -> failed++
                }
            }
            _events.emit(buildString {
                append("Copied $copied file(s)")
                if (failed > 0) append(", $failed failed")
            })
            // Refresh the comparison so the UI reflects the new state.
            _state.value = _state.value.copy(syncInProgress = false)
            start(folderResult.leftPath, folderResult.rightPath)
        }
    }

    fun overwriteChanged(direction: SyncDirection) {
        val folderResult = _state.value.folderResult ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(syncInProgress = true)
            val sourceRoot = if (direction == SyncDirection.LEFT_TO_RIGHT) folderResult.leftPath else folderResult.rightPath
            val targetRoot = if (direction == SyncDirection.LEFT_TO_RIGHT) folderResult.rightPath else folderResult.leftPath

            val changed = folderResult.entries.filter { !it.isDirectory && it.kind == FolderEntryDiffKind.BOTH_CHANGED }
            if (changed.isEmpty()) {
                _events.emit("Nothing changed to overwrite")
                _state.value = _state.value.copy(syncInProgress = false)
                return@launch
            }

            var overwritten = 0
            var failed = 0
            for (entry in changed) {
                val sourceFile = File(sourceRoot, entry.relativePath)
                val targetFile = File(targetRoot, entry.relativePath)
                if (!sourceFile.isFile) {
                    failed++
                    continue
                }
                try {
                    targetFile.parentFile?.mkdirs()
                    sourceFile.copyTo(targetFile, overwrite = true)
                    overwritten++
                } catch (_: Exception) {
                    failed++
                }
            }
            _events.emit(buildString {
                append("Overwrote $overwritten file(s)")
                if (failed > 0) append(", $failed failed")
            })
            _state.value = _state.value.copy(syncInProgress = false)
            start(folderResult.leftPath, folderResult.rightPath)
        }
    }

    fun reset() {
        _state.value = CompareState()
    }
}

enum class SyncDirection { LEFT_TO_RIGHT, RIGHT_TO_LEFT }
