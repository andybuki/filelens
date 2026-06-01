package com.filelens.commander.vfs

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalFileNode(
    private val file: File,
    override val parent: VirtualFileNode? = null
) : VirtualFileNode {

    override val name: String get() = file.name.ifEmpty { file.absolutePath }
    override val path: String get() = file.absolutePath
    override val type: NodeType get() = if (file.isDirectory) NodeType.DIRECTORY else NodeType.FILE
    override val size: Long? get() = if (file.isFile) file.length() else null
    override val lastModified: Long? get() = file.lastModified().takeIf { it > 0 }
    override val isReadable: Boolean get() = file.canRead()

    val isHidden: Boolean get() = file.isHidden

    override suspend fun children(): List<VirtualFileNode> = withContext(Dispatchers.IO) {
        if (!file.isDirectory) return@withContext emptyList()
        file.listFiles()
            ?.map { LocalFileNode(it, this@LocalFileNode) }
            ?: emptyList()
    }

    override suspend fun openStream(): InputStream? = withContext(Dispatchers.IO) {
        if (file.isFile && file.canRead()) FileInputStream(file) else null
    }

    val absoluteFile: File get() = file

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LocalFileNode) return false
        return file.absolutePath == other.file.absolutePath
    }

    override fun hashCode(): Int = file.absolutePath.hashCode()

    override fun toString(): String = "LocalFileNode($path)"
}
