package com.filelens.commander.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.filelens.commander.data.RemoteConnectionsRepository
import com.filelens.commander.data.RemoteOpenBus
import com.filelens.commander.domain.model.RemoteConnection
import com.filelens.commander.domain.model.RemoteProtocol
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RemoteServersViewModel @Inject constructor(
    private val repository: RemoteConnectionsRepository,
    private val openBus: RemoteOpenBus
) : ViewModel() {

    val connections: StateFlow<List<RemoteConnection>> = repository.connections.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun save(draft: RemoteConnection, clearHostKey: Boolean = false) {
        viewModelScope.launch {
            val saved = repository.upsert(draft)
            if (clearHostKey) repository.clearHostKey(saved.id)
        }
    }

    fun remove(id: String) {
        viewModelScope.launch { repository.remove(id) }
    }

    fun connect(id: String) {
        openBus.request(id)
    }

    fun blankDraft(): RemoteConnection = RemoteConnection(
        id = "",
        name = "",
        protocol = RemoteProtocol.SFTP,
        host = "",
        port = RemoteProtocol.SFTP.defaultPort,
        username = "",
        password = "",
        basePath = "",
        shareName = "",
        anonymous = false,
        ftpPassive = true,
        createdAt = System.currentTimeMillis()
    )
}
