package com.filelens.commander.vfs

/**
 * Lifecycle handle for a connected remote server. Closing must be idempotent
 * and safe to call multiple times.
 */
interface RemoteSession : AutoCloseable {
    /** Root URI for this session, e.g. `ftp://host:21` or `smb://host/share`. */
    val rootUri: String

    /**
     * Builds a FILE node for a previously-listed [fullPath] (which begins with
     * [rootUri]) so callers can open its stream without re-walking the tree.
     * Returns null if the path doesn't belong to this session.
     */
    fun fileNodeAt(fullPath: String): VirtualFileNode?

    override fun close()
}
