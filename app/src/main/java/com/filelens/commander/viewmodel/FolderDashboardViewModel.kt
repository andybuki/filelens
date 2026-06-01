package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.usecase.FolderAnalysisUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FolderDashboardState(
    val active: Boolean = false,
    val isAnalyzing: Boolean = false,
    val summary: FolderAnalysisUseCase.FolderSummary? = null,
    val error: String? = null
)

@HiltViewModel
class FolderDashboardViewModel @Inject constructor(
    private val folderAnalysisUseCase: FolderAnalysisUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(FolderDashboardState())
    val state: StateFlow<FolderDashboardState> = _state.asStateFlow()

    fun analyze(folderPath: String) {
        _state.value = FolderDashboardState(active = true, isAnalyzing = true)
        viewModelScope.launch {
            try {
                val summary = folderAnalysisUseCase.analyze(folderPath)
                _state.value = FolderDashboardState(active = true, isAnalyzing = false, summary = summary)
            } catch (e: Exception) {
                _state.value = FolderDashboardState(active = true, isAnalyzing = false, error = e.message)
            }
        }
    }

    fun dismiss() {
        _state.value = FolderDashboardState()
    }
}
