package com.filelens.commander.vfs

import com.filelens.commander.data.RemoteConnectionsRepository
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
    val session: RemoteSession,
    /** Set on first-trust SFTP connects to surface the captured host key. */
    val capturedHostKeyFingerprint: String? = null
)

/**
 * Opens a [RemoteConnection] and returns the root [VirtualFileNode] plus the
 * underlying session. Holds at most one live session per connection id;
 * opening the same connection twice closes the previous one first.
 *
 * For SFTP, the first connect to a new host runs in trust-on-first-use mode
 * and persists the server's host key so subsequent connects can pin it. If a
 * pinned key fails to match, a [HostKeyMismatchException] propagates so the
 * UI can refuse the connection.
 */
@Singleton
class RemoteConnector @Inject constructor(
    private val connectionsRepository: RemoteConnectionsRepository
) {

    private val sessions = mutableMapOf<String, RemoteSession>()
    private val mutex = Mutex()

    suspend fun open(connection: RemoteConnection): RemoteOpenResult = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessions.remove(connection.id)?.let { existing ->
                try { existing.close() } catch (_: Exception) {}
            }
            var capturedFingerprint: String? = null
            val session: RemoteSession = when (connection.protocol) {
                RemoteProtocol.FTP -> FtpSession(connection).also { it.connect() }
                RemoteProtocol.SFTP -> SftpSession(connection).also {
                    val result = it.connect()
                    if (result.capturedHostKey != null) {
                        connectionsRepository.saveHostKey(connection.id, result.capturedHostKey)
                        capturedFingerprint = result.fingerprint
                    }
                }
                RemoteProtocol.SMB -> SmbSession(connection).also { it.connect() }
            }
            sessions[connection.id] = session
            val root: VirtualFileNode = when (session) {
                is FtpSession -> session.rootNode(null)
                is SftpSession -> session.rootNode(null)
                is SmbSession -> session.rootNode(null)
                else -> error("Unknown session type: $session")
            }
            RemoteOpenResult(root, session, capturedFingerprint)
        }
    }

    suspend fun closeAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            sessions.values.forEach { runCatching { it.close() } }
            sessions.clear()
        }
    }
}
