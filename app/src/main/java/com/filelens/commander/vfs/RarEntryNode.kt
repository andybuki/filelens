package com.filelens.commander.vfs

import com.github.junrar.Archive
import com.github.junrar.rarfile.FileHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * VFS node for RAR archives (`.rar`). Uses junrar, which only supports the
 * RAR4 format — RAR5 archives will fail to open. We walk the archive once
 * and inline small entries' bytes; larger entries are listable but
 * [openStream] returns null.
 */
class RarEntryNode private constructor(
    override val name: String,
    override val path: String,
    override val type: NodeType,
    override val size: Long?,
    override val lastModified: Long?,
    override val parent: VirtualFileNode?,
    private val archivePath: String,
    private val entryName: String?,
    private val allEntries: List<RarEntryInfo>
) : VirtualFileNode {

    override val isReadable: Boolean = true

    data class RarEntryInfo(
        val name: String,
        val fullPath: String,
        val isDirectory: Boolean,
        val size: Long,
        val mtime: Long,
        val content: ByteArray?
    )

    override suspend fun children(): List<VirtualFileNode> = withContext(Dispatchers.IO) {
        val prefix = if (entryName != null) "$entryName/" else ""
        val seen = linkedMapOf<String, RarEntryInfo?>()

        for (entry in allEntries) {
            val relative = entry.fullPath.removePrefix(prefix)
            if (relative.isEmpty() || entry.fullPath == entryName) continue
            if (prefix.isNotEmpty() && !entry.fullPath.startsWith(prefix)) continue

            val first = relative.split('/').firstOrNull()?.takeIf { it.isNotEmpty() } ?: continue
            val isLeaf = relative == first && !entry.isDirectory
            if (isLeaf) {
                seen[first] = entry
            } else if (seen[first] == null) {
                val dirPath = if (prefix.isEmpty()) first else "$prefix$first"
                seen[first] = allEntries.firstOrNull { it.fullPath == dirPath && it.isDirectory }
            }
        }

        seen.map { (childName, entry) ->
            val isDir = entry == null || entry.isDirectory
            val childFullPath = if (prefix.isEmpty()) childName else "$prefix$childName"
            RarEntryNode(
                name = childName,
                path = "$archivePath/$childFullPath",
                type = if (isDir) NodeType.DIRECTORY else NodeType.FILE,
                size = entry?.size?.takeIf { !isDir },
                lastModified = entry?.mtime,
                parent = this@RarEntryNode,
                archivePath = archivePath,
                entryName = childFullPath,
                allEntries = allEntries
            )
        }
    }

    override suspend fun openStream(): InputStream? = withContext(Dispatchers.IO) {
        if (type != NodeType.FILE) return@withContext null
        val entry = allEntries.firstOrNull { it.fullPath == entryName } ?: return@withContext null
        entry.content?.let { ByteArrayInputStream(it) }
    }

    companion object {
        const val MAX_INLINE_BYTES = 8L * 1024 * 1024
        const val MAX_ENTRIES = 50_000

        suspend fun fromFile(file: File, parent: VirtualFileNode?): RarEntryNode? =
            withContext(Dispatchers.IO) {
                if (!file.isFile || !file.canRead()) return@withContext null
                val entries = try {
                    readEntries(file)
                } catch (_: Exception) {
                    return@withContext null
                }
                RarEntryNode(
                    name = file.name,
                    path = file.absolutePath,
                    type = NodeType.DIRECTORY,
                    size = null,
                    lastModified = file.lastModified(),
                    parent = parent,
                    archivePath = file.absolutePath,
                    entryName = null,
                    allEntries = entries
                )
            }

        private fun readEntries(file: File): List<RarEntryInfo> {
            val out = mutableListOf<RarEntryInfo>()
            Archive(file).use { archive ->
                if (archive.isEncrypted) return emptyList()
                for (header in archive.fileHeaders) {
                    val raw = headerName(header)?.replace('\\', '/')?.trim('/') ?: continue
                    if (raw.isEmpty()) continue

                    val isDir = header.isDirectory
                    val declaredSize = if (isDir) 0L else header.fullUnpackSize
                    val mtime = try {
                        header.mTime?.time ?: 0L
                    } catch (_: Throwable) {
                        0L
                    }

                    val content: ByteArray? =
                        if (!isDir && !header.isEncrypted && declaredSize in 1..MAX_INLINE_BYTES) {
                            try {
                                val baos = ByteArrayOutputStream(declaredSize.toInt())
                                archive.extractFile(header, baos)
                                baos.toByteArray()
                            } catch (_: Throwable) {
                                null
                            }
                        } else {
                            null
                        }

                    out += RarEntryInfo(
                        name = raw.substringAfterLast('/'),
                        fullPath = raw,
                        isDirectory = isDir,
                        size = declaredSize,
                        mtime = mtime,
                        content = content
                    )
                    if (out.size >= MAX_ENTRIES) break
                }
            }
            return out
        }

        private fun headerName(header: FileHeader): String? {
            return try {
                header.fileName?.takeIf { it.isNotBlank() }
            } catch (_: Throwable) {
                null
            }
        }
    }
}
