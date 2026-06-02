package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.RecentsRepository
import com.filelens.commander.domain.model.FrequentFolder
import com.filelens.commander.domain.model.RecentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecentsViewModel @Inject constructor(
    private val repo: RecentsRepository
) : ViewModel() {

    val recentFiles: StateFlow<List<RecentFile>> = repo.recentFiles.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    val frequentFolders: StateFlow<List<FrequentFolder>> = repo.frequentFolders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun clear() = viewModelScope.launch { repo.clear() }
}
