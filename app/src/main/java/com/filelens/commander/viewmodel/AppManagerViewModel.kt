package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.domain.model.InstalledApp
import com.filelens.commander.domain.usecase.AppManagerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppFilter { USER, SYSTEM, ALL }

data class AppManagerState(
    val isLoading: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val filter: AppFilter = AppFilter.USER,
    val query: String = ""
)

sealed interface AppManagerEvent {
    data class ApkExtracted(val app: InstalledApp, val targetPath: String) : AppManagerEvent
    data class ShareApk(val app: InstalledApp, val targetPath: String) : AppManagerEvent
    data class OpenInspector(val apkPath: String) : AppManagerEvent
    data class ExtractFailed(val app: InstalledApp) : AppManagerEvent
}

@HiltViewModel
class AppManagerViewModel @Inject constructor(
    private val useCase: AppManagerUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AppManagerState())
    val state: StateFlow<AppManagerState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AppManagerEvent>()
    val events: SharedFlow<AppManagerEvent> = _events.asSharedFlow()

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val apps = useCase.listApps()
            _state.value = _state.value.copy(isLoading = false, apps = apps)
        }
    }

    fun setFilter(filter: AppFilter) {
        _state.value = _state.value.copy(filter = filter)
    }

    fun setQuery(query: String) {
        _state.value = _state.value.copy(query = query)
    }

    fun visibleApps(): List<InstalledApp> {
        val s = _state.value
        val byFilter = when (s.filter) {
            AppFilter.USER -> s.apps.filter { !it.isSystem || it.isUpdatedSystemApp }
            AppFilter.SYSTEM -> s.apps.filter { it.isSystem }
            AppFilter.ALL -> s.apps
        }
        val q = s.query.trim().lowercase()
        if (q.isEmpty()) return byFilter
        return byFilter.filter {
            it.label.lowercase().contains(q) || it.packageName.lowercase().contains(q)
        }
    }

    fun extractApk(app: InstalledApp) {
        viewModelScope.launch {
            val out = useCase.extractApk(app)
            if (out != null) {
                _events.emit(AppManagerEvent.ApkExtracted(app, out.absolutePath))
            } else {
                _events.emit(AppManagerEvent.ExtractFailed(app))
            }
        }
    }

    fun shareApk(app: InstalledApp) {
        viewModelScope.launch {
            val out = useCase.extractApkForShare(app)
            if (out != null) {
                _events.emit(AppManagerEvent.ShareApk(app, out.absolutePath))
            } else {
                _events.emit(AppManagerEvent.ExtractFailed(app))
            }
        }
    }

    fun openInspector(app: InstalledApp) {
        viewModelScope.launch {
            val out = useCase.extractApkForShare(app)
            if (out != null) {
                _events.emit(AppManagerEvent.OpenInspector(out.absolutePath))
            } else {
                _events.emit(AppManagerEvent.ExtractFailed(app))
            }
        }
    }
}
