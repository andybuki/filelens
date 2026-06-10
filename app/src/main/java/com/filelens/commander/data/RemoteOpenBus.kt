package com.filelens.commander.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Thin event bus the Remote Servers screen uses to ask the file list to open
 * a saved connection after navigation pops back to main. Uses a Channel so the
 * event is delivered exactly once, even if the collector starts after the emit.
 */
@Singleton
class RemoteOpenBus @Inject constructor() {
    private val _events = Channel<String>(capacity = Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    fun request(connectionId: String) {
        _events.trySend(connectionId)
    }
}
