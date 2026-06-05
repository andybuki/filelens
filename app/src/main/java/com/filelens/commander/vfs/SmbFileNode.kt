package com.filelens.commander.vfs

import com.filelens.commander.domain.model.RemoteConnection
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.EnumSet

/**
 * VFS node for browsing SMB2/3 shares via smbj. Paths are stored relative to
 * the connected share. Sessions and the underlying [Connection] are owned by
 * the [SmbSession]; nodes share them across the tree.
 */
class SmbFileNode internal constructor(
    override val name: String,
    override val path: String,
    override val type: NodeType,
    override val size: Long?,
    override val lastModified: Long?,
    override val parent: VirtualFileNode?,
    private val session: SmbSession,
    private val relativePath: String
) : VirtualFileNode {

    override val isReadable: Boolean = true

    override suspend fun children(): List<VirtualFileNode> = withContext(Dispatchers.IO) {
        if (type != NodeType.DIRECTORY) return@withContext emptyList()
        val share = session.share()
        val pathArg = if (relativePath.isEmpty()) "" else relativePath
        val items = try {
            share.list(pathArg)
        } catch (_: Exception) {
            return@withContext emptyList()
        }
        items.asSequence()
            .filter { it.fileName != "." && it.fileName != ".." }
            .map { info ->
                val isDir = (info.fileAttributes and 0x10L) != 0L
                val childRel = if (relativePath.isEmpty()) info.fileName else "$relativePath\\${info.fileName}"
                SmbFileNode(
                    name = info.fileName,
                    path = "${session.rootUri}/${childRel.replace('\\', '/')}",
                    type = if (isDir) NodeType.DIRECTORY else NodeType.FILE,
                    size = info.endOfFile.takeIf { !isDir },
                    lastModified = info.lastWriteTime?.toEpochMillis(),
                    parent = this@SmbFileNode,
                    session = session,
                    relativePath = childRel
                )
            }
            .toList()
    }

    override suspend fun openStream(): InputStream? = withContext(Dispatchers.IO) {
        if (type != NodeType.FILE) return@withContext null
        val share = session.share()
        val file = try {
            share.openFile(
                relativePath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
        } catch (_: Exception) {
            return@withContext null
        }
        file.inputStream  // closing the stream closes the file handle
    }
}

class SmbSession internal constructor(
    private val connection: RemoteConnection
) : RemoteSession {

    private val client = SMBClient()
    private var conn: Connection? = null
    private var smbSession: Session? = null
    private var share: DiskShare? = null

    val rootUri: String = run {
        val portPart = if (connection.port > 0 && connection.port != 445) ":${connection.port}" else ""
        "smb://${connection.host}$portPart/${connection.shareName.trim('/')}"
    }

    @Synchronized
    fun connect() {
        val share = connection.shareName.trim('/').takeIf { it.isNotEmpty() }
            ?: throw IllegalArgumentException("SMB share name is required")
        val c = client.connect(connection.host, connection.effectivePort)
        val auth = if (connection.anonymous) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(
                connection.username,
                connection.password.toCharArray(),
                "" // domain — left blank; users can include DOMAIN\user in username if needed
            )
        }
        val sess = c.authenticate(auth)
        val ds = sess.connectShare(share) as DiskShare
        this.conn = c
        this.smbSession = sess
        this.share = ds
    }

    @Synchronized
    fun share(): DiskShare =
        share ?: throw IllegalStateException("SMB session not connected")

    override fun close() {
        try {
            share?.close()
        } catch (_: Exception) {
        }
        try {
            smbSession?.close()
        } catch (_: Exception) {
        }
        try {
            conn?.close()
        } catch (_: Exception) {
        }
        share = null
        smbSession = null
        conn = null
    }

    fun rootNode(parent: VirtualFileNode?): SmbFileNode {
        val base = connection.basePath.trim('/').replace('/', '\\')
        val displayName = connection.name.ifBlank { "${connection.host}/${connection.shareName}" }
        return SmbFileNode(
            name = displayName,
            path = "$rootUri/${base.replace('\\', '/')}",
            type = NodeType.DIRECTORY,
            size = null,
            lastModified = null,
            parent = parent,
            session = this,
            relativePath = base
        )
    }
}
