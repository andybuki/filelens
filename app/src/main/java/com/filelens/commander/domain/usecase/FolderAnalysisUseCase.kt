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
        val largest: List<FileEntry>,
        val newest: List<FileEntry>,
        val unknown: List<FileEntry>,
        val duplicateCandidates: List<List<FileEntry>>
    )

    suspend fun analyze(folderPath: String): FolderSummary = withContext(Dispatchers.IO) {
        val folder = File(folderPath)
        if (!folder.isDirectory) {
            return@withContext FolderSummary(folderPath, 0, 0, emptyMap(), emptyList(), emptyList(), emptyList(), emptyList())
        }
        val files = folder.listFiles()?.filter { it.isFile } ?: emptyList()
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
            largest = largest,
            newest = newest,
            unknown = unknown,
            duplicateCandidates = duplicates
        )
    }
}
