package com.filelens.commander.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Thin event bus the Remote Servers screen uses to ask the file list to open
 * a saved connection after navigation pops back to main. Replays the latest
 * request so a fast pop doesn't drop it.
 */
@Singleton
class RemoteOpenBus @Inject constructor() {
    private val _events = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun request(connectionId: String) {
        _events.tryEmit(connectionId)
    }
}
