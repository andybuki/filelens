package com.filelens.commander.domain.usecase

import android.content.Context
import com.filelens.commander.domain.model.InspectorInfo
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.RiskSeverity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportExportUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    enum class Format { MARKDOWN, JSON }

    suspend fun export(
        info: InspectorInfo,
        result: InspectorResult,
        detectedType: String? = null,
        format: Format
    ): File = withContext(Dispatchers.IO) {
        val safeName = info.name.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
        val ext = if (format == Format.MARKDOWN) "md" else "json"
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val out = File(context.cacheDir, "filelens_report_${safeName}_$timestamp.$ext")
        val content = when (format) {
            Format.MARKDOWN -> renderMarkdown(info, result, detectedType)
            Format.JSON -> renderJson(info, result, detectedType)
        }
        out.writeText(content)
        out
    }

    fun renderMarkdown(info: InspectorInfo, result: InspectorResult, detectedType: String?): String {
        val sb = StringBuilder()
        sb.appendLine("# FileLens report")
        sb.appendLine()
        sb.appendLine("## Overview")
        sb.appendLine("- **File**: ${info.name}")
        sb.appendLine("- **Path**: `${info.path}`")
        sb.appendLine("- **Type**: ${info.category.name}")
        info.extension.takeIf { it.isNotEmpty() }?.let { sb.appendLine("- **Extension**: .$it") }
        info.size?.let { sb.appendLine("- **Size**: $it bytes") }
        info.mimeType?.let { sb.appendLine("- **MIME**: $it") }
        detectedType?.let { sb.appendLine("- **Detected type**: $it") }

        result.trustScore?.let { score ->
            sb.appendLine()
            sb.appendLine("## Trust score")
            sb.appendLine("- **Level**: ${score.level.displayName}")
            sb.appendLine("- **Score**: ${score.numericScore}/100")
            sb.appendLine("- **Top reasons**:")
            score.topReasons.forEach { sb.appendLine("  - $it") }
        }

        result.provenance?.let { p ->
            sb.appendLine()
            sb.appendLine("## Provenance")
            sb.appendLine("- **Source**: ${p.displayName}")
            sb.appendLine("- **Confidence**: ${p.confidence.name}")
            sb.appendLine("- ${p.description}")
        }

        if (result.overview.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("## Inspector overview")
            result.overview.forEach { (k, v) -> sb.appendLine("- **$k**: $v") }
        }

        if (result.metadata.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("## Metadata")
            result.metadata.forEach { (k, v) -> sb.appendLine("- **$k**: $v") }
        }

        if (result.risks.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("## Risks (${result.risks.size})")
            result.risks.forEach { r ->
                sb.appendLine()
                sb.appendLine("### [${r.severity.name}] ${r.title}")
                sb.appendLine(r.description)
                r.whyItMatters?.let { sb.appendLine().appendLine("**Why it matters:** $it") }
                r.recommendedAction?.let { sb.appendLine().appendLine("**Recommended action:** $it") }
                if (r.evidence.isNotEmpty()) {
                    sb.appendLine()
                    sb.appendLine("Evidence:")
                    r.evidence.forEach { (k, v) -> sb.appendLine("- $k: `$v`") }
                }
                r.detector?.let { sb.appendLine().appendLine("_Detector: ${it}_") }
            }
        }

        if (result.secrets.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("## Possible secrets (${result.secrets.size})")
            sb.appendLine("Values are redacted.")
            result.secrets.take(50).forEach { s ->
                val line = s.lineNumber?.let { " (line $it)" } ?: ""
                sb.appendLine("- **${s.kind}**: `${s.redactedSample}`$line")
            }
        }

        return sb.toString()
    }

    fun renderJson(info: InspectorInfo, result: InspectorResult, detectedType: String?): String {
        val root = JSONObject()
        root.put("file", JSONObject().apply {
            put("name", info.name)
            put("path", info.path)
            put("category", info.category.name)
            put("extension", info.extension)
            put("size", info.size)
            put("mimeType", info.mimeType)
            put("detectedType", detectedType)
        })

        result.trustScore?.let { score ->
            root.put("trustScore", JSONObject().apply {
                put("level", score.level.name)
                put("score", score.numericScore)
                put("topReasons", JSONArray(score.topReasons))
            })
        }

        result.provenance?.let { p ->
            root.put("provenance", JSONObject().apply {
                put("source", p.source.name)
                put("displayName", p.displayName)
                put("confidence", p.confidence.name)
                put("description", p.description)
            })
        }

        root.put("overview", JSONObject(result.overview))
        root.put("metadata", JSONObject(result.metadata))

        root.put("risks", JSONArray().apply {
            result.risks.forEach { r ->
                put(JSONObject().apply {
                    put("id", r.id)
                    put("severity", r.severity.name)
                    put("category", r.category.name)
                    put("title", r.title)
                    put("description", r.description)
                    put("whyItMatters", r.whyItMatters)
                    put("recommendedAction", r.recommendedAction)
                    put("detector", r.detector)
                    put("evidence", JSONObject(r.evidence))
                })
            }
        })

        root.put("secrets", JSONArray().apply {
            result.secrets.forEach { s ->
                put(JSONObject().apply {
                    put("kind", s.kind)
                    put("redactedSample", s.redactedSample)
                    put("location", s.location)
                    put("lineNumber", s.lineNumber)
                })
            }
        })

        return root.toString(2)
    }

    suspend fun exportBatch(
        folderPath: String,
        results: List<BatchScanUseCase.ScanResult>,
        format: Format
    ): File = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val ext = if (format == Format.MARKDOWN) "md" else "json"
        val out = File(context.cacheDir, "filelens_batch_$timestamp.$ext")
        val content = when (format) {
            Format.MARKDOWN -> renderBatchMarkdown(folderPath, results)
            Format.JSON -> renderBatchJson(folderPath, results)
        }
        out.writeText(content)
        out
    }

    private fun renderBatchMarkdown(folderPath: String, results: List<BatchScanUseCase.ScanResult>): String {
        val sb = StringBuilder()
        sb.appendLine("# FileLens batch report")
        sb.appendLine("**Folder:** `$folderPath`")
        sb.appendLine("**Files scanned:** ${results.size}")
        sb.appendLine()
        val risky = results.filter { it.bucket == BatchScanUseCase.Bucket.RISKY }
        val safe = results.filter { it.bucket == BatchScanUseCase.Bucket.SAFE }
        val unsupported = results.filter { it.bucket == BatchScanUseCase.Bucket.UNSUPPORTED }
        sb.appendLine("- Risky: ${risky.size}")
        sb.appendLine("- Safe: ${safe.size}")
        sb.appendLine("- Unsupported: ${unsupported.size}")

        if (risky.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("## Risky files")
            risky.forEach { r ->
                sb.appendLine()
                sb.appendLine("### ${r.name}")
                sb.appendLine("`${r.path}`")
                r.risks.forEach { risk ->
                    sb.appendLine("- **[${risk.severity.name}]** ${risk.title} — ${risk.description}")
                }
            }
        }
        return sb.toString()
    }

    private fun renderBatchJson(folderPath: String, results: List<BatchScanUseCase.ScanResult>): String {
        val root = JSONObject()
        root.put("folder", folderPath)
        root.put("totalScanned", results.size)
        root.put("files", JSONArray().apply {
            results.forEach { r ->
                put(JSONObject().apply {
                    put("path", r.path)
                    put("name", r.name)
                    put("bucket", r.bucket.name)
                    put("risks", JSONArray().apply {
                        r.risks.forEach { risk ->
                            put(JSONObject().apply {
                                put("id", risk.id)
                                put("severity", risk.severity.name)
                                put("title", risk.title)
                                put("description", risk.description)
                            })
                        }
                    })
                })
            }
        })
        return root.toString(2)
    }
}
