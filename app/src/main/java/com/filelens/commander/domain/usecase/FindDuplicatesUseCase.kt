package com.filelens.commander.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FindDuplicatesUseCase @Inject constructor(
    private val checksumUseCase: ChecksumUseCase
) {

    data class DuplicateFile(
        val path: String,
        val size: Long,
        val lastModified: Long
    )

    data class DuplicateGroup(
        val checksum: String,
        val size: Long,
        val files: List<DuplicateFile>
    ) {
        // Wasted bytes = (count - 1) * size; keeping one copy.
        val wastedBytes: Long get() = (files.size - 1).coerceAtLeast(0).toLong() * size
    }

    sealed class Progress {
        data class Scanning(val filesSeen: Int) : Progress()
        data class Hashing(val current: Int, val total: Int) : Progress()
        data class Done(
            val groups: List<DuplicateGroup>,
            val totalFiles: Int,
            val totalWastedBytes: Long
        ) : Progress()
    }

    /**
     * Two-pass dedup: walk + group by size first, then hash only files in
     * size-collision groups. A single-file size bucket can't have a duplicate
     * so it skips the expensive hash.
     */
    fun find(rootPath: String, minSize: Long = 1L): Flow<Progress> = flow {
        val root = File(rootPath)
        if (!root.isDirectory) {
            emit(Progress.Done(emptyList(), 0, 0L))
            return@flow
        }

        val allFiles = mutableListOf<File>()
        var seen = 0
        val stack = ArrayDeque<File>()
        stack += root
        while (stack.isNotEmpty()) {
            val dir = stack.removeFirst()
            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (allFiles.size >= MAX_FILES) break
                if (child.isDirectory && child.canRead()) {
                    stack += child
                } else if (child.isFile && child.canRead() && child.length() >= minSize) {
                    allFiles += child
                    seen++
                    if (seen % 200 == 0) emit(Progress.Scanning(seen))
                }
            }
        }

        val sizeGroups = allFiles.groupBy { it.length() }.filter { it.value.size > 1 }
        val candidates = sizeGroups.values.flatten()
        emit(Progress.Scanning(allFiles.size))

        val hashToFiles = linkedMapOf<String, MutableList<DuplicateFile>>()
        candidates.forEachIndexed { idx, file ->
            if (idx % 5 == 0) emit(Progress.Hashing(current = idx, total = candidates.size))
            val hash = checksumUseCase.quickSha256(file) ?: return@forEachIndexed
            hashToFiles.getOrPut(hash) { mutableListOf() }.add(
                DuplicateFile(
                    path = file.absolutePath,
                    size = file.length(),
                    lastModified = file.lastModified()
                )
            )
        }

        val groups = hashToFiles
            .filter { it.value.size > 1 }
            .map { (hash, files) ->
                DuplicateGroup(checksum = hash, size = files.first().size, files = files)
            }
            .sortedByDescending { it.wastedBytes }

        emit(
            Progress.Done(
                groups = groups,
                totalFiles = allFiles.size,
                totalWastedBytes = groups.sumOf { it.wastedBytes }
            )
        )
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val MAX_FILES = 200_000
    }
}
