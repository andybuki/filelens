package com.filelens.commander.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

sealed class FileOperationResult {
    data object Success : FileOperationResult()
    data class Error(val message: String) : FileOperationResult()
}

@Singleton
class FileOperationsUseCase @Inject constructor() {

    suspend fun createFolder(parentPath: String, name: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val dir = File(parentPath, name)
            if (dir.exists()) return@withContext FileOperationResult.Error("Folder already exists")
            if (dir.mkdir()) FileOperationResult.Success
            else FileOperationResult.Error("Failed to create folder")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun rename(path: String, newName: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val file = File(path)
            val target = File(file.parentFile, newName)
            if (target.exists()) return@withContext FileOperationResult.Error("A file with this name already exists")
            if (file.renameTo(target)) FileOperationResult.Success
            else FileOperationResult.Error("Failed to rename")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun delete(paths: List<String>): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            var failed = 0
            for (path in paths) {
                val file = File(path)
                val success = if (file.isDirectory) file.deleteRecursively() else file.delete()
                if (!success) failed++
            }
            if (failed == 0) FileOperationResult.Success
            else FileOperationResult.Error("Failed to delete $failed file(s)")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun copy(sourcePaths: List<String>, targetDir: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val target = File(targetDir)
            if (!target.isDirectory) return@withContext FileOperationResult.Error("Target is not a directory")

            var failed = 0
            for (sourcePath in sourcePaths) {
                val source = File(sourcePath)
                val dest = File(target, source.name)
                try {
                    if (source.isDirectory) {
                        source.copyRecursively(dest, overwrite = false)
                    } else {
                        source.copyTo(dest, overwrite = false)
                    }
                } catch (_: Exception) {
                    failed++
                }
            }
            if (failed == 0) FileOperationResult.Success
            else FileOperationResult.Error("Failed to copy $failed file(s)")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Unknown error")
        }
    }

    suspend fun move(sourcePaths: List<String>, targetDir: String): FileOperationResult = withContext(Dispatchers.IO) {
        try {
            val target = File(targetDir)
            if (!target.isDirectory) return@withContext FileOperationResult.Error("Target is not a directory")

            var failed = 0
            for (sourcePath in sourcePaths) {
                val source = File(sourcePath)
                val dest = File(target, source.name)
                if (!source.renameTo(dest)) {
                    try {
                        if (source.isDirectory) {
                            source.copyRecursively(dest, overwrite = false)
                            source.deleteRecursively()
                        } else {
                            source.copyTo(dest, overwrite = false)
                            source.delete()
                        }
                    } catch (_: Exception) {
                        failed++
                    }
                }
            }
            if (failed == 0) FileOperationResult.Success
            else FileOperationResult.Error("Failed to move $failed file(s)")
        } catch (e: Exception) {
            FileOperationResult.Error(e.message ?: "Unknown error")
        }
    }
}
