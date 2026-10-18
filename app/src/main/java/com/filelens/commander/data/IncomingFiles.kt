package com.filelens.commander.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.IntentCompat
import java.io.File

/**
 * Copies files handed over by other apps (Share / Open with) into app-private
 * cache so the path-based inspectors can work on them without storage access.
 */
object IncomingFiles {

    private const val STALE_AFTER_MS = 24L * 60 * 60 * 1000

    /** The URIs an ACTION_VIEW / ACTION_SEND / ACTION_SEND_MULTIPLE intent carries. */
    fun urisFrom(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        Intent.ACTION_SEND -> listOfNotNull(
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        )
        Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(
            intent, Intent.EXTRA_STREAM, Uri::class.java
        ).orEmpty()
        else -> emptyList()
    }.filter { it.scheme == "content" }

    /**
     * Copies [uri] into `cache/<subdir>/<unique>/<display name>`. Each copy gets
     * its own folder so two shares of "IMG_0001.jpg" never collide. Blocking.
     */
    fun copyToCache(context: Context, uri: Uri, subdir: String): File {
        // Our own FileProvider can read anywhere in the sandbox; never let a
        // caller point us back at it.
        require(uri.authority?.startsWith(context.packageName) != true) { "Refusing own provider URI" }
        val root = File(context.cacheDir, subdir)
        pruneStale(root)
        val dir = File(root, System.nanoTime().toString()).apply { mkdirs() }
        val target = File(dir, safeName(displayName(context, uri)))
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open $uri")
        input.use { src -> target.outputStream().use { src.copyTo(it) } }
        return target
    }

    private fun displayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull() ?: uri.lastPathSegment
    }

    internal fun safeName(raw: String?): String {
        val base = raw.orEmpty().substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[\\u0000-\\u001f:*?\"<>|]"), "_")
            .trim().trimStart('.')
            .take(120)
        return base.ifEmpty { "shared_file" }
    }

    private fun pruneStale(root: File) {
        val cutoff = System.currentTimeMillis() - STALE_AFTER_MS
        root.listFiles()?.forEach { if (it.lastModified() < cutoff) it.deleteRecursively() }
    }
}
