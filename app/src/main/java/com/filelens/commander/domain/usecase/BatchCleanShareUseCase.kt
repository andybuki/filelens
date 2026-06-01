package com.filelens.commander.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatchCleanShareUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cleanCopyUseCase: CleanCopyUseCase
) {

    data class BatchCleanResult(
        val zipPath: String?,
        val cleanedCount: Int,
        val skippedCount: Int,
        val errorMessage: String? = null
    )

    private val cleanableExtensions = setOf(
        "jpg", "jpeg", "png", "webp", "tiff", "tif",
        "docx", "xlsx", "pptx"
    )

    suspend fun cleanAndPackage(paths: List<String>): BatchCleanResult = withContext(Dispatchers.IO) {
        if (paths.isEmpty()) {
            return@withContext BatchCleanResult(null, 0, 0, "Nothing to clean")
        }

        val workDir = File(context.cacheDir, "batch_clean_${System.currentTimeMillis()}").apply { mkdirs() }
        val cleanedFiles = mutableListOf<File>()
        var skipped = 0

        try {
            for (raw in paths) {
                val src = File(raw)
                if (!src.exists() || !src.canRead()) {
                    skipped++
                    continue
                }
                val ext = src.extension.lowercase()
                if (ext !in cleanableExtensions) {
                    skipped++
                    continue
                }
                val result = cleanCopyUseCase.createCleanedCopy(src.absolutePath, workDir.absolutePath)
                if (result is CleanCopyUseCase.CleanResult.Success) {
                    cleanedFiles += File(result.cleanPath)
                } else {
                    skipped++
                }
            }

            if (cleanedFiles.isEmpty()) {
                return@withContext BatchCleanResult(null, 0, skipped, "No supported files to clean")
            }

            val zipFile = File(context.cacheDir, "filelens_clean_${System.currentTimeMillis()}.zip")
            ZipOutputStream(FileOutputStream(zipFile)).use { zout ->
                for (file in cleanedFiles) {
                    zout.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { it.copyTo(zout) }
                    zout.closeEntry()
                }
            }

            BatchCleanResult(
                zipPath = zipFile.absolutePath,
                cleanedCount = cleanedFiles.size,
                skippedCount = skipped
            )
        } catch (e: Exception) {
            BatchCleanResult(null, cleanedFiles.size, skipped, e.message ?: "Failed to package cleaned copies")
        } finally {
            // Leave workDir for the FileProvider to read; cleanup is best-effort on next session.
        }
    }
}
