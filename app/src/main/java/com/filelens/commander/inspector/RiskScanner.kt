package com.filelens.commander.inspector

import com.filelens.commander.detector.MagicBytesDetector
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.Risks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RiskScanner @Inject constructor(
    private val magicBytesDetector: MagicBytesDetector
) {
    suspend fun scanFile(file: File): List<RiskItem> = withContext(Dispatchers.IO) {
        val risks = mutableListOf<RiskItem>()
        if (!file.exists() || !file.canRead()) return@withContext risks

        val detection = magicBytesDetector.detect(file)

        if (detection.extensionMismatch) {
            risks.add(Risks.extensionMismatch(detection.declaredExtension, detection.detectedType))
        }

        when (detection.category) {
            FileCategory.IMAGE -> scanImage(file, risks)
            FileCategory.ARCHIVE -> scanArchive(file, risks)
            FileCategory.APK -> scanArchive(file, risks)
            FileCategory.DOCUMENT, FileCategory.SPREADSHEET, FileCategory.PRESENTATION -> scanOfficeDocument(file, risks)
            FileCategory.TEXT, FileCategory.CODE -> scanTextFile(file, risks)
            else -> {}
        }

        risks
    }

    suspend fun scanFolder(folder: File): Map<String, List<RiskItem>> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, List<RiskItem>>()
        if (!folder.isDirectory) return@withContext results

        folder.listFiles()?.forEach { file ->
            if (file.isFile) {
                val risks = scanFile(file)
                if (risks.isNotEmpty()) {
                    results[file.absolutePath] = risks
                }
            }
        }

        results
    }

    private fun scanImage(file: File, risks: MutableList<RiskItem>) {
        try {
            val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
            val latLong = exif.latLong
            if (latLong != null) {
                risks.add(Risks.exifGps(latLong[0], latLong[1]))
            }
            exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_ARTIST)?.let {
                risks.add(Risks.exifAuthor(it))
            }
        } catch (_: Exception) {}
    }

    private fun scanArchive(file: File, risks: MutableList<RiskItem>) {
        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()
            val execExts = setOf("exe", "dll", "bat", "cmd", "sh", "ps1", "msi", "com", "scr", "vbs", "js")
            val execs = entries.filter { e -> e.name.substringAfterLast('.', "").lowercase() in execExts }
            if (execs.isNotEmpty()) {
                risks.add(Risks.zipExecutables(execs.size))
            }

            val totalUncompressed = entries.sumOf { it.size.coerceAtLeast(0) }
            if (totalUncompressed > file.length() * 100 && file.length() > 1024) {
                risks.add(Risks.zipBomb(totalUncompressed / file.length()))
            }
            zipFile.close()
        } catch (_: Exception) {}
    }

    private fun scanOfficeDocument(file: File, risks: MutableList<RiskItem>) {
        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()

            if (entries.any { it.name.contains("vba", ignoreCase = true) && it.name.endsWith(".bin") }) {
                risks.add(Risks.officeMacros())
            }

            val coreXml = zipFile.getEntry("docProps/core.xml")?.let {
                zipFile.getInputStream(it).bufferedReader().readText()
            }
            if (coreXml != null) {
                val authorPattern = Regex("<dc:creator>([^<]+)</dc:creator>")
                authorPattern.find(coreXml)?.groupValues?.get(1)?.let { author ->
                    risks.add(RiskItem(
                        id = "office-author",
                        severity = RiskSeverity.INFO,
                        category = com.filelens.commander.domain.model.RiskCategory.PRIVACY,
                        title = "Document contains author metadata",
                        description = "Author: $author",
                        whyItMatters = "The dc:creator field identifies the original author and travels with the file.",
                        recommendedAction = "Strip core properties before sharing.",
                        evidence = mapOf("Author" to author),
                        detector = "RiskScanner"
                    ))
                }
            }

            val docXml = zipFile.getEntry("word/document.xml")?.let {
                zipFile.getInputStream(it).bufferedReader().readText()
            }
            if (docXml != null) {
                if (docXml.contains("<w:ins ") || docXml.contains("<w:del ")) {
                    risks.add(Risks.officeTrackedChanges())
                }
            }

            if (entries.any { it.name == "word/comments.xml" } ||
                entries.any { it.name.startsWith("ppt/comments/") }) {
                risks.add(Risks.officeComments())
            }

            val workbook = zipFile.getEntry("xl/workbook.xml")?.let {
                zipFile.getInputStream(it).bufferedReader().readText()
            }
            if (workbook != null && workbook.contains("state=\"hidden\"")) {
                val hiddenSheets = Regex("""state="hidden".*?name="([^"]+)"""")
                    .findAll(workbook)
                    .map { it.groupValues[1] }
                    .toList()
                risks.add(Risks.officeHiddenSheets(hiddenSheets.ifEmpty { listOf("(unknown)") }))
            }

            zipFile.close()
        } catch (_: Exception) {}
    }

    private fun scanTextFile(file: File, risks: MutableList<RiskItem>) {
        try {
            val maxRead = 64 * 1024L
            val content = if (file.length() <= maxRead) {
                file.readText()
            } else {
                file.reader().use { r ->
                    val buf = CharArray(maxRead.toInt())
                    val n = r.read(buf)
                    String(buf, 0, n)
                }
            }

            val secretPatterns = listOf(
                Regex("(?i)(api[_-]?key|apikey)\\s*[:=]\\s*[\"']?[\\w\\-]{16,}") to "API key",
                Regex("(?i)(password|passwd|pwd)\\s*[:=]\\s*[\"']?[^\\s\"']{8,}") to "Password",
                Regex("(?i)(secret|token|auth)\\s*[:=]\\s*[\"']?[\\w\\-\\.]{16,}") to "Secret/Token",
                Regex("-----BEGIN\\s+(RSA\\s+)?PRIVATE\\s+KEY-----") to "Private key"
            )

            val found = mutableSetOf<String>()
            for ((pattern, label) in secretPatterns) {
                if (pattern.containsMatchIn(content) && found.add(label)) {
                    risks.add(Risks.secretsFound(count = 1, kinds = listOf(label)))
                }
            }
        } catch (_: Exception) {}
    }
}
