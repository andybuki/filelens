package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.SettingsRepository
import com.filelens.commander.domain.model.AppSettings
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import com.filelens.commander.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setShowHidden(value: Boolean) = viewModelScope.launch { repo.setShowHidden(value) }
    fun setSizeUnits(units: SizeUnits) = viewModelScope.launch { repo.setSizeUnits(units) }
    fun setDefaultSort(field: SortField, direction: SortDirection) = viewModelScope.launch {
        repo.setDefaultSort(field, direction)
    }
}
