package com.filelens.commander.vfs

import com.filelens.commander.domain.model.RemoteConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * VFS node for browsing FTP servers. The underlying [FTPClient] is owned by
 * the [FtpSession] and shared across all nodes in the tree. Because the client
 * is stateful, callers must serialize operations — coroutines on
 * [Dispatchers.IO] are sufficient given typical user navigation.
 */
class FtpFileNode internal constructor(
    override val name: String,
    override val path: String,
    override val type: NodeType,
    override val size: Long?,
    override val lastModified: Long?,
    override val parent: VirtualFileNode?,
    private val session: FtpSession,
    private val remotePath: String
) : VirtualFileNode {

    override val isReadable: Boolean = true

    override suspend fun children(): List<VirtualFileNode> = withContext(Dispatchers.IO) {
        if (type != NodeType.DIRECTORY) return@withContext emptyList()
        val client = session.client()
        val entries: Array<FTPFile> = try {
            client.listFiles(remotePath)
        } catch (_: IOException) {
            return@withContext emptyList()
        } ?: return@withContext emptyList()

        entries.filter { it.name != "." && it.name != ".." }.map { file ->
            val childPath = joinPath(remotePath, file.name)
            FtpFileNode(
                name = file.name,
                path = "${session.rootUri}$childPath",
                type = if (file.isDirectory) NodeType.DIRECTORY else NodeType.FILE,
                size = file.size.takeIf { file.isFile },
                lastModified = file.timestamp?.timeInMillis,
                parent = this@FtpFileNode,
                session = session,
                remotePath = childPath
            )
        }
    }

    override suspend fun openStream(): InputStream? = withContext(Dispatchers.IO) {
        if (type != NodeType.FILE) return@withContext null
        val client = session.client()
        client.setFileType(FTP.BINARY_FILE_TYPE)
        val stream = try {
            client.retrieveFileStream(remotePath) ?: return@withContext null
        } catch (_: IOException) {
            return@withContext null
        }
        // The FTPClient requires completePendingCommand() after each retrieve.
        object : FilterInputStream(stream) {
            override fun close() {
                try {
                    super.close()
                } finally {
                    try {
                        client.completePendingCommand()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun joinPath(base: String, child: String): String {
        val b = base.trimEnd('/')
        return if (b.isEmpty()) "/$child" else "$b/$child"
    }
}

class FtpSession internal constructor(
    private val connection: RemoteConnection
) : RemoteSession {

    private val ftp = FTPClient()

    val rootUri: String = run {
        val portPart = if (connection.port > 0 && connection.port != 21) ":${connection.port}" else ""
        "ftp://${connection.host}$portPart"
    }

    fun client(): FTPClient = ftp

    @Synchronized
    fun connect() {
        ftp.connectTimeout = 15_000
        ftp.defaultTimeout = 30_000
        ftp.connect(connection.host, connection.effectivePort)
        if (!FTPReply.isPositiveCompletion(ftp.replyCode)) {
            ftp.disconnect()
            throw IOException("FTP server refused connection: ${ftp.replyString?.trim()}")
        }
        val user = if (connection.anonymous) "anonymous" else connection.username
        val pass = if (connection.anonymous) "anonymous@" else connection.password
        val loggedIn = ftp.login(user, pass)
        if (!loggedIn) {
            val reply = ftp.replyString?.trim() ?: ""
            ftp.disconnect()
            throw IOException(
                "FTP login failed for $user" +
                    (if (pass.isEmpty()) " (no password provided — edit the entry and re-enter your password)" else "") +
                    if (reply.isNotEmpty()) " — server: $reply" else ""
            )
        }
        ftp.setFileType(FTP.BINARY_FILE_TYPE)
        if (connection.ftpPassive) ftp.enterLocalPassiveMode() else ftp.enterLocalActiveMode()
    }

    override fun close() {
        try {
            if (ftp.isConnected) {
                try {
                    ftp.logout()
                } catch (_: Exception) {
                }
                ftp.disconnect()
            }
        } catch (_: Exception) {
        }
    }

    fun rootNode(parent: VirtualFileNode?): FtpFileNode {
        val basePath = connection.basePath.ifBlank { "/" }
        val displayName = connection.name.ifBlank { connection.host }
        return FtpFileNode(
            name = displayName,
            path = "$rootUri${if (basePath.startsWith("/")) basePath else "/$basePath"}",
            type = NodeType.DIRECTORY,
            size = null,
            lastModified = null,
            parent = parent,
            session = this,
            remotePath = if (basePath.startsWith("/")) basePath else "/$basePath"
        )
    }
}
