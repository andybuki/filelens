package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.usecase.BatchRenameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BatchRenameState(
    val sourcePaths: List<String> = emptyList(),
    val rule: BatchRenameUseCase.Rule = BatchRenameUseCase.Rule(pattern = "{name}{ext}"),
    val plan: BatchRenameUseCase.Plan = BatchRenameUseCase.Plan(emptyList(), emptySet()),
    val applying: Boolean = false,
    val done: Boolean = false
)

@HiltViewModel
class BatchRenameViewModel @Inject constructor(
    private val batchRename: BatchRenameUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(BatchRenameState())
    val state: StateFlow<BatchRenameState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun start(sourcePaths: List<String>) {
        if (sourcePaths.isEmpty()) return
        val rule = BatchRenameUseCase.Rule(pattern = "{name}{ext}")
        _state.value = BatchRenameState(
            sourcePaths = sourcePaths,
            rule = rule,
            plan = batchRename.plan(sourcePaths, rule)
        )
    }

    fun updateRule(rule: BatchRenameUseCase.Rule) {
        _state.value = _state.value.copy(
            rule = rule,
            plan = batchRename.plan(_state.value.sourcePaths, rule)
        )
    }

    fun commit(onDone: () -> Unit) {
        if (_state.value.applying) return
        viewModelScope.launch {
            _state.value = _state.value.copy(applying = true)
            val result = batchRename.apply(_state.value.plan)
            _state.value = _state.value.copy(applying = false, done = true)
            _events.emit(
                buildString {
                    append("Renamed ${result.renamed}")
                    if (result.skipped > 0) append(", skipped ${result.skipped}")
                    if (result.errors.isNotEmpty()) append(", ${result.errors.size} failed")
                }
            )
            onDone()
        }
    }

    fun reset() {
        _state.value = BatchRenameState()
    }
}
