package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.usecase.BatchCleanShareUseCase
import com.filelens.commander.domain.usecase.BatchScanUseCase
import com.filelens.commander.domain.usecase.ScanState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchScanState(
    val isScanning: Boolean = false,
    val current: Int = 0,
    val total: Int = 0,
    val currentFile: String = "",
    val results: List<BatchScanUseCase.ScanResult> = emptyList(),
    val error: String? = null,
    val active: Boolean = false,
    val isCleaning: Boolean = false,
    val cleanedZipPath: String? = null,
    val cleanMessage: String? = null
)

@HiltViewModel
class BatchScanViewModel @Inject constructor(
    private val batchScanUseCase: BatchScanUseCase,
    private val batchCleanShareUseCase: BatchCleanShareUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BatchScanState())
    val state: StateFlow<BatchScanState> = _state.asStateFlow()

    private var job: Job? = null

    fun startScan(folderPath: String) {
        job?.cancel()
        _state.value = BatchScanState(isScanning = true, active = true)
        job = viewModelScope.launch {
            batchScanUseCase.scanFolder(folderPath).collect { scanState ->
                when (scanState) {
                    is ScanState.Progress -> _state.value = _state.value.copy(
                        current = scanState.current,
                        total = scanState.total,
                        currentFile = scanState.currentFile
                    )
                    is ScanState.Complete -> _state.value = _state.value.copy(
                        isScanning = false,
                        results = scanState.results
                    )
                    is ScanState.Error -> _state.value = _state.value.copy(
                        isScanning = false,
                        error = scanState.message
                    )
                }
            }
        }
    }

    fun cleanAndShareRisky() {
        val paths = _state.value.results
            .filter { it.bucket == BatchScanUseCase.Bucket.RISKY }
            .map { it.path }
        if (paths.isEmpty()) {
            _state.value = _state.value.copy(cleanMessage = "No risky files to clean")
            return
        }
        _state.value = _state.value.copy(isCleaning = true, cleanedZipPath = null, cleanMessage = null)
        viewModelScope.launch {
            val result = batchCleanShareUseCase.cleanAndPackage(paths)
            _state.value = _state.value.copy(
                isCleaning = false,
                cleanedZipPath = result.zipPath,
                cleanMessage = result.errorMessage ?: "Packaged ${result.cleanedCount} cleaned file(s)${if (result.skippedCount > 0) ", skipped ${result.skippedCount}" else ""}"
            )
        }
    }

    fun clearCleanedZip() {
        _state.value = _state.value.copy(cleanedZipPath = null, cleanMessage = null)
    }

    fun dismiss() {
        job?.cancel()
        _state.value = BatchScanState()
    }
}
