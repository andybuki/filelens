package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileItem
import com.filelens.commander.domain.model.classifyByExtension
import com.filelens.commander.vfs.NodeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recursive global file search across the local filesystem. Emits incremental
 * batches of [FileItem] matches so the UI can show partial results while the
 * scan continues. The flow is cancellable — collecting it from a coroutine
 * scope that gets cancelled (e.g. cancelling the search button) immediately
 * stops the walk.
 *
 * Matches against file name, extension, and detected [FileCategory]. The
 * caller picks the home roots to scan; the use case clamps recursion depth
 * and skips common noise directories (`.thumbnails`, app-private caches).
 */
@Singleton
class GlobalSearchUseCase @Inject constructor() {

    data class Result(
        val items: List<FileItem>,
        val scannedFolders: Int,
        val isDone: Boolean
    )

    fun search(
        roots: List<File>,
        query: String,
        categoryFilter: FileCategory? = null,
        maxDepth: Int = 8,
        maxHits: Int = 500,
        batchSize: Int = 25
    ): Flow<Result> = channelFlow {
        val needle = query.trim().lowercase()
        val out = mutableListOf<FileItem>()
        var scannedFolders = 0
        var pendingBatch = 0

        suspend fun emit(done: Boolean) {
            send(Result(out.toList(), scannedFolders, done))
            pendingBatch = 0
        }

        for (root in roots) {
            if (!root.exists() || !root.canRead()) continue
            walk(
                dir = root,
                needle = needle,
                categoryFilter = categoryFilter,
                maxDepth = maxDepth,
                onFolder = { scannedFolders++ },
                onMatch = { item ->
                    out.add(item)
                    pendingBatch++
                    if (pendingBatch >= batchSize) emit(false)
                    out.size < maxHits
                }
            )
            if (out.size >= maxHits) break
        }
        emit(true)
    }.flowOn(Dispatchers.IO)

    /**
     * Returns true if scanning should continue. [onMatch] returns true when
     * more matches are still wanted; once it returns false we unwind.
     */
    private suspend fun walk(
        dir: File,
        needle: String,
        categoryFilter: FileCategory?,
        maxDepth: Int,
        onFolder: () -> Unit,
        onMatch: suspend (FileItem) -> Boolean,
        depth: Int = 0
    ): Boolean {
        if (depth > maxDepth) return true
        currentCoroutineContext().ensureActive()

        val children = try { dir.listFiles() } catch (_: Exception) { null } ?: return true
        onFolder()

        for (file in children) {
            currentCoroutineContext().ensureActive()

            val name = file.name
            if (SKIP_DIR_NAMES.contains(name)) continue

            val ext = file.extension
            val category = if (file.isDirectory) FileCategory.DIRECTORY
                else classifyByExtension(ext)

            if (matches(name, ext, category, needle, categoryFilter)) {
                val item = FileItem(
                    name = name,
                    path = file.absolutePath,
                    nodeType = if (file.isDirectory) NodeType.DIRECTORY else NodeType.FILE,
                    size = if (file.isFile) file.length() else null,
                    lastModified = file.lastModified(),
                    isReadable = file.canRead(),
                    isHidden = file.isHidden,
                    fileCategory = category,
                    extension = ext
                )
                val wantMore = onMatch(item)
                if (!wantMore) return false
            }

            if (file.isDirectory && file.canRead()) {
                val keepGoing = walk(
                    dir = file,
                    needle = needle,
                    categoryFilter = categoryFilter,
                    maxDepth = maxDepth,
                    onFolder = onFolder,
                    onMatch = onMatch,
                    depth = depth + 1
                )
                if (!keepGoing) return false
            }
        }
        return true
    }

    private fun matches(
        name: String,
        extension: String,
        category: FileCategory,
        needle: String,
        categoryFilter: FileCategory?
    ): Boolean {
        if (categoryFilter != null && category != categoryFilter) return false
        if (needle.isEmpty()) return categoryFilter != null
        if (name.lowercase().contains(needle)) return true
        if (extension.lowercase() == needle) return true
        if (category.name.lowercase().contains(needle)) return true
        return false
    }

    private companion object {
        val SKIP_DIR_NAMES = setOf(
            ".thumbnails",
            ".trash",
            ".trashed",
            ".nomedia",
            "cache",
            "code_cache"
        )
    }
}
