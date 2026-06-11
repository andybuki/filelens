package com.filelens.commander.vfs

import com.filelens.commander.domain.model.RemoteConnection
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.HostKey
import com.jcraft.jsch.HostKeyRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UIKeyboardInteractive
import com.jcraft.jsch.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FilterInputStream
import java.io.InputStream
import java.util.Properties

/**
 * VFS node for browsing SFTP (SSH) servers via JSch. The single [ChannelSftp]
 * is shared across nodes and serialized by callers — JSch channels are not
 * thread-safe across concurrent operations.
 */
class SftpFileNode internal constructor(
    override val name: String,
    override val path: String,
    override val type: NodeType,
    override val size: Long?,
    override val lastModified: Long?,
    override val parent: VirtualFileNode?,
    private val session: SftpSession,
    private val remotePath: String
) : VirtualFileNode {

    override val isReadable: Boolean = true

    override suspend fun children(): List<VirtualFileNode> = withContext(Dispatchers.IO) {
        if (type != NodeType.DIRECTORY) return@withContext emptyList()
        val channel = session.channel()
        @Suppress("UNCHECKED_CAST")
        val entries: List<ChannelSftp.LsEntry> = try {
            channel.ls(remotePath) as List<ChannelSftp.LsEntry>
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        entries.asSequence()
            .filter { it.filename != "." && it.filename != ".." }
            .map { entry ->
                val attrs = entry.attrs
                val childPath = joinPath(remotePath, entry.filename)
                SftpFileNode(
                    name = entry.filename,
                    path = "${session.rootUri}$childPath",
                    type = if (attrs.isDir) NodeType.DIRECTORY else NodeType.FILE,
                    size = attrs.size.takeIf { !attrs.isDir },
                    lastModified = attrs.mTime.toLong() * 1000L,
                    parent = this@SftpFileNode,
                    session = session,
                    remotePath = childPath
                )
            }
            .toList()
    }

    override suspend fun openStream(): InputStream? = withContext(Dispatchers.IO) {
        if (type != NodeType.FILE) return@withContext null
        val channel = session.channel()
        val raw = try {
            channel.get(remotePath) ?: return@withContext null
        } catch (_: Exception) {
            return@withContext null
        }
        object : FilterInputStream(raw) {
            override fun close() {
                try {
                    super.close()
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun joinPath(base: String, child: String): String {
        val b = base.trimEnd('/')
        return if (b.isEmpty()) "/$child" else "$b/$child"
    }
}

/**
 * Result of an SFTP connect attempt. On first connect for a new server, we
 * capture the server's host key into [capturedHostKey] — callers should
 * persist it so the next connect can pin against it.
 */
data class SftpConnectResult(
    val capturedHostKey: String?,
    val fingerprint: String?
)

/**
 * Thrown when a saved host key does not match what the server presented.
 * Surface this to the user verbatim — silently re-pinning would defeat the
 * point of pinning in the first place.
 */
class HostKeyMismatchException(
    val expected: String,
    val actual: String,
    val fingerprint: String?
) : RuntimeException("SFTP host key changed. Expected $expected but server sent $actual")

class SftpSession internal constructor(
    private val connection: RemoteConnection
) : RemoteSession {

    private var session: Session? = null
    private var channel: ChannelSftp? = null
    private var capturedHostKey: String? = null
    private var capturedFingerprint: String? = null

    override val rootUri: String = run {
        val portPart = if (connection.port > 0 && connection.port != 22) ":${connection.port}" else ""
        "sftp://${connection.host}$portPart"
    }

    @Synchronized
    fun connect(): SftpConnectResult {
        val jsch = JSch()
        val expected = connection.expectedHostKey.trim()
        val s = jsch.getSession(connection.username, connection.host, connection.effectivePort)
        s.setPassword(connection.password)
        // Servers that gate password auth behind keyboard-interactive need a
        // handler to answer the prompts — without one JSch has nothing to send
        // and authentication fails even with a correct password.
        s.userInfo = object : UserInfo, UIKeyboardInteractive {
            override fun getPassphrase(): String? = null
            override fun getPassword(): String = connection.password
            override fun promptPassword(message: String?): Boolean = true
            override fun promptPassphrase(message: String?): Boolean = false
            override fun promptYesNo(message: String?): Boolean = true
            override fun showMessage(message: String?) {}
            override fun promptKeyboardInteractive(
                destination: String?,
                name: String?,
                instruction: String?,
                prompt: Array<out String>?,
                echo: BooleanArray?
            ): Array<String>? = prompt?.let { Array(it.size) { connection.password } }
        }
        val props = Properties().apply {
            put("PreferredAuthentications", "password,keyboard-interactive")
            if (expected.isNotEmpty()) {
                put("StrictHostKeyChecking", "yes")
            } else {
                // First-time connect: accept whatever the server presents, then
                // hand the fingerprint back so the caller can pin it.
                put("StrictHostKeyChecking", "no")
            }
        }
        s.setConfig(props)
        if (expected.isNotEmpty()) {
            val parts = expected.split(' ', limit = 2)
            val keyBytes = try {
                android.util.Base64.decode(parts.last(), android.util.Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                null
            }
            if (keyBytes != null) {
                jsch.hostKeyRepository.add(
                    HostKey(connection.host, keyBytes),
                    null as com.jcraft.jsch.UserInfo?
                )
            }
        }
        s.timeout = 15_000
        try {
            s.connect(15_000)
        } catch (e: Exception) {
            // JSch reports key mismatches as JSchException with a "HostKey has
            // been changed" message — translate so the UI can react.
            val msg = e.message.orEmpty()
            if (expected.isNotEmpty() && (msg.contains("HostKey", ignoreCase = true) ||
                    msg.contains("reject", ignoreCase = true))) {
                throw HostKeyMismatchException(
                    expected = expected,
                    actual = s.hostKey?.let { "${it.type} ${it.key}" } ?: "<unknown>",
                    fingerprint = s.hostKey?.getFingerPrint(jsch)
                )
            }
            throw e
        }
        val hk = s.hostKey
        if (hk != null) {
            capturedHostKey = "${hk.type} ${hk.key}"
            capturedFingerprint = try { hk.getFingerPrint(jsch) } catch (_: Exception) { null }
        }
        val ch = s.openChannel("sftp") as ChannelSftp
        ch.connect(15_000)
        session = s
        channel = ch
        // Only report capture on first-trust connects so callers know to save.
        val firstTrustCapture = if (expected.isEmpty()) capturedHostKey else null
        return SftpConnectResult(firstTrustCapture, capturedFingerprint)
    }

    @Synchronized
    fun channel(): ChannelSftp =
        channel ?: throw IllegalStateException("SFTP session not connected")

    override fun close() {
        try {
            channel?.disconnect()
        } catch (_: Exception) {
        }
        try {
            session?.disconnect()
        } catch (_: Exception) {
        }
        channel = null
        session = null
    }

    fun rootNode(parent: VirtualFileNode?): SftpFileNode {
        val basePath = connection.basePath.ifBlank { "." }
        val normalized = if (basePath == "." || basePath.startsWith("/")) basePath else "/$basePath"
        val displayName = connection.name.ifBlank { connection.host }
        return SftpFileNode(
            name = displayName,
            path = "$rootUri/${normalized.trimStart('/')}",
            type = NodeType.DIRECTORY,
            size = null,
            lastModified = null,
            parent = parent,
            session = this,
            remotePath = normalized
        )
    }

    override fun fileNodeAt(fullPath: String): VirtualFileNode? {
        if (!fullPath.startsWith(rootUri)) return null
        val remotePath = fullPath.removePrefix(rootUri).ifEmpty { return null }
        return SftpFileNode(
            name = remotePath.substringAfterLast('/'),
            path = fullPath,
            type = NodeType.FILE,
            size = null,
            lastModified = null,
            parent = null,
            session = this,
            remotePath = remotePath
        )
    }
}
