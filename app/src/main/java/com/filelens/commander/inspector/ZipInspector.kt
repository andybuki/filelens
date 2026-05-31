package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZipInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.ARCHIVE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()

            val fileEntries = entries.filter { !it.isDirectory }
            val dirEntries = entries.filter { it.isDirectory }
            val totalUncompressed = fileEntries.sumOf { it.size.coerceAtLeast(0) }
            val totalCompressed = fileEntries.sumOf { it.compressedSize.coerceAtLeast(0) }

            overview["Files"] = "%,d".format(fileEntries.size)
            overview["Folders"] = "%,d".format(dirEntries.size)
            overview["Compressed size"] = formatSize(file.length())
            overview["Uncompressed size"] = formatSize(totalUncompressed)
            if (totalUncompressed > 0) {
                val ratio = (1.0 - totalCompressed.toDouble() / totalUncompressed) * 100
                overview["Compression ratio"] = "%.1f%%".format(ratio)
            }

            val encrypted = entries.any { it.method == 99 }
            overview["Encrypted"] = if (encrypted) "Yes" else "No"

            val execExtensions = setOf("exe", "dll", "bat", "cmd", "sh", "ps1", "msi", "com", "scr", "pif", "vbs", "js", "wsf")
            val archiveExtensions = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")
            val executables = fileEntries.filter { entry ->
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                ext in execExtensions
            }
            val nestedArchives = fileEntries.filter { entry ->
                val ext = entry.name.substringAfterLast('.', "").lowercase()
                ext in archiveExtensions
            }

            if (executables.isNotEmpty()) {
                overview["Executables"] = executables.size.toString()
                risks.add(RiskItem(
                    severity = RiskSeverity.WARNING,
                    title = "Contains executables",
                    description = "This archive contains ${executables.size} executable file(s): ${executables.take(3).joinToString { it.name.substringAfterLast('/') }}"
                ))
            }
            if (nestedArchives.isNotEmpty()) {
                overview["Nested archives"] = nestedArchives.size.toString()
                risks.add(RiskItem(
                    severity = RiskSeverity.INFO,
                    title = "Nested archives",
                    description = "${nestedArchives.size} archive(s) inside: ${nestedArchives.take(3).joinToString { it.name.substringAfterLast('/') }}"
                ))
            }
            if (encrypted) {
                risks.add(RiskItem(
                    severity = RiskSeverity.INFO,
                    title = "Encrypted archive",
                    description = "Some entries in this archive are encrypted."
                ))
            }
            if (totalUncompressed > 0 && totalUncompressed > file.length() * 100) {
                risks.add(RiskItem(
                    severity = RiskSeverity.DANGER,
                    title = "Possible zip bomb",
                    description = "Decompression ratio is extremely high (${totalUncompressed / file.length()}x). This could be a zip bomb."
                ))
            }

            val extCounts = fileEntries.groupBy { it.name.substringAfterLast('.', "unknown").lowercase() }
                .mapValues { it.value.size }
                .entries.sortedByDescending { it.value }
                .take(10)
            metadata["File types"] = extCounts.joinToString(", ") { "${it.key}: ${it.value}" }

            val tree = buildZipTree(entries, maxEntries = 200)

            zipFile.close()

            InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                structure = tree
            )
        } catch (e: Exception) {
            overview["Error"] = "Failed to read ZIP: ${e.message?.take(100)}"
            InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun buildZipTree(entries: List<java.util.zip.ZipEntry>, maxEntries: Int): StructureNode {
        data class TreeNode(val name: String, val children: MutableMap<String, TreeNode> = mutableMapOf(), var size: Long? = null, var isFile: Boolean = false)

        val root = TreeNode("root")
        var count = 0

        for (entry in entries) {
            if (count >= maxEntries) break
            val parts = entry.name.trimEnd('/').split("/").filter { it.isNotEmpty() }
            var current = root
            for ((i, part) in parts.withIndex()) {
                current = current.children.getOrPut(part) { TreeNode(part) }
                if (i == parts.lastIndex && !entry.isDirectory) {
                    current.size = entry.size
                    current.isFile = true
                }
            }
            count++
        }

        fun toStructureNode(node: TreeNode): StructureNode {
            return if (node.children.isEmpty()) {
                StructureNode.Leaf(name = node.name, value = node.size?.let { formatSize(it) }, size = node.size)
            } else {
                val children = node.children.values
                    .sortedWith(compareBy<TreeNode> { it.isFile }.thenBy { it.name.lowercase() })
                    .map { toStructureNode(it) }
                StructureNode.Branch(name = node.name, children = children)
            }
        }

        val children = root.children.values
            .sortedWith(compareBy<TreeNode> { it.isFile }.thenBy { it.name.lowercase() })
            .map { toStructureNode(it) }

        return StructureNode.Branch(
            name = "Archive contents",
            children = if (entries.size > maxEntries) {
                children + StructureNode.Leaf(name = "... ${entries.size - maxEntries} more entries")
            } else children
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 0) return "?"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        val gb = mb / 1024.0
        return "%.1f GB".format(gb)
    }
}
