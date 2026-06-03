package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.FolderCompareResult
import com.filelens.commander.domain.model.FolderEntryDiff
import com.filelens.commander.domain.model.FolderEntryDiffKind
import com.filelens.commander.domain.model.RenamedPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompareFoldersUseCase @Inject constructor(
    private val checksumUseCase: ChecksumUseCase
) {

    suspend fun compare(
        leftPath: String,
        rightPath: String,
        detectRenames: Boolean = true,
        deepVerify: Boolean = false
    ): FolderCompareResult? = withContext(Dispatchers.IO) {
        val left = File(leftPath)
        val right = File(rightPath)
        if (!left.isDirectory || !right.isDirectory) return@withContext null

        val leftEntries = walk(left)
        val rightEntries = walk(right)

        val allPaths = (leftEntries.keys + rightEntries.keys).toSortedSet()
        val diffs = mutableListOf<FolderEntryDiff>()

        for (relPath in allPaths) {
            val l = leftEntries[relPath]
            val r = rightEntries[relPath]
            val isDir = (l?.isDirectory ?: r?.isDirectory) == true
            val initialKind = when {
                l == null -> FolderEntryDiffKind.RIGHT_ONLY
                r == null -> FolderEntryDiffKind.LEFT_ONLY
                isDir -> FolderEntryDiffKind.BOTH_SAME
                l.size != r.size -> FolderEntryDiffKind.BOTH_CHANGED
                // Same size but different mtime — assume changed by default;
                // deep verify can flip it back to SAME if checksums match.
                l.lastModified != r.lastModified -> FolderEntryDiffKind.BOTH_CHANGED
                else -> FolderEntryDiffKind.BOTH_SAME
            }
            val finalKind = if (deepVerify && l != null && r != null && !isDir &&
                l.size == r.size && l.size in 1..MAX_HASH_BYTES
            ) {
                verifyByChecksum(l.file, r.file, initialKind)
            } else {
                initialKind
            }
            diffs += FolderEntryDiff(
                relativePath = relPath,
                isDirectory = isDir,
                kind = finalKind,
                leftSize = l?.size,
                rightSize = r?.size,
                leftModified = l?.lastModified,
                rightModified = r?.lastModified
            )
        }

        val renamed = if (detectRenames) {
            findRenameCandidates(leftEntries, rightEntries)
        } else {
            emptyList()
        }

        FolderCompareResult(
            leftPath = leftPath,
            rightPath = rightPath,
            entries = diffs,
            renamedCandidates = renamed
        )
    }

    /**
     * Used by deep verify: take an initial classification (SAME or CHANGED based on
     * size+mtime) and override it with a byte-equal checksum check. Files that look
     * different by mtime but match by hash are downgraded to SAME, and vice versa.
     */
    private suspend fun verifyByChecksum(
        left: java.io.File,
        right: java.io.File,
        initialKind: FolderEntryDiffKind
    ): FolderEntryDiffKind {
        val leftHash = checksumUseCase.quickSha256(left) ?: return initialKind
        val rightHash = checksumUseCase.quickSha256(right) ?: return initialKind
        return if (leftHash == rightHash) FolderEntryDiffKind.BOTH_SAME else FolderEntryDiffKind.BOTH_CHANGED
    }

    /**
     * Same content under different paths is interesting — moved or renamed files.
     * Only consider files that exist on one side but not the other (those classified
     * as LEFT_ONLY / RIGHT_ONLY), within the size cap to keep checksumming bounded.
     */
    private suspend fun findRenameCandidates(
        leftEntries: Map<String, EntryInfo>,
        rightEntries: Map<String, EntryInfo>
    ): List<RenamedPair> {
        val leftOnly = leftEntries.filter { (k, v) -> !v.isDirectory && k !in rightEntries && v.size in 1..MAX_HASH_BYTES }
        val rightOnly = rightEntries.filter { (k, v) -> !v.isDirectory && k !in leftEntries && v.size in 1..MAX_HASH_BYTES }
        if (leftOnly.isEmpty() || rightOnly.isEmpty()) return emptyList()

        val sizeIndex = rightOnly.entries.groupBy { it.value.size }
        val matches = mutableListOf<RenamedPair>()
        val rightHashCache = mutableMapOf<String, String?>()

        for ((leftRel, leftInfo) in leftOnly) {
            val candidates = sizeIndex[leftInfo.size] ?: continue
            val leftHash = checksumUseCase.quickSha256(leftInfo.file) ?: continue
            for (entry in candidates) {
                val rightRel = entry.key
                val rightInfo = entry.value
                val rightHash = rightHashCache.getOrPut(rightRel) {
                    checksumUseCase.quickSha256(rightInfo.file)
                } ?: continue
                if (rightHash == leftHash) {
                    matches += RenamedPair(
                        leftRelativePath = leftRel,
                        rightRelativePath = rightRel,
                        sharedChecksum = leftHash,
                        size = leftInfo.size
                    )
                    break
                }
            }
        }
        return matches
    }

    private fun walk(root: File): Map<String, EntryInfo> {
        val out = linkedMapOf<String, EntryInfo>()
        val rootPath = root.absolutePath
        val stack = ArrayDeque<File>()
        stack += root
        var processed = 0
        while (stack.isNotEmpty()) {
            val dir = stack.removeFirst()
            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (processed++ > MAX_ENTRIES) return out
                val rel = child.absolutePath.removePrefix(rootPath).trimStart('/')
                out[rel] = EntryInfo(
                    file = child,
                    isDirectory = child.isDirectory,
                    size = if (child.isFile) child.length() else 0L,
                    lastModified = child.lastModified()
                )
                if (child.isDirectory && child.canRead()) stack += child
            }
        }
        return out
    }

    private data class EntryInfo(
        val file: File,
        val isDirectory: Boolean,
        val size: Long,
        val lastModified: Long
    )

    private companion object {
        const val MAX_ENTRIES = 50_000
        const val MAX_HASH_BYTES = 50L * 1024 * 1024
    }
}
