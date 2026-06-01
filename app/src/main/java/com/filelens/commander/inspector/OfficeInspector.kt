package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.Risks
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfficeInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(
        FileCategory.DOCUMENT,
        FileCategory.SPREADSHEET,
        FileCategory.PRESENTATION
    )

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val ext = file.extension.lowercase()
        when (ext) {
            "docx" -> inspectDocx(file)
            "xlsx" -> inspectXlsx(file)
            "pptx" -> inspectPptx(file)
            else -> inspectGenericOoxml(file)
        }
    }

    private fun inspectDocx(file: File): InspectorResult {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()

            extractCoreProperties(zipFile, metadata)

            val documentXml = zipFile.getEntry("word/document.xml")?.let { entry ->
                zipFile.getInputStream(entry).bufferedReader().readText()
            }

            if (documentXml != null) {
                val paragraphs = Regex("<w:p[ >]").findAll(documentXml).count()
                val tables = Regex("<w:tbl[ >]").findAll(documentXml).count()
                val images = Regex("<w:drawing[ >]").findAll(documentXml).count()
                val hyperlinks = Regex("<w:hyperlink[ >]").findAll(documentXml).count()
                val revisions = Regex("<w:(ins|del)[ >]").findAll(documentXml).count()
                val sectPr = Regex("<w:sectPr[ >]").findAll(documentXml).count()

                overview["Paragraphs"] = "%,d".format(paragraphs)
                if (tables > 0) overview["Tables"] = tables.toString()
                if (images > 0) overview["Images"] = images.toString()
                if (hyperlinks > 0) overview["Hyperlinks"] = hyperlinks.toString()
                if (revisions > 0) overview["Revision marks"] = revisions.toString()
                if (sectPr > 1) overview["Sections"] = sectPr.toString()
            }

            // External image / hyperlink links via document.xml.rels
            val relsXml = zipFile.getEntry("word/_rels/document.xml.rels")?.let { entry ->
                zipFile.getInputStream(entry).bufferedReader().readText()
            }
            if (relsXml != null) {
                val externalLinks = Regex("""TargetMode="External"\s+Target="([^"]+)"""").findAll(relsXml)
                    .map { it.groupValues[1] }
                    .toList()
                if (externalLinks.isNotEmpty()) {
                    overview["External links"] = externalLinks.size.toString()
                    metadata["External targets"] = externalLinks.take(5).joinToString("\n")
                    risks.add(Risks.officeExternalLinks())
                }
            }

            // Custom properties
            val customXml = zipFile.getEntry("docProps/custom.xml")?.let { entry ->
                zipFile.getInputStream(entry).bufferedReader().readText()
            }
            if (customXml != null) {
                val customCount = Regex("<property[ >]").findAll(customXml).count()
                if (customCount > 0) metadata["Custom properties"] = customCount.toString()
            }

            // Template path
            val appXml = zipFile.getEntry("docProps/app.xml")?.let { entry ->
                zipFile.getInputStream(entry).bufferedReader().readText()
            }
            if (appXml != null) {
                Regex("<Template>([^<]+)</Template>").find(appXml)?.groupValues?.get(1)?.let {
                    if (it.isNotBlank() && it != "Normal.dotm") metadata["Template"] = it
                }
            }

            val hasComments = entries.any { it.name == "word/comments.xml" }
            val hasTrackedChanges = documentXml?.contains("<w:ins ") == true || documentXml?.contains("<w:del ") == true
            val hasEmbedded = entries.any { it.name.startsWith("word/embeddings/") }
            val hasMacros = entries.any { it.name.endsWith(".bin") && it.name.contains("vba", ignoreCase = true) }
            val hasMedia = entries.filter { it.name.startsWith("word/media/") }

            if (hasComments) {
                overview["Comments"] = "Present"
                risks.add(Risks.officeComments())
            }
            if (hasTrackedChanges) {
                overview["Tracked changes"] = "Present"
                risks.add(Risks.officeTrackedChanges())
            }
            if (hasEmbedded) {
                overview["Embedded objects"] = entries.count { it.name.startsWith("word/embeddings/") }.toString()
                risks.add(Risks.officeEmbeddedObjects())
            }
            if (hasMacros) {
                risks.add(Risks.officeMacros())
            }
            if (hasMedia.isNotEmpty()) {
                overview["Media files"] = hasMedia.size.toString()
            }

            val tree = buildOoxmlTree(entries)
            zipFile.close()

            return InspectorResult(overview = overview, metadata = metadata, risks = risks, structure = tree)
        } catch (e: Exception) {
            overview["Error"] = "Failed to read DOCX: ${e.message?.take(100)}"
            return InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun inspectXlsx(file: File): InspectorResult {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()

            extractCoreProperties(zipFile, metadata)

            val workbookXml = zipFile.getEntry("xl/workbook.xml")?.let { entry ->
                zipFile.getInputStream(entry).bufferedReader().readText()
            }

            if (workbookXml != null) {
                val sheetNames = Regex("""name="([^"]+)"""").findAll(workbookXml).map { it.groupValues[1] }.toList()
                overview["Sheets"] = sheetNames.size.toString()
                metadata["Sheet names"] = sheetNames.joinToString(", ")

                val hiddenSheets = Regex("""state="hidden".*?name="([^"]+)"""").findAll(workbookXml).map { it.groupValues[1] }.toList()
                if (hiddenSheets.isNotEmpty()) {
                    overview["Hidden sheets"] = hiddenSheets.size.toString()
                    risks.add(Risks.officeHiddenSheets(hiddenSheets))
                }
                val veryHidden = Regex("""state="veryHidden".*?name="([^"]+)"""").findAll(workbookXml).map { it.groupValues[1] }.toList()
                if (veryHidden.isNotEmpty()) {
                    overview["Very hidden sheets"] = veryHidden.size.toString()
                    risks.add(RiskItem(
                        id = "office-very-hidden-sheets",
                        severity = RiskSeverity.WARNING,
                        category = com.filelens.commander.domain.model.RiskCategory.PRIVACY,
                        title = "Very hidden sheets",
                        description = veryHidden.joinToString(", "),
                        whyItMatters = "Very hidden sheets are only revealed via VBA. They are often used to hide sensitive data or hidden helper data.",
                        recommendedAction = "Inspect every very hidden sheet through the VBA editor or by unhiding programmatically before sharing.",
                        evidence = mapOf("Very hidden" to veryHidden.joinToString(", ")),
                        detector = "OfficeInspector"
                    ))
                }
                val definedNames = Regex("<definedName[ >]").findAll(workbookXml).count()
                if (definedNames > 0) overview["Defined names"] = definedNames.toString()
            }

            val sharedStrings = zipFile.getEntry("xl/sharedStrings.xml")?.let { entry ->
                val content = zipFile.getInputStream(entry).bufferedReader().readText()
                val count = Regex("""count="(\d+)"""").find(content)?.groupValues?.get(1)?.toIntOrNull()
                count
            }
            sharedStrings?.let { overview["Shared strings"] = "%,d".format(it) }

            val sheetEntries = entries.filter { it.name.startsWith("xl/worksheets/sheet") && it.name.endsWith(".xml") }
            var totalRows = 0
            var totalFormulas = 0
            val suspiciousFormulas = mutableListOf<String>()
            val suspiciousFns = listOf("WEBSERVICE", "HYPERLINK", "DDE", "CALL", "EXEC", "FILTERXML")
            for (sheetEntry in sheetEntries.take(20)) {
                val sheetContent = zipFile.getInputStream(sheetEntry).bufferedReader().readText()
                val rows = Regex("<row ").findAll(sheetContent).count()
                totalRows += rows
                totalFormulas += Regex("<f[ >]").findAll(sheetContent).count()
                for (fn in suspiciousFns) {
                    if (sheetContent.contains(fn, ignoreCase = true)) {
                        suspiciousFormulas += fn
                    }
                }
            }
            if (totalRows > 0) overview["Data rows"] = "%,d".format(totalRows)
            if (totalFormulas > 0) overview["Formulas"] = "%,d".format(totalFormulas)
            val uniqueSuspicious = suspiciousFormulas.distinct()
            if (uniqueSuspicious.isNotEmpty()) {
                risks.add(RiskItem(
                    id = "xlsx-suspicious-formulas",
                    severity = RiskSeverity.WARNING,
                    category = com.filelens.commander.domain.model.RiskCategory.ACTIVE_CONTENT,
                    title = "Suspicious spreadsheet formulas",
                    description = "Functions found: ${uniqueSuspicious.joinToString(", ")}",
                    whyItMatters = "WEBSERVICE, HYPERLINK, DDE, and CALL can make outbound requests or execute commands when the workbook opens.",
                    recommendedAction = "Open in protected view and inspect the formulas before allowing live evaluation.",
                    evidence = mapOf("Functions" to uniqueSuspicious.joinToString(", ")),
                    detector = "OfficeInspector"
                ))
            }

            val hasMacros = entries.any { it.name.endsWith(".bin") && it.name.contains("vba", ignoreCase = true) }
            val hasExternalLinks = entries.any { it.name.startsWith("xl/externalLinks/") }

            if (hasMacros) {
                risks.add(Risks.officeMacros())
            }
            if (hasExternalLinks) {
                risks.add(Risks.officeExternalLinks())
            }

            val tree = buildOoxmlTree(entries)
            zipFile.close()

            return InspectorResult(overview = overview, metadata = metadata, risks = risks, structure = tree)
        } catch (e: Exception) {
            overview["Error"] = "Failed to read XLSX: ${e.message?.take(100)}"
            return InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun inspectPptx(file: File): InspectorResult {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()

            extractCoreProperties(zipFile, metadata)

            val slides = entries.filter { it.name.startsWith("ppt/slides/slide") && it.name.endsWith(".xml") }
            overview["Slides"] = slides.size.toString()

            val media = entries.filter { it.name.startsWith("ppt/media/") }
            if (media.isNotEmpty()) {
                overview["Media files"] = media.size.toString()
                val totalMediaSize = media.sumOf { it.size.coerceAtLeast(0) }
                overview["Media size"] = formatSize(totalMediaSize)
            }

            // Hidden slides
            var hiddenSlides = 0
            for (slide in slides.take(200)) {
                val slideXml = zipFile.getInputStream(slide).bufferedReader().readText()
                if (Regex("<p:sld\\b[^>]*\\bshow=\"0\"").containsMatchIn(slideXml)) {
                    hiddenSlides++
                }
            }
            if (hiddenSlides > 0) {
                overview["Hidden slides"] = hiddenSlides.toString()
                risks.add(RiskItem(
                    id = "pptx-hidden-slides",
                    severity = com.filelens.commander.domain.model.RiskSeverity.WARNING,
                    category = com.filelens.commander.domain.model.RiskCategory.PRIVACY,
                    title = "Hidden slides",
                    description = "$hiddenSlides slide(s) marked hidden.",
                    whyItMatters = "Hidden slides ship with the file and may contain content the presenter intended to remove.",
                    recommendedAction = "Unhide and review hidden slides before sharing the deck.",
                    evidence = mapOf("Count" to hiddenSlides.toString()),
                    detector = "OfficeInspector"
                ))
            }

            // External media via slide rels
            val slideRels = entries.filter { it.name.startsWith("ppt/slides/_rels/") && it.name.endsWith(".rels") }
            val externalMedia = mutableListOf<String>()
            for (rel in slideRels.take(200)) {
                val relXml = zipFile.getInputStream(rel).bufferedReader().readText()
                Regex("""TargetMode="External"\s+Target="([^"]+)"""")
                    .findAll(relXml)
                    .forEach { externalMedia += it.groupValues[1] }
            }
            if (externalMedia.isNotEmpty()) {
                overview["External media links"] = externalMedia.size.toString()
                metadata["External media targets"] = externalMedia.distinct().take(5).joinToString("\n")
                risks.add(Risks.officeExternalLinks())
            }

            val hasNotes = entries.any { it.name.startsWith("ppt/notesSlides/") }
            if (hasNotes) {
                overview["Speaker notes"] = "Present"
                risks.add(Risks.officeSpeakerNotes())
            }

            val hasComments = entries.any { it.name.startsWith("ppt/comments/") }
            if (hasComments) {
                risks.add(Risks.officeComments())
            }

            val hasMacros = entries.any { it.name.endsWith(".bin") && it.name.contains("vba", ignoreCase = true) }
            if (hasMacros) {
                risks.add(Risks.officeMacros())
            }

            val tree = buildOoxmlTree(entries)
            zipFile.close()

            return InspectorResult(overview = overview, metadata = metadata, risks = risks, structure = tree)
        } catch (e: Exception) {
            overview["Error"] = "Failed to read PPTX: ${e.message?.take(100)}"
            return InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun inspectGenericOoxml(file: File): InspectorResult {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        try {
            val zipFile = ZipFile(file)
            val entries = zipFile.entries().toList()
            extractCoreProperties(zipFile, metadata)
            overview["Files"] = entries.count { !it.isDirectory }.toString()
            val tree = buildOoxmlTree(entries)
            zipFile.close()
            return InspectorResult(overview = overview, metadata = metadata, risks = risks, structure = tree)
        } catch (e: Exception) {
            overview["Error"] = e.message?.take(100) ?: "Failed to read"
            return InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun extractCoreProperties(zipFile: ZipFile, metadata: MutableMap<String, String>) {
        val coreEntry = zipFile.getEntry("docProps/core.xml") ?: return
        try {
            val content = zipFile.getInputStream(coreEntry).bufferedReader().readText()
            extractXmlValue(content, "dc:title")?.let { metadata["Title"] = it }
            extractXmlValue(content, "dc:creator")?.let { metadata["Author"] = it }
            extractXmlValue(content, "cp:lastModifiedBy")?.let { metadata["Last modified by"] = it }
            extractXmlValue(content, "dcterms:created")?.let { metadata["Created"] = it }
            extractXmlValue(content, "dcterms:modified")?.let { metadata["Modified"] = it }
            extractXmlValue(content, "cp:revision")?.let { metadata["Revision"] = it }
            extractXmlValue(content, "dc:description")?.let { metadata["Description"] = it }
            extractXmlValue(content, "dc:subject")?.let { metadata["Subject"] = it }
            extractXmlValue(content, "cp:keywords")?.let { metadata["Keywords"] = it }
        } catch (_: Exception) {}

        val appEntry = zipFile.getEntry("docProps/app.xml") ?: return
        try {
            val content = zipFile.getInputStream(appEntry).bufferedReader().readText()
            extractXmlValue(content, "Application")?.let { metadata["Application"] = it }
            extractXmlValue(content, "AppVersion")?.let { metadata["App version"] = it }
            extractXmlValue(content, "Company")?.let { metadata["Company"] = it }
            extractXmlValue(content, "Pages")?.let { metadata["Pages"] = it }
            extractXmlValue(content, "Words")?.let { metadata["Words"] = it }
            extractXmlValue(content, "Characters")?.let { metadata["Characters"] = it }
        } catch (_: Exception) {}
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val pattern = Regex("""<$tag[^>]*>([^<]+)</$tag>""")
        return pattern.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun buildOoxmlTree(entries: List<java.util.zip.ZipEntry>): StructureNode {
        data class TreeNode(val name: String, val children: MutableMap<String, TreeNode> = mutableMapOf(), var size: Long? = null)

        val root = TreeNode("root")
        for (entry in entries) {
            val parts = entry.name.trimEnd('/').split("/").filter { it.isNotEmpty() }
            var current = root
            for ((i, part) in parts.withIndex()) {
                current = current.children.getOrPut(part) { TreeNode(part) }
                if (i == parts.lastIndex && !entry.isDirectory) {
                    current.size = entry.size
                }
            }
        }

        fun toNode(node: TreeNode): StructureNode {
            return if (node.children.isEmpty()) {
                StructureNode.Leaf(name = node.name, value = node.size?.let { formatSize(it) }, size = node.size)
            } else {
                StructureNode.Branch(
                    name = node.name,
                    children = node.children.values.sortedBy { it.name }.map { toNode(it) }
                )
            }
        }

        return StructureNode.Branch(
            name = "Document structure",
            children = root.children.values.sortedBy { it.name }.map { toNode(it) }
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 0) return "?"
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        return "%.1f MB".format(mb)
    }
}
