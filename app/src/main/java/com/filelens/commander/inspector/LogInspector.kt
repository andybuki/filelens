package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskCategory
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.TEXT)

    fun handles(file: File): Boolean {
        val ext = file.extension.lowercase()
        if (ext in setOf("log", "logs")) return true
        val name = file.name.lowercase()
        if (name.endsWith(".log.gz") || name.endsWith(".log.1") || name.endsWith(".log.2")) return true
        if (name == "logcat.txt") return true
        return false
    }

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val maxBytes = 4 * 1024 * 1024L
        val truncated = file.length() > maxBytes
        val content = if (!truncated) {
            file.readText()
        } else {
            overview["Note"] = "Log truncated for analysis (${formatSize(file.length())})"
            file.reader().use { reader ->
                val buf = CharArray(maxBytes.toInt())
                val n = reader.read(buf)
                String(buf, 0, n)
            }
        }

        val lines = content.lines()
        overview["Lines"] = "%,d".format(lines.size)

        val levelCounts = linkedMapOf(
            "FATAL" to 0,
            "ERROR" to 0,
            "WARN" to 0,
            "INFO" to 0,
            "DEBUG" to 0,
            "TRACE" to 0
        )
        val levelRegex = Regex("\\b(FATAL|ERROR|SEVERE|WARN(?:ING)?|INFO|DEBUG|TRACE|VERBOSE)\\b")

        val httpStatus = linkedMapOf<String, Int>()
        val httpRegex = Regex("\\b(?:HTTP/\\d\\.\\d\\s*|status[\"']?\\s*[:=]\\s*)(\\d{3})\\b")

        val exceptions = mutableMapOf<String, Int>()
        val exceptionRegex = Regex("\\b([A-Z][A-Za-z0-9_]*(?:Exception|Error|Throwable))\\b")

        val errorLines = mutableListOf<String>()
        val timeRegex = Regex("\\b(\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,6})?)\\b")
        val timestamps = mutableListOf<String>()

        for (line in lines) {
            val match = levelRegex.find(line)
            if (match != null) {
                val raw = match.value.uppercase()
                val key = when (raw) {
                    "SEVERE" -> "ERROR"
                    "WARNING" -> "WARN"
                    "VERBOSE" -> "TRACE"
                    else -> raw
                }
                levelCounts[key] = (levelCounts[key] ?: 0) + 1
                if (key == "ERROR" || key == "FATAL") {
                    if (errorLines.size < 200) errorLines += line
                }
            }
            for (h in httpRegex.findAll(line)) {
                val status = h.groupValues[1]
                httpStatus[status] = (httpStatus[status] ?: 0) + 1
            }
            for (e in exceptionRegex.findAll(line)) {
                val name = e.value
                exceptions[name] = (exceptions[name] ?: 0) + 1
            }
            timeRegex.find(line)?.let { timestamps += it.groupValues[1] }
        }

        levelCounts.filterValues { it > 0 }.forEach { (lvl, cnt) ->
            overview["$lvl lines"] = "%,d".format(cnt)
        }

        if (timestamps.isNotEmpty()) {
            metadata["First timestamp"] = timestamps.first()
            metadata["Last timestamp"] = timestamps.last()
            metadata["Timestamps found"] = "%,d".format(timestamps.size)
        }

        val server5xx = httpStatus.filterKeys { it.startsWith("5") }.values.sum()
        val client4xx = httpStatus.filterKeys { it.startsWith("4") }.values.sum()
        if (httpStatus.isNotEmpty()) {
            overview["HTTP status codes"] = httpStatus.entries.sortedByDescending { it.value }.take(6).joinToString(", ") { "${it.key}×${it.value}" }
        }

        val topErrors = errorLines
            .map { normaliseErrorLine(it) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(5)

        val structureChildren = mutableListOf<StructureNode>()
        if (topErrors.isNotEmpty()) {
            structureChildren += StructureNode.Branch(
                name = "Top errors",
                children = topErrors.map { (msg, cnt) ->
                    StructureNode.Leaf(name = msg, value = "×$cnt")
                },
                details = "${topErrors.size} groups"
            )
        }
        if (exceptions.isNotEmpty()) {
            val exList = exceptions.entries.sortedByDescending { it.value }.take(10)
            structureChildren += StructureNode.Branch(
                name = "Exceptions",
                children = exList.map { StructureNode.Leaf(name = it.key, value = "×${it.value}") },
                details = "${exceptions.size} types"
            )
        }
        if (httpStatus.isNotEmpty()) {
            structureChildren += StructureNode.Branch(
                name = "HTTP status",
                children = httpStatus.entries.sortedBy { it.key }.map {
                    StructureNode.Leaf(name = it.key, value = "×${it.value}")
                }
            )
        }

        val structure = if (structureChildren.isNotEmpty()) {
            StructureNode.Branch(name = "Log summary", children = structureChildren)
        } else null

        val errorCount = (levelCounts["ERROR"] ?: 0) + (levelCounts["FATAL"] ?: 0)
        if (errorCount > 0) {
            risks.add(RiskItem(
                id = "log-errors",
                severity = if (errorCount >= 10) RiskSeverity.WARNING else RiskSeverity.INFO,
                category = RiskCategory.INTEGRITY,
                title = "$errorCount error line${if (errorCount > 1) "s" else ""} in log",
                description = topErrors.take(3).joinToString("\n") { "• ${it.key} (×${it.value})" }.ifEmpty { "Errors recorded." },
                whyItMatters = "Recurring errors usually point to a real bug, a misconfigured service, or a security event worth investigating.",
                recommendedAction = "Group similar errors and check whether they cluster around a specific time or request.",
                evidence = mapOf("Errors" to errorCount.toString(), "Fatals" to (levelCounts["FATAL"] ?: 0).toString()),
                detector = "LogInspector"
            ))
        }
        if (server5xx >= 5) {
            risks.add(RiskItem(
                id = "log-http-5xx",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.INTEGRITY,
                title = "Server errors (5xx) in log",
                description = "$server5xx requests returned a 5xx status.",
                whyItMatters = "5xx clusters point to a service failure on your side and may indicate a broken deploy, dependency outage, or attack.",
                recommendedAction = "Find the time window of the failures and correlate with recent deploys or external traffic.",
                detector = "LogInspector"
            ))
        }
        if (client4xx >= 50) {
            risks.add(RiskItem(
                id = "log-http-4xx",
                severity = RiskSeverity.INFO,
                category = RiskCategory.INTEGRITY,
                title = "Many client errors (4xx) in log",
                description = "$client4xx requests returned a 4xx status.",
                whyItMatters = "Large 4xx volume can indicate a misbehaving client, broken integration, or scanning activity.",
                recommendedAction = "Group by route and source IP if your log includes them.",
                detector = "LogInspector"
            ))
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            structure = structure,
            previewContent = PreviewContent.TextContent(
                content = content.take(200_000),
                language = "Log",
                lineCount = lines.size
            )
        )
    }

    private fun normaliseErrorLine(line: String): String {
        var s = line
        s = Regex("\\b\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,6})?(?:Z|[+-]\\d{2}:?\\d{2})?\\b").replace(s, "<time>")
        s = Regex("\\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\b").replace(s, "<uuid>")
        s = Regex("\\b\\d{6,}\\b").replace(s, "<num>")
        s = Regex("\\s+").replace(s.trim(), " ")
        return s.take(200)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.1f GB".format(mb / 1024.0)
    }
}
