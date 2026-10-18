package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.SettingsRepository
import com.filelens.commander.domain.model.AppSettings
import com.filelens.commander.domain.model.PanelMode
import com.filelens.commander.domain.model.SizeUnits
import com.filelens.commander.domain.model.SortDirection
import com.filelens.commander.domain.model.SortField
import com.filelens.commander.domain.model.StartScreen
import com.filelens.commander.domain.model.ThemeMode
import com.filelens.commander.domain.usecase.BackupUseCase
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
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository,
    private val backupUseCase: BackupUseCase
) : ViewModel() {

    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    suspend fun exportToString(): String = backupUseCase.export()

    fun importFromJson(json: String) = viewModelScope.launch {
        val result = backupUseCase.importJson(json)
        _events.emit(result.message)
    }

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    fun setShowHidden(value: Boolean) = viewModelScope.launch { repo.setShowHidden(value) }
    fun setHideEmptyFolders(value: Boolean) = viewModelScope.launch { repo.setHideEmptyFolders(value) }
    fun setSizeUnits(units: SizeUnits) = viewModelScope.launch { repo.setSizeUnits(units) }
    fun setDefaultPanelMode(mode: PanelMode) = viewModelScope.launch { repo.setDefaultPanelMode(mode) }
    fun setDefaultSort(field: SortField, direction: SortDirection) = viewModelScope.launch {
        repo.setDefaultSort(field, direction)
    }
    fun setRemotePreviewLimitMb(mb: Int) = viewModelScope.launch { repo.setRemotePreviewLimitMb(mb) }
    fun setRemoteArchivePreviewLimitMb(mb: Int) = viewModelScope.launch { repo.setRemoteArchivePreviewLimitMb(mb) }
    fun setLanguageTag(tag: String?) = viewModelScope.launch { repo.setLanguageTag(tag) }
    fun setStartScreen(screen: StartScreen) = viewModelScope.launch { repo.setStartScreen(screen) }
    fun setAskBeforeDelete(value: Boolean) = viewModelScope.launch { repo.setAskBeforeDelete(value) }
    fun setTelemetryEnabled(value: Boolean) = viewModelScope.launch { repo.setTelemetryEnabled(value) }
    fun setWarnRootStorage(value: Boolean) = viewModelScope.launch { repo.setWarnRootStorage(value) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setDynamicColor(value: Boolean) = viewModelScope.launch { repo.setDynamicColor(value) }
    fun addSearchRoot(path: String) = viewModelScope.launch { repo.addSearchRoot(path) }
    fun removeSearchRoot(path: String) = viewModelScope.launch { repo.removeSearchRoot(path) }

    val hasSeenOnboarding: StateFlow<Boolean> = repo.hasSeenOnboarding.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    fun completeOnboarding() = viewModelScope.launch { repo.setHasSeenOnboarding(true) }
    fun resetOnboarding() = viewModelScope.launch { repo.setHasSeenOnboarding(false) }
}
