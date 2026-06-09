package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.VaultRepository
import com.filelens.commander.domain.model.VaultItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VaultState(
    val isUnlocked: Boolean = false,
    val items: List<VaultItem> = emptyList(),
    val selectedPaths: Set<String> = emptySet()
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val repo: VaultRepository
) : ViewModel() {

    val count: StateFlow<Int> = MutableStateFlow(repo.count()).also { sink ->
        viewModelScope.launch { repo.items.collect { sink.value = it.size } }
    }

    private val _state = MutableStateFlow(VaultState(items = repo.items.value))
    val state: StateFlow<VaultState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            repo.items.collect { current ->
                _state.value = _state.value.copy(
                    items = current,
                    // Drop selections whose underlying item disappeared.
                    selectedPaths = _state.value.selectedPaths.intersect(current.map { it.vaultPath }.toSet())
                )
            }
        }
    }

    fun unlock() {
        _state.value = _state.value.copy(isUnlocked = true)
    }

    fun lock() {
        _state.value = _state.value.copy(isUnlocked = false, selectedPaths = emptySet())
    }

    fun toggleSelection(path: String) {
        val current = _state.value.selectedPaths
        _state.value = _state.value.copy(
            selectedPaths = if (path in current) current - path else current + path
        )
    }

    fun clearSelection() {
        _state.value = _state.value.copy(selectedPaths = emptySet())
    }

    fun selectAll() {
        _state.value = _state.value.copy(
            selectedPaths = _state.value.items.map { it.vaultPath }.toSet()
        )
    }

    fun restoreSelected() {
        val targets = selectedItems()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val n = repo.restore(targets)
            _events.emit("Moved $n item(s) out of vault")
            clearSelection()
        }
    }

    fun eraseSelected() {
        val targets = selectedItems()
        if (targets.isEmpty()) return
        viewModelScope.launch {
            val n = repo.erase(targets)
            _events.emit("Permanently deleted $n item(s)")
            clearSelection()
        }
    }

    private fun selectedItems(): List<VaultItem> {
        val sel = _state.value.selectedPaths
        return _state.value.items.filter { it.vaultPath in sel }
    }
}
