package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JsonInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.TEXT)

    suspend fun canHandle(file: File): Boolean = withContext(Dispatchers.IO) {
        file.extension.lowercase() == "json" || run {
            try {
                val start = file.reader().use { it.read(CharArray(1)).let { n -> n > 0 } }
                val firstChar = file.reader().use { val c = CharArray(1); it.read(c); c[0] }
                firstChar == '{' || firstChar == '['
            } catch (_: Exception) { false }
        }
    }

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val maxSize = 2 * 1024 * 1024L
        val content = if (file.length() <= maxSize) {
            file.readText(Charsets.UTF_8)
        } else {
            overview["Warning"] = "File truncated for parsing (${formatSize(file.length())})"
            file.reader(Charsets.UTF_8).use { reader ->
                val buf = CharArray(maxSize.toInt())
                val read = reader.read(buf)
                String(buf, 0, read)
            }
        }

        try {
            val tokener = JSONTokener(content)
            val root = tokener.nextValue()

            val stats = JsonStats()
            analyzeValue(root, 0, stats)

            overview["Root type"] = if (root is JSONObject) "Object" else "Array"
            overview["Total keys"] = "%,d".format(stats.totalKeys)
            overview["Nested objects"] = "%,d".format(stats.objectCount)
            overview["Arrays"] = "%,d".format(stats.arrayCount)
            overview["Max depth"] = stats.maxDepth.toString()
            overview["Strings"] = "%,d".format(stats.stringCount)
            overview["Numbers"] = "%,d".format(stats.numberCount)
            overview["Booleans"] = "%,d".format(stats.boolCount)
            overview["Nulls"] = "%,d".format(stats.nullCount)

            if (stats.possibleSecrets > 0) {
                risks.add(RiskItem(
                    severity = RiskSeverity.DANGER,
                    title = "Possible secrets detected",
                    description = "${stats.possibleSecrets} key(s) may contain secrets (API keys, tokens, passwords)"
                ))
            }

            val tree = buildTree(root, "root", maxChildren = 50, maxDepth = 10)

            val prettyContent = try {
                when (root) {
                    is JSONObject -> root.toString(2)
                    is JSONArray -> root.toString(2)
                    else -> content
                }
            } catch (_: Exception) { content }

            InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                structure = tree,
                previewContent = PreviewContent.TextContent(
                    content = prettyContent.take(100_000),
                    language = "JSON",
                    lineCount = prettyContent.count { it == '\n' } + 1,
                    rawContent = content.take(100_000)
                )
            )
        } catch (e: Exception) {
            overview["Error"] = "Invalid JSON: ${e.message?.take(100)}"
            InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                previewContent = PreviewContent.TextContent(
                    content = content.take(10_000),
                    language = "JSON",
                    lineCount = content.count { it == '\n' } + 1
                )
            )
        }
    }

    private data class JsonStats(
        var totalKeys: Int = 0,
        var objectCount: Int = 0,
        var arrayCount: Int = 0,
        var stringCount: Int = 0,
        var numberCount: Int = 0,
        var boolCount: Int = 0,
        var nullCount: Int = 0,
        var maxDepth: Int = 0,
        var possibleSecrets: Int = 0
    )

    private val secretKeyPatterns = listOf(
        Regex("(?i)(api[_-]?key|apikey)"),
        Regex("(?i)(secret|password|passwd|pwd)"),
        Regex("(?i)(token|auth[_-]?token|access[_-]?token)"),
        Regex("(?i)(private[_-]?key)"),
        Regex("(?i)(aws[_-]?access|aws[_-]?secret)"),
        Regex("(?i)(database[_-]?url|connection[_-]?string)")
    )

    private fun analyzeValue(value: Any?, depth: Int, stats: JsonStats) {
        if (depth > stats.maxDepth) stats.maxDepth = depth

        when (value) {
            is JSONObject -> {
                stats.objectCount++
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    stats.totalKeys++
                    if (secretKeyPatterns.any { it.containsMatchIn(key) }) {
                        val v = value.opt(key)
                        if (v is String && v.length >= 8) {
                            stats.possibleSecrets++
                        }
                    }
                    analyzeValue(value.opt(key), depth + 1, stats)
                }
            }
            is JSONArray -> {
                stats.arrayCount++
                for (i in 0 until value.length()) {
                    analyzeValue(value.opt(i), depth + 1, stats)
                }
            }
            is String -> stats.stringCount++
            is Number -> stats.numberCount++
            is Boolean -> stats.boolCount++
            JSONObject.NULL, null -> stats.nullCount++
        }
    }

    private fun buildTree(value: Any?, name: String, maxChildren: Int, maxDepth: Int, depth: Int = 0): StructureNode {
        if (depth >= maxDepth) {
            return StructureNode.Leaf(name = name, value = "...")
        }

        return when (value) {
            is JSONObject -> {
                val children = mutableListOf<StructureNode>()
                val keys = value.keys().asSequence().toList()
                val displayKeys = if (keys.size > maxChildren) {
                    keys.take(maxChildren)
                } else keys

                for (key in displayKeys) {
                    children.add(buildTree(value.opt(key), key, maxChildren, maxDepth, depth + 1))
                }
                if (keys.size > maxChildren) {
                    children.add(StructureNode.Leaf(name = "... ${keys.size - maxChildren} more keys"))
                }
                StructureNode.Branch(name = "$name {${keys.size}}", children = children)
            }
            is JSONArray -> {
                val children = mutableListOf<StructureNode>()
                val count = value.length()
                val displayCount = minOf(count, maxChildren)

                for (i in 0 until displayCount) {
                    children.add(buildTree(value.opt(i), "[$i]", maxChildren, maxDepth, depth + 1))
                }
                if (count > maxChildren) {
                    children.add(StructureNode.Leaf(name = "... ${count - maxChildren} more items"))
                }
                StructureNode.Branch(name = "$name [${count}]", children = children)
            }
            is String -> StructureNode.Leaf(name = name, value = if (value.length > 100) "\"${value.take(100)}...\"" else "\"$value\"")
            is Number -> StructureNode.Leaf(name = name, value = value.toString())
            is Boolean -> StructureNode.Leaf(name = name, value = value.toString())
            JSONObject.NULL, null -> StructureNode.Leaf(name = name, value = "null")
            else -> StructureNode.Leaf(name = name, value = value.toString())
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        return "%.1f MB".format(mb)
    }
}
