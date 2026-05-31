package com.filelens.commander.vfs

import java.io.InputStream

enum class NodeType {
    FILE,
    DIRECTORY,
    ARCHIVE_ENTRY,
    VIRTUAL_ROOT
}

interface VirtualFileNode {
    val name: String
    val path: String
    val type: NodeType
    val size: Long?
    val lastModified: Long?
    val isReadable: Boolean
    val parent: VirtualFileNode?

    suspend fun children(): List<VirtualFileNode>
    suspend fun openStream(): InputStream?
}
