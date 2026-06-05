package com.filelens.commander.vfs

import com.filelens.commander.domain.model.RemoteConnection
import com.filelens.commander.domain.model.RemoteProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class RemoteOpenResult(
    val rootNode: VirtualFileNode,
    val session: RemoteSession
)

/**
 * Opens a [RemoteConnection] and returns the root [VirtualFileNode] plus the
 * underlying session. Holds at most one live session per connection id; opening
 * the same connection twice closes the previous one first.
 */
@Singleton
class RemoteConnector @Inject constructor() {

    private val sessions = mutableMapOf<String, RemoteSession>()
    private val mutex = Mutex()

    suspend fun open(connection: RemoteConnection): RemoteOpenResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessions.remove(connection.id)?.let { existing ->
                try { existing.close() } catch (_: Exception) {}
            }
            val session: RemoteSession = when (connection.protocol) {
                RemoteProtocol.FTP -> FtpSession(connection).also { it.connect() }
                RemoteProtocol.SFTP -> SftpSession(connection).also { it.connect() }
                RemoteProtocol.SMB -> SmbSession(connection).also { it.connect() }
            }
            sessions[connection.id] = session
            val root: VirtualFileNode = when (session) {
                is FtpSession -> session.rootNode(null)
                is SftpSession -> session.rootNode(null)
                is SmbSession -> session.rootNode(null)
                else -> error("Unknown session type: $session")
            }
            RemoteOpenResult(root, session)
        }
    }

    suspend fun closeAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessions.values.forEach { runCatching { it.close() } }
            sessions.clear()
        }
    }
}
