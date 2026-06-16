package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.classifyByExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderAnalysisUseCase @Inject constructor() {

    data class FileEntry(
        val name: String,
        val path: String,
        val size: Long,
        val lastModified: Long,
        val category: FileCategory
    )

    data class FolderSummary(
        val folderPath: String,
        val fileCount: Int,
        val totalSize: Long,
        val categoryCounts: Map<FileCategory, Int>,
        val categoryBytes: Map<FileCategory, Long>,
        val largest: List<FileEntry>,
        val newest: List<FileEntry>,
        val unknown: List<FileEntry>,
        val duplicateCandidates: List<List<FileEntry>>
    )

    suspend fun analyze(folderPath: String, recursive: Boolean = false): FolderSummary = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        if (!folder.isDirectory) {
            return@withContext FolderSummary(folderPath, 0, 0, emptyMap(), emptyMap(), emptyList(), emptyList(), emptyList(), emptyList())
        }
        val files = if (recursive) {
            // Bounded walk so a deeply-nested tree can't stall the analyzer.
            // 8 levels is enough for the standard external-storage layout
            // (Pictures/Camera/MyEvent/... etc.) without blowing up.
            val collected = mutableListOf<File>()
            collectFiles(folder, collected, maxDepth = 8)
            collected
        } else {
            folder.listFiles()?.filter { it.isFile } ?: emptyList()
        }
        val entries = files.map { f ->
            FileEntry(
                name = f.name,
                path = f.absolutePath,
                size = f.length(),
                lastModified = f.lastModified(),
                category = classifyByExtension(f.extension)
            )
        }
        val totalSize = entries.sumOf { it.size }
        val categoryCounts = entries.groupingBy { it.category }.eachCount()
        val categoryBytes = entries.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.size } }
        val largest = entries.sortedByDescending { it.size }.take(10)
        val newest = entries.sortedByDescending { it.lastModified }.take(10)
        val unknown = entries.filter { it.category == FileCategory.UNKNOWN }
        val duplicates = entries
            .groupBy { it.size }
            .filterValues { it.size > 1 && it.first().size > 0 }
            .values
            .toList()

        FolderSummary(
            folderPath = folderPath,
            fileCount = entries.size,
            totalSize = totalSize,
            categoryCounts = categoryCounts,
            categoryBytes = categoryBytes,
            largest = largest,
            newest = newest,
            unknown = unknown,
            duplicateCandidates = duplicates
        )
    }

    private fun collectFiles(dir: File, out: MutableList<File>, maxDepth: Int, depth: Int = 0) {
        if (depth > maxDepth) return
        val children = dir.listFiles() ?: return
        for (f in children) {
            // Skip our own trash so freshly-deleted files don't get re-counted
            // against the user — they should appear in Recycle bin, not here.
            if (f.isDirectory && f.name == ".trash") continue
            when {
                f.isFile -> out.add(f)
                f.isDirectory && f.canRead() -> collectFiles(f, out, maxDepth, depth + 1)
            }
        }
    }
}
