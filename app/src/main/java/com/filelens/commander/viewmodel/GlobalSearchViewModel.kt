package com.filelens.commander.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.usecase.GlobalSearchUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class GlobalSearchState(
    val query: String = "",
    val categoryFilter: FileCategory? = null,
    val results: List<FileItem> = emptyList(),
    val scannedFolders: Int = 0,
    val isSearching: Boolean = false,
    val didFinish: Boolean = false,
    val didCancel: Boolean = false
)

@HiltViewModel
class GlobalSearchViewModel @Inject constructor(
    private val useCase: GlobalSearchUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(GlobalSearchState())
    val state: StateFlow<GlobalSearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun setCategoryFilter(category: FileCategory?) {
        _state.value = _state.value.copy(categoryFilter = category)
        // Re-run automatically if we already have a query — keeps the
        // filter chips feeling like a live narrowing of results rather
        // than a switch that needs an explicit "Search" press.
        if (_state.value.query.isNotBlank() || category != null) {
            runSearch()
        }
    }

    fun runSearch() {
        val current = _state.value
        if (current.query.isBlank() && current.categoryFilter == null) return

        searchJob?.cancel()
        _state.value = current.copy(
            isSearching = true,
            didFinish = false,
            didCancel = false,
            results = emptyList(),
            scannedFolders = 0
        )

        val root = Environment.getExternalStorageDirectory() ?: File("/")
        searchJob = viewModelScope.launch {
            try {
                useCase.search(
                    roots = listOf(root),
                    query = current.query,
                    categoryFilter = current.categoryFilter
                ).collect { result ->
                    _state.value = _state.value.copy(
                        results = result.items,
                        scannedFolders = result.scannedFolders,
                        isSearching = !result.isDone,
                        didFinish = result.isDone
                    )
                }
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Stopped by the user; preserve whatever partial results we had.
                _state.value = _state.value.copy(
                    isSearching = false,
                    didCancel = true,
                    didFinish = false
                )
            }
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        searchJob = null
    }

    fun clear() {
        searchJob?.cancel()
        _state.value = GlobalSearchState()
    }

    override fun onCleared() {
        searchJob?.cancel()
        super.onCleared()
    }
}
