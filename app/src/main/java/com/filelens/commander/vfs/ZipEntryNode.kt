package com.filelens.commander.vfs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ZipEntryNode private constructor(
    override val name: String,
    override val path: String,
    override val type: NodeType,
    override val size: Long?,
    override val lastModified: Long?,
    override val parent: VirtualFileNode?,
    private val zipFilePath: String,
    private val entryName: String?,
    private val childEntries: List<ZipEntryInfo>
) : VirtualFileNode {

    override val isReadable: Boolean = true

    data class ZipEntryInfo(
        val name: String,
        val fullPath: String,
        val isDirectory: Boolean,
        val size: Long,
        val compressedSize: Long,
        val crc: Long,
        val method: Int,
        val time: Long
    )

    override suspend fun children(): List<VirtualFileNode> = withContext(Dispatchers.IO) {
        val prefix = if (entryName != null) "$entryName/" else ""
        val directChildren = mutableMapOf<String, MutableList<ZipEntryInfo>>()

        for (entry in childEntries) {
            val relativePath = entry.fullPath.removePrefix(prefix)
            if (relativePath.isEmpty()) continue

            val parts = relativePath.split("/").filter { it.isNotEmpty() }
            if (parts.isEmpty()) continue

            val childName = parts[0]
            val isDirectChild = parts.size == 1 && !entry.isDirectory
            val isDirectChildDir = parts.size >= 1 && (parts.size > 1 || entry.isDirectory)

            if (isDirectChild || isDirectChildDir) {
                directChildren.getOrPut(childName) { mutableListOf() }.add(entry)
            }
        }

        val uniqueNames = mutableSetOf<String>()
        childEntries.forEach { entry ->
            val relativePath = entry.fullPath.removePrefix(prefix)
            val parts = relativePath.split("/").filter { it.isNotEmpty() }
            if (parts.isNotEmpty()) uniqueNames.add(parts[0])
        }

        uniqueNames.sorted().map { childName ->
            val childPrefix = if (prefix.isEmpty()) childName else "$prefix$childName"
            val matchingEntries = childEntries.filter {
                it.fullPath == childName ||
                it.fullPath == "$childName/" ||
                it.fullPath.startsWith("$childPrefix/") ||
                it.fullPath == childPrefix
            }

            val isDir = matchingEntries.any { it.isDirectory || it.fullPath.contains("$childName/") }
            val directEntry = matchingEntries.find { it.fullPath == childPrefix && !it.isDirectory }

            ZipEntryNode(
                name = childName,
                path = "$path/$childName",
                type = if (isDir && directEntry == null) NodeType.DIRECTORY else NodeType.ARCHIVE_ENTRY,
                size = directEntry?.size,
                lastModified = directEntry?.time ?: matchingEntries.firstOrNull()?.time,
                parent = this@ZipEntryNode,
                zipFilePath = zipFilePath,
                entryName = childPrefix,
                childEntries = childEntries.filter { it.fullPath.startsWith("$childPrefix/") }
            )
        }
    }

    override suspend fun openStream(): InputStream? = withContext(Dispatchers.IO) {
        if (entryName == null) return@withContext null
        try {
            val zipFile = ZipFile(zipFilePath)
            val entry = zipFile.getEntry(entryName) ?: return@withContext null
            zipFile.getInputStream(entry)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun fromFile(file: File, parent: VirtualFileNode? = null): ZipEntryNode? {
            return try {
                val zipFile = ZipFile(file)
                val entries = zipFile.entries().toList().map { entry ->
                    ZipEntryInfo(
                        name = entry.name.trimEnd('/').substringAfterLast('/'),
                        fullPath = entry.name.trimEnd('/'),
                        isDirectory = entry.isDirectory,
                        size = entry.size,
                        compressedSize = entry.compressedSize,
                        crc = entry.crc,
                        method = entry.method,
                        time = entry.time
                    )
                }
                zipFile.close()

                ZipEntryNode(
                    name = file.name,
                    path = file.absolutePath,
                    type = NodeType.DIRECTORY,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    parent = parent,
                    zipFilePath = file.absolutePath,
                    entryName = null,
                    childEntries = entries
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
