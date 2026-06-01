package com.filelens.commander.domain.usecase

import android.database.sqlite.SQLiteDatabase
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.SearchHit
import com.filelens.commander.domain.model.SearchHitKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StructuredSearchUseCase @Inject constructor() {

    suspend fun search(
        file: File,
        query: String,
        category: FileCategory
    ): List<SearchHit> = withContext(Dispatchers.IO) {
        if (query.isBlank() || !file.exists() || !file.canRead()) return@withContext emptyList()

        val needle = query.lowercase()
        val out = mutableListOf<SearchHit>()
        val maxHits = 300

        try {
            when (category) {
                FileCategory.ARCHIVE, FileCategory.APK,
                FileCategory.DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> {
                    searchZip(file, needle, out, maxHits, category)
                }
                FileCategory.DATABASE -> {
                    searchSqlite(file, needle, out, maxHits)
                }
                FileCategory.TEXT, FileCategory.CODE -> {
                    val ext = file.extension.lowercase()
                    when (ext) {
                        "json" -> searchJson(file, needle, out, maxHits)
                        "xml", "xsl", "xslt", "xsd", "svg", "xhtml", "plist" -> searchXml(file, needle, out, maxHits)
                        "csv", "tsv" -> searchCsv(file, needle, out, maxHits, sep = if (ext == "tsv") '\t' else ',')
                        else -> searchText(file, needle, out, maxHits)
                    }
                }
                else -> searchText(file, needle, out, maxHits)
            }
        } catch (_: Exception) {
            // best effort
        }

        out
    }

    private fun searchZip(
        file: File,
        needle: String,
        out: MutableList<SearchHit>,
        max: Int,
        category: FileCategory
    ) {
        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence().toList()

            for (entry in entries) {
                if (out.size >= max) return
                if (entry.name.lowercase().contains(needle)) {
                    out += SearchHit(
                        kind = SearchHitKind.ENTRY_NAME,
                        location = entry.name,
                        snippet = entry.name,
                        size = entry.size
                    )
                }
            }

            val textyExts = setOf("txt", "md", "xml", "json", "yaml", "yml", "toml", "ini",
                "properties", "env", "conf", "cfg", "kt", "java", "py", "js", "ts", "rs", "go",
                "html", "css", "rels")

            for (entry in entries) {
                if (out.size >= max) break
                if (entry.isDirectory) continue
                if (entry.size > 2 * 1024 * 1024L) continue
                val name = entry.name.lowercase()
                val ext = name.substringAfterLast('.', "")
                val isInspectable = ext in textyExts ||
                    name == "manifest.mf" ||
                    name.startsWith("word/") ||
                    name.startsWith("xl/") ||
                    name.startsWith("ppt/") ||
                    name.startsWith("docprops/") ||
                    name == "androidmanifest.xml"
                if (!isInspectable) continue
                try {
                    val text = zip.getInputStream(entry).bufferedReader().use { it.readText() }
                    text.lineSequence().forEachIndexed { idx, line ->
                        if (out.size >= max) return
                        if (line.lowercase().contains(needle)) {
                            out += SearchHit(
                                kind = SearchHitKind.ENTRY_CONTENT,
                                location = "${entry.name}:${idx + 1}",
                                snippet = line.trim().take(180),
                                lineNumber = idx + 1
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun searchJson(file: File, needle: String, out: MutableList<SearchHit>, max: Int) {
        val text = file.readText()
        try {
            val root = JSONTokener(text).nextValue()
            walkJson(root, "$", needle, out, max)
        } catch (_: Exception) {
            searchText(file, needle, out, max)
        }
    }

    private fun walkJson(value: Any?, path: String, needle: String, out: MutableList<SearchHit>, max: Int) {
        if (out.size >= max) return
        when (value) {
            is JSONObject -> {
                val keys = value.keys()
                while (keys.hasNext()) {
                    if (out.size >= max) return
                    val key = keys.next()
                    if (key.lowercase().contains(needle)) {
                        out += SearchHit(
                            kind = SearchHitKind.JSON_KEY,
                            location = "$path.$key",
                            snippet = "$key: ${value.opt(key).toString().take(120)}"
                        )
                    }
                    walkJson(value.opt(key), "$path.$key", needle, out, max)
                }
            }
            is JSONArray -> {
                for (i in 0 until value.length()) {
                    walkJson(value.opt(i), "$path[$i]", needle, out, max)
                }
            }
            is String -> {
                if (value.lowercase().contains(needle)) {
                    out += SearchHit(
                        kind = SearchHitKind.JSON_VALUE,
                        location = path,
                        snippet = value.take(180)
                    )
                }
            }
            else -> {}
        }
    }

    private fun searchXml(file: File, needle: String, out: MutableList<SearchHit>, max: Int) {
        val text = file.readText()
        val tagRegex = Regex("<([A-Za-z_][^\\s/>]*)\\b")
        val attrRegex = Regex("([A-Za-z_][\\w:-]*)\\s*=\\s*\"([^\"]*)\"")
        var line = 0
        for (rawLine in text.lineSequence()) {
            if (out.size >= max) return
            line++
            for (m in tagRegex.findAll(rawLine)) {
                if (m.groupValues[1].lowercase().contains(needle)) {
                    out += SearchHit(
                        kind = SearchHitKind.XML_TAG,
                        location = "tag <${m.groupValues[1]}> line $line",
                        snippet = rawLine.trim().take(180),
                        lineNumber = line
                    )
                }
            }
            for (m in attrRegex.findAll(rawLine)) {
                val k = m.groupValues[1]
                val v = m.groupValues[2]
                if (k.lowercase().contains(needle) || v.lowercase().contains(needle)) {
                    out += SearchHit(
                        kind = SearchHitKind.XML_ATTR,
                        location = "@$k line $line",
                        snippet = "$k=\"${v.take(120)}\"",
                        lineNumber = line
                    )
                }
            }
            // text content fallback
            val stripped = rawLine.replace(Regex("<[^>]+>"), " ")
            if (stripped.lowercase().contains(needle) && stripped.isNotBlank()) {
                out += SearchHit(
                    kind = SearchHitKind.XML_TEXT,
                    location = "text line $line",
                    snippet = stripped.trim().take(180),
                    lineNumber = line
                )
            }
        }
    }

    private fun searchCsv(file: File, needle: String, out: MutableList<SearchHit>, max: Int, sep: Char) {
        file.bufferedReader().useLines { lines ->
            lines.forEachIndexed { idx, line ->
                if (out.size >= max) return
                val cells = line.split(sep)
                cells.forEachIndexed { col, cell ->
                    if (cell.lowercase().contains(needle)) {
                        out += SearchHit(
                            kind = SearchHitKind.CSV_CELL,
                            location = "row ${idx + 1}, col ${col + 1}",
                            snippet = cell.trim().take(180),
                            lineNumber = idx + 1
                        )
                    }
                }
            }
        }
    }

    private fun searchText(file: File, needle: String, out: MutableList<SearchHit>, max: Int) {
        file.bufferedReader().useLines { lines ->
            lines.forEachIndexed { idx, line ->
                if (out.size >= max) return
                if (line.lowercase().contains(needle)) {
                    out += SearchHit(
                        kind = SearchHitKind.LINE,
                        location = "line ${idx + 1}",
                        snippet = line.trim().take(180),
                        lineNumber = idx + 1
                    )
                }
            }
        }
    }

    private fun searchSqlite(file: File, needle: String, out: MutableList<SearchHit>, max: Int) {
        try {
            SQLiteDatabase.openDatabase(
                file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS
            ).use { db ->
                db.rawQuery("SELECT name, type, sql FROM sqlite_master", null).use { c ->
                    while (c.moveToNext()) {
                        if (out.size >= max) return
                        val name = c.getString(0) ?: continue
                        val type = c.getString(1) ?: ""
                        val sql = c.getString(2) ?: ""
                        if (name.lowercase().contains(needle)) {
                            out += SearchHit(
                                kind = SearchHitKind.SQLITE_OBJECT,
                                location = "$type $name",
                                snippet = sql.take(180)
                            )
                        } else if (sql.lowercase().contains(needle)) {
                            out += SearchHit(
                                kind = SearchHitKind.SQLITE_OBJECT,
                                location = "$type $name (in DDL)",
                                snippet = sql.take(180)
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }
}
