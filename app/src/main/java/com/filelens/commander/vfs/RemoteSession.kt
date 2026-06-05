package com.filelens.commander.vfs

/**
 * Lifecycle handle for a connected remote server. Closing must be idempotent
 * and safe to call multiple times.
 */
interface RemoteSession : AutoCloseable {
    override fun close()
}
