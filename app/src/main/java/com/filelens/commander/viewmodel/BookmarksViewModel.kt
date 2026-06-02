package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.BookmarksRepository
import com.filelens.commander.domain.model.Bookmark
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BookmarksViewModel @Inject constructor(
    private val repo: BookmarksRepository
) : ViewModel() {

    val bookmarks: StateFlow<List<Bookmark>> = repo.bookmarks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )

    fun add(path: String, displayName: String) = viewModelScope.launch {
        repo.add(path, displayName)
    }

    fun remove(path: String) = viewModelScope.launch { repo.remove(path) }
}
