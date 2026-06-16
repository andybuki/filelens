package com.filelens.commander.viewmodel

import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.StorageUsageCache
import com.filelens.commander.domain.usecase.FolderAnalysisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUsageState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val rootPath: String = "",
    val deviceLabel: String = "",
    val totalBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val summary: FolderAnalysisUseCase.FolderSummary? = null,
    val error: String? = null
)

@HiltViewModel
class StorageUsageViewModel @Inject constructor(
    private val folderAnalysisUseCase: FolderAnalysisUseCase,
    private val cache: StorageUsageCache
) : ViewModel() {

    private val _state = MutableStateFlow(StorageUsageState())
    val state: StateFlow<StorageUsageState> = _state.asStateFlow()

    init {
        load()
    }

    /**
     * Show cached state immediately (so revisits feel instant) and trigger a
     * background refresh. When there's no cache, the screen sits on its
     * loading state until the first scan finishes.
     */
    fun load() {
        val cached = cache.get()
        if (cached != null) {
            _state.value = StorageUsageState(
                isLoading = false,
                isRefreshing = true,
                rootPath = cached.summary.folderPath,
                deviceLabel = deviceLabel(),
                totalBytes = cached.totalBytes,
                freeBytes = cached.freeBytes,
                summary = cached.summary
            )
        } else {
            _state.value = _state.value.copy(
                isLoading = true,
                deviceLabel = deviceLabel()
            )
        }
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
        // Update volume stats right away — those are cheap to read and let
        // the header feel correct while the recursive scan runs.
        _state.value = _state.value.copy(
            rootPath = root,
            totalBytes = total,
            freeBytes = free,
            isRefreshing = true
        )
        viewModelScope.launch {
            try {
                val summary = folderAnalysisUseCase.analyze(root, recursive = true)
                cache.put(
                    StorageUsageCache.Snapshot(
                        summary = summary,
                        totalBytes = total,
                        freeBytes = free,
                        capturedAt = System.currentTimeMillis()
                    )
                )
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    summary = summary
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message
                )
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
