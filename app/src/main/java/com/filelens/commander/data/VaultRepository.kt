package com.filelens.commander.data

import android.content.Context
import android.media.MediaScannerConnection
import com.filelens.commander.domain.model.VaultItem
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Manages the hidden vault: files moved here live under
 * [Context.getNoBackupFilesDir]/vault/ so they sit on app-private
 * storage (no other app can read them, they're skipped by auto-backup
 * and aren't visible to MediaStore). A JSONL index alongside the
 * payload remembers the original location so items can be restored.
 *
 * Concretely *not* an encryption layer — authentication is gated at
 * the UI by [androidx.biometric.BiometricPrompt]; on-disk content is
 * the raw bytes.
 */
@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val vaultDir: File
        get() = File(context.noBackupFilesDir, "vault").apply { if (!exists()) mkdirs() }

    private val indexFile: File
        get() = File(vaultDir, ".index.jsonl")

    private val _items = MutableStateFlow<List<VaultItem>>(emptyList())
    val items: StateFlow<List<VaultItem>> = _items.asStateFlow()

    init {
        // Eagerly load — vault is tiny and the Home tile shows a count.
        _items.value = readAll()
    }

    suspend fun refresh(): List<VaultItem> = withContext(Dispatchers.IO) {
        val snapshot = readAll()
        _items.value = snapshot
        snapshot
    }

    fun count(): Int = _items.value.size

    /**
     * Moves the given files into the vault. Each source is moved (not
     * copied), then MediaStore is asked to forget its old path so the
     * item disappears from gallery apps. Returns the number of items
     * successfully moved.
     */
    suspend fun add(paths: List<String>): Int = withContext(Dispatchers.IO) {
        val dir = vaultDir
        var moved = 0
        val newEntries = mutableListOf<VaultItem>()

        for (path in paths) {
            val source = File(path)
            if (!source.exists() || !source.isFile) continue

            val safeName = source.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val target = File(dir, "${System.currentTimeMillis()}_${System.nanoTime()}_$safeName")

            val ok = try {
                source.renameTo(target) || copyAndDelete(source, target)
            } catch (_: Exception) {
                false
            }

            if (ok) {
                val item = VaultItem(
                    vaultPath = target.absolutePath,
                    originalPath = source.absolutePath,
                    displayName = source.name,
                    size = target.length(),
                    addedAt = System.currentTimeMillis()
                )
                newEntries.add(item)
                moved++
                notifyMediaStoreGone(source.absolutePath)
            }
        }

        if (newEntries.isNotEmpty()) {
            appendIndex(newEntries)
            _items.value = readAll()
        }
        moved
    }

    /**
     * Moves the given vault items back out — to their original location
     * if [targetDir] is null, otherwise to [targetDir]. Returns the
     * number of successfully restored items.
     */
    suspend fun restore(items: List<VaultItem>, targetDir: String? = null): Int = withContext(Dispatchers.IO) {
        var restored = 0
        val keys = mutableSetOf<String>()

        for (item in items) {
            val vaultFile = File(item.vaultPath)
            if (!vaultFile.exists()) {
                keys.add(item.vaultPath)
                continue
            }

            val destDir = targetDir?.let { File(it) }
                ?: File(item.originalPath).parentFile
                ?: continue
            destDir.mkdirs()
            val target = uniqueChild(destDir, item.displayName)

            try {
                val ok = vaultFile.renameTo(target) || copyAndDelete(vaultFile, target)
                if (ok) {
                    restored++
                    keys.add(item.vaultPath)
                    notifyMediaStoreAdded(target.absolutePath)
                }
            } catch (_: Exception) {
            }
        }

        if (keys.isNotEmpty()) {
            rewriteIndex { it.vaultPath !in keys }
            _items.value = readAll()
        }
        restored
    }

    /**
     * Permanently deletes the given vault items.
     */
    suspend fun erase(items: List<VaultItem>): Int = withContext(Dispatchers.IO) {
        var erased = 0
        val keys = mutableSetOf<String>()
        for (item in items) {
            val file = File(item.vaultPath)
            val ok = !file.exists() || file.delete()
            if (ok) {
                erased++
                keys.add(item.vaultPath)
            }
        }
        if (keys.isNotEmpty()) {
            rewriteIndex { it.vaultPath !in keys }
            _items.value = readAll()
        }
        erased
    }

    private fun readAll(): List<VaultItem> {
        val file = indexFile
        if (!file.exists()) return emptyList()
        val items = mutableListOf<VaultItem>()
        val survivors = mutableListOf<VaultItem>()
        try {
            file.useLines { lines ->
                for (line in lines) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    val item = parseLine(trimmed) ?: continue
                    items.add(item)
                    if (File(item.vaultPath).exists()) survivors.add(item)
                }
            }
            if (survivors.size != items.size) writeAll(survivors)
        } catch (_: Exception) {
        }
        return survivors.sortedByDescending { it.addedAt }
    }

    private fun appendIndex(entries: List<VaultItem>) {
        try {
            val sb = StringBuilder()
            for (item in entries) sb.append(serialize(item)).append('\n')
            indexFile.appendText(sb.toString())
        } catch (_: Exception) {
        }
    }

    private fun rewriteIndex(keep: (VaultItem) -> Boolean) {
        val file = indexFile
        if (!file.exists()) return
        val kept = mutableListOf<VaultItem>()
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

    private fun writeAll(items: List<VaultItem>) {
        try {
            val sb = StringBuilder()
            for (item in items) sb.append(serialize(item)).append('\n')
            indexFile.writeText(sb.toString())
        } catch (_: Exception) {
        }
    }

    private fun serialize(item: VaultItem): String {
        val obj = JSONObject()
        obj.put("vaultPath", item.vaultPath)
        obj.put("originalPath", item.originalPath)
        obj.put("displayName", item.displayName)
        obj.put("size", item.size)
        obj.put("addedAt", item.addedAt)
        return obj.toString()
    }

    private fun parseLine(line: String): VaultItem? {
        return try {
            val obj = JSONObject(line)
            VaultItem(
                vaultPath = obj.getString("vaultPath"),
                originalPath = obj.getString("originalPath"),
                displayName = obj.getString("displayName"),
                size = obj.optLong("size", 0L),
                addedAt = obj.getLong("addedAt")
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun uniqueChild(dir: File, name: String): File {
        val candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext = if (dot > 0) name.substring(dot) else ""
        var n = 1
        while (true) {
            val next = File(dir, "$base ($n)$ext")
            if (!next.exists()) return next
            n++
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
            try { if (target.exists()) target.delete() } catch (_: Exception) {}
            false
        }
    }

    private fun notifyMediaStoreGone(path: String) {
        try {
            // Scan-by-path with the file already removed prompts MediaStore
            // to drop the stale row from gallery / picker surfaces.
            MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        } catch (_: Exception) {
        }
    }

    private fun notifyMediaStoreAdded(path: String) {
        try {
            MediaScannerConnection.scanFile(context, arrayOf(path), null, null)
        } catch (_: Exception) {
        }
    }
}
