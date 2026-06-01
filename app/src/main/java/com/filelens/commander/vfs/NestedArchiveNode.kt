package com.filelens.commander.vfs

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/**
 * Wraps a [ZipEntryNode] that itself represents a ZIP archive, materialising
 * the inner archive's bytes into the application cache directory so it can be
 * navigated as a regular [ZipEntryNode]. This enables recursive navigation of
 * archives nested inside archives.
 */
class NestedArchiveNode private constructor(
    private val zipEntry: ZipEntryNode,
    private val tempFile: File,
    private val delegate: ZipEntryNode
) : VirtualFileNode {

    override val name: String get() = zipEntry.name
    override val path: String get() = zipEntry.path
    override val type: NodeType = NodeType.DIRECTORY
    override val size: Long? get() = zipEntry.size
    override val lastModified: Long? get() = zipEntry.lastModified
    override val isReadable: Boolean = true
    override val parent: VirtualFileNode? get() = zipEntry.parent

    override suspend fun children(): List<VirtualFileNode> = delegate.children()

    override suspend fun openStream(): InputStream? = zipEntry.openStream()

    /**
     * Best-effort cleanup of the materialised temp file. Safe to call multiple
     * times; failures are swallowed.
     */
    fun cleanup() {
        try {
            if (tempFile.exists()) tempFile.delete()
        } catch (_: Exception) {
        }
    }

    companion object {
        private val ZIP_EXTENSIONS = setOf(
            "zip", "jar", "war", "ear", "apk", "aab",
            "docx", "xlsx", "pptx", "odt", "ods", "odp", "epub"
        )

        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)
        private val ZIP_MAGIC_EMPTY = byteArrayOf(0x50, 0x4B, 0x05, 0x06)
        private val ZIP_MAGIC_SPANNED = byteArrayOf(0x50, 0x4B, 0x07, 0x08)

        /**
         * Returns true if the given [ZipEntryNode] looks like a ZIP archive,
         * either by file-name extension or by inspecting the first bytes of
         * its stream.
         */
        suspend fun looksLikeZip(zipEntry: ZipEntryNode): Boolean = withContext(Dispatchers.IO) {
            if (zipEntry.type != NodeType.ARCHIVE_ENTRY) return@withContext false
            val ext = zipEntry.name.substringAfterLast('.', "").lowercase()
            if (ext in ZIP_EXTENSIONS) return@withContext true
            try {
                zipEntry.openStream()?.use { input ->
                    val head = ByteArray(4)
                    val n = input.read(head)
                    if (n < 4) return@use false
                    head.contentEquals(ZIP_MAGIC) ||
                        head.contentEquals(ZIP_MAGIC_EMPTY) ||
                        head.contentEquals(ZIP_MAGIC_SPANNED)
                } ?: false
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Factory that materialises nested archives into cacheDir. Inject this
     * where you need to construct [NestedArchiveNode]s.
     */
    @javax.inject.Singleton
    class Factory @javax.inject.Inject constructor(
        @ApplicationContext private val context: Context
    ) {
        /**
         * Returns a [NestedArchiveNode] for the supplied entry if it is a ZIP
         * archive, otherwise null.
         */
        suspend fun from(zipEntry: ZipEntryNode): NestedArchiveNode? = withContext(Dispatchers.IO) {
            if (!looksLikeZip(zipEntry)) return@withContext null

            val nestedDir = File(context.cacheDir, "nested-archives").apply { mkdirs() }
            val safeName = zipEntry.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val tempFile = File.createTempFile("nested-${System.nanoTime()}-", "-$safeName", nestedDir)

            try {
                val stream = zipEntry.openStream() ?: run {
                    tempFile.delete()
                    return@withContext null
                }
                stream.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }

                val delegate = ZipEntryNode.fromFile(tempFile, parent = zipEntry.parent)
                if (delegate == null) {
                    tempFile.delete()
                    return@withContext null
                }
                NestedArchiveNode(zipEntry, tempFile, delegate)
            } catch (_: Exception) {
                try {
                    tempFile.delete()
                } catch (_: Exception) {
                }
                null
            }
        }
    }
}
