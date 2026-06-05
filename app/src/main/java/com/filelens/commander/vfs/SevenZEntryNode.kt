package com.filelens.commander.vfs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

/**
 * VFS node for 7-Zip archives (`.7z`). Backed by Apache commons-compress's
 * [SevenZFile]. Because 7z is a solid-archive format with sequential reads,
 * we walk the archive once on open and capture small entries' bytes inline;
 * larger entries can still be browsed (we know their metadata) but
 * [openStream] returns null for them.
 */
class SevenZEntryNode private constructor(
    override val name: String,
    override val path: String,
    override val type: NodeType,
    override val size: Long?,
    override val lastModified: Long?,
    override val parent: VirtualFileNode?,
    private val archivePath: String,
    private val entryName: String?,
    private val allEntries: List<SevenZEntryInfo>
) : VirtualFileNode {

    override val isReadable: Boolean = true

    data class SevenZEntryInfo(
        val name: String,
        val fullPath: String,
        val isDirectory: Boolean,
        val size: Long,
        val mtime: Long,
        val content: ByteArray?
    )

    override suspend fun children(): List<VirtualFileNode> = withContext(Dispatchers.IO) {
        val prefix = if (entryName != null) "$entryName/" else ""
        val seen = linkedMapOf<String, SevenZEntryInfo?>()

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
            SevenZEntryNode(
                name = childName,
                path = "$archivePath/$childFullPath",
                type = if (isDir) NodeType.DIRECTORY else NodeType.FILE,
                size = entry?.size?.takeIf { !isDir },
                lastModified = entry?.mtime,
                parent = this@SevenZEntryNode,
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

        suspend fun fromFile(file: File, parent: VirtualFileNode?): SevenZEntryNode? =
            withContext(Dispatchers.IO) {
                if (!file.isFile || !file.canRead()) return@withContext null
                val entries = try {
                    readEntries(file)
                } catch (_: Exception) {
                    return@withContext null
                }
                SevenZEntryNode(
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

        private fun readEntries(file: File): List<SevenZEntryInfo> {
            val out = mutableListOf<SevenZEntryInfo>()
            SevenZFile(file).use { sevenZ ->
                while (true) {
                    val entry = sevenZ.nextEntry ?: break
                    val rawName = entry.name?.replace('\\', '/')?.trim('/') ?: continue
                    if (rawName.isEmpty()) continue

                    val isDir = entry.isDirectory
                    val declaredSize = if (isDir) 0L else entry.size
                    val mtime = try {
                        if (entry.hasLastModifiedDate) entry.lastModifiedDate?.time ?: 0L else 0L
                    } catch (_: Throwable) {
                        0L
                    }

                    val content: ByteArray? = if (!isDir && declaredSize in 1..MAX_INLINE_BYTES) {
                        val baos = ByteArrayOutputStream(declaredSize.toInt())
                        val buf = ByteArray(64 * 1024)
                        var total = 0L
                        while (total < declaredSize) {
                            val toRead = minOf(buf.size.toLong(), declaredSize - total).toInt()
                            val n = sevenZ.read(buf, 0, toRead)
                            if (n <= 0) break
                            baos.write(buf, 0, n)
                            total += n
                        }
                        baos.toByteArray()
                    } else {
                        null
                    }

                    out += SevenZEntryInfo(
                        name = rawName.substringAfterLast('/'),
                        fullPath = rawName,
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
    }
}
