package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.ShortcutsRepository
import com.filelens.commander.domain.model.ShortcutAction
import com.filelens.commander.domain.model.ShortcutBinding
import com.filelens.commander.domain.model.ShortcutMap
import com.filelens.commander.domain.model.defaultShortcuts
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val repo: ShortcutsRepository
) : ViewModel() {

    val shortcuts: StateFlow<ShortcutMap> = repo.shortcuts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = defaultShortcuts()
    )

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun assign(action: ShortcutAction, binding: ShortcutBinding) {
        viewModelScope.launch {
            // Two actions sharing a binding would silently break one of them at
            // runtime — much friendlier to swap so both keep working.
            val current = shortcuts.value
            val collision = current.entries.firstOrNull { (other, value) ->
                other != action && value.matches(binding)
            }
            if (collision != null) {
                repo.set(collision.key, current.getValue(action))
                _events.emit("Swapped with ${collision.key.label}")
            }
            repo.set(action, binding)
        }
    }

    fun resetDefaults() {
        viewModelScope.launch {
            repo.resetToDefaults()
            _events.emit("Defaults restored")
        }
    }
}
