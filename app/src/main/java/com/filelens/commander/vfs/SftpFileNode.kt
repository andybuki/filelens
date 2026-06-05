package com.filelens.commander.vfs

import com.filelens.commander.domain.model.RemoteConnection
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
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
        // Keep the underlying SFTP session alive — caller is responsible for
        // closing the stream when done reading.
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

class SftpSession internal constructor(
    private val connection: RemoteConnection
) : RemoteSession {

    private var session: Session? = null
    private var channel: ChannelSftp? = null

    val rootUri: String = run {
        val portPart = if (connection.port > 0 && connection.port != 22) ":${connection.port}" else ""
        "sftp://${connection.host}$portPart"
    }

    @Synchronized
    fun connect() {
        val jsch = JSch()
        val s = jsch.getSession(connection.username, connection.host, connection.effectivePort)
        s.setPassword(connection.password)
        val props = Properties().apply {
            // No known-hosts checking on first connect — we have no UX for it yet.
            // A follow-up should surface the host fingerprint and let users pin it.
            put("StrictHostKeyChecking", "no")
            put("PreferredAuthentications", "password,keyboard-interactive")
        }
        s.setConfig(props)
        s.timeout = 15_000
        s.connect(15_000)
        val ch = s.openChannel("sftp") as ChannelSftp
        ch.connect(15_000)
        session = s
        channel = ch
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
}
