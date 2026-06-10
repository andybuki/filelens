package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class TrashState(
    val items: List<TrashUseCase.TrashedItem> = emptyList(),
    val isLoading: Boolean = true
)

sealed interface TrashEvent {
    data class Restored(val count: Int) : TrashEvent
    object Emptied : TrashEvent
}

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trash: TrashUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TrashState())
    val state: StateFlow<TrashState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TrashEvent>()
    val events: SharedFlow<TrashEvent> = _events.asSharedFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val items = trash.listTrashed()
            _state.value = TrashState(items = items, isLoading = false)
        }
    }

    fun restoreAll() {
        val items = _state.value.items
        if (items.isEmpty()) return
        viewModelScope.launch {
            val restored = trash.restore(items)
            _events.emit(TrashEvent.Restored(restored))
            refresh()
        }
    }

    fun restore(items: List<TrashUseCase.TrashedItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            val restored = trash.restore(items)
            _events.emit(TrashEvent.Restored(restored))
            refresh()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trash.emptyTrash()
            _events.emit(TrashEvent.Emptied)
            refresh()
        }
    }
}
