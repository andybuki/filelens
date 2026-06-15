package com.filelens.commander.viewmodel

import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.usecase.FolderAnalysisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUsageState(
    val isLoading: Boolean = true,
    val rootPath: String = "",
    val deviceLabel: String = "",
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val summary: FolderAnalysisUseCase.FolderSummary? = null,
    val error: String? = null
)

@HiltViewModel
class StorageUsageViewModel @Inject constructor(
    private val folderAnalysisUseCase: FolderAnalysisUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StorageUsageState())
    val state: StateFlow<StorageUsageState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val ext = Environment.getExternalStorageDirectory()
        val root = ext?.absolutePath.orEmpty()
        val (total, free) = try {
            val statFs = StatFs(root)
            statFs.totalBytes to statFs.availableBytes
        } catch (_: Exception) {
            0L to 0L
        }
        _state.value = StorageUsageState(
            isLoading = true,
            rootPath = root,
            deviceLabel = deviceLabel(),
            totalBytes = total,
            freeBytes = free
        )
        viewModelScope.launch {
            try {
                val summary = folderAnalysisUseCase.analyze(root, recursive = true)
                _state.value = _state.value.copy(isLoading = false, summary = summary)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    private fun deviceLabel(): String {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() }.orEmpty()
        val model = Build.MODEL.orEmpty()
        return when {
            model.isBlank() -> "Phone"
            manufacturer.isBlank() || model.startsWith(manufacturer, ignoreCase = true) -> model
            else -> "$manufacturer $model"
        }
    }
}
