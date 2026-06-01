package com.filelens.commander.domain.usecase

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrashUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val trashDir: File
        get() = File(context.filesDir, ".trash").apply { if (!exists()) mkdirs() }

    private val indexFile: File
        get() = File(trashDir, "index.jsonl")

    data class TrashedItem(
        val originalPath: String,
        val trashPath: String,
        val name: String,
        val timestamp: Long
    )

    /**
     * Moves the given files into the trash directory, giving each a unique
     * timestamped name. Returns the list of successfully trashed items.
     */
    suspend fun moveToTrash(paths: List<String>): List<TrashedItem> = withContext(Dispatchers.IO) {
        val dir = trashDir
        val moved = mutableListOf<TrashedItem>()

        for (path in paths) {
            val source = File(path)
            if (!source.exists()) continue

            val timestamp = System.currentTimeMillis()
            val safeName = source.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val trashName = "${timestamp}_${System.nanoTime()}_$safeName"
            val target = File(dir, trashName)

            val ok = try {
                source.renameTo(target) || copyAndDelete(source, target)
            } catch (_: Exception) {
                false
            }

            if (ok) {
                val item = TrashedItem(
                    originalPath = source.absolutePath,
                    trashPath = target.absolutePath,
                    name = source.name,
                    timestamp = timestamp
                )
                appendIndex(item)
                moved.add(item)
            }
        }

        moved
    }

    /**
     * Restores the given trashed items back to their original locations. Returns
     * the number of items successfully restored.
     */
    suspend fun restore(items: List<TrashedItem>): Int = withContext(Dispatchers.IO) {
        var restored = 0
        val keys = items.map { it.trashPath }.toSet()

        for (item in items) {
            val trashFile = File(item.trashPath)
            val target = File(item.originalPath)

            if (!trashFile.exists()) continue

            try {
                target.parentFile?.mkdirs()
                val ok = trashFile.renameTo(target) || copyAndDelete(trashFile, target)
                if (ok) restored++
            } catch (_: Exception) {
            }
        }

        // Rewrite the index, dropping any entry whose trashPath was restored
        // regardless of whether the restore succeeded for that specific entry
        // (the source file is gone either way).
        rewriteIndex { entry -> entry.trashPath !in keys }

        restored
    }

    /**
     * Empties the trash directory entirely.
     */
    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val dir = trashDir
        dir.listFiles()?.forEach { file ->
            try {
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Returns the contents of the trash index. Entries whose underlying file
     * no longer exists are filtered out (and removed from the index).
     */
    suspend fun listTrashed(): List<TrashedItem> = withContext(Dispatchers.IO) {
        val file = indexFile
        if (!file.exists()) return@withContext emptyList()

        val items = mutableListOf<TrashedItem>()
        val survivors = mutableListOf<TrashedItem>()

        file.useLines { lines ->
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) continue
                val item = parseLine(trimmed) ?: continue
                items.add(item)
                if (File(item.trashPath).exists()) survivors.add(item)
            }
        }

        if (survivors.size != items.size) {
            writeAll(survivors)
        }

        survivors
    }

    private fun appendIndex(item: TrashedItem) {
        try {
            indexFile.appendText(serialize(item) + "\n")
        } catch (_: Exception) {
        }
    }

    private fun rewriteIndex(keep: (TrashedItem) -> Boolean) {
        val file = indexFile
        if (!file.exists()) return
        val kept = mutableListOf<TrashedItem>()
        try {
            file.useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val item = parseLine(trimmed) ?: continue
                    if (keep(item)) kept.add(item)
                }
            }
            writeAll(kept)
        } catch (_: Exception) {
        }
    }

    private fun writeAll(items: List<TrashedItem>) {
        try {
            val sb = StringBuilder()
            for (item in items) sb.append(serialize(item)).append('\n')
            indexFile.writeText(sb.toString())
        } catch (_: Exception) {
        }
    }

    private fun serialize(item: TrashedItem): String {
        val obj = JSONObject()
        obj.put("originalPath", item.originalPath)
        obj.put("trashPath", item.trashPath)
        obj.put("name", item.name)
        obj.put("timestamp", item.timestamp)
        return obj.toString()
    }

    private fun parseLine(line: String): TrashedItem? {
        return try {
            val obj = JSONObject(line)
            TrashedItem(
                originalPath = obj.getString("originalPath"),
                trashPath = obj.getString("trashPath"),
                name = obj.getString("name"),
                timestamp = obj.getLong("timestamp")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun copyAndDelete(source: File, target: File): Boolean {
        return try {
            source.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            source.delete()
            true
        } catch (_: Exception) {
            try {
                if (target.exists()) target.delete()
            } catch (_: Exception) {
            }
            false
        }
    }
}
