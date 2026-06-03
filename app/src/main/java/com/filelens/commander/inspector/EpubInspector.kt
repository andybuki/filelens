package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EPUB is a ZIP with `META-INF/container.xml` pointing to a `.opf` manifest.
 * The OPF carries Dublin Core metadata (title, author, language, ...) and a
 * manifest/spine that tells us the chapter list.
 */
@Singleton
class EpubInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.DOCUMENT, FileCategory.ARCHIVE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()

        try {
            ZipFile(file).use { zip ->
                overview["Format"] = "EPUB"
                val container = zip.getEntry("META-INF/container.xml")?.let { entry ->
                    zip.getInputStream(entry).bufferedReader().readText()
                }
                val opfPath = container?.let { CONTAINER_OPF.find(it)?.groupValues?.getOrNull(1) }
                val opf = opfPath?.let { path ->
                    zip.getEntry(path)?.let { e -> zip.getInputStream(e).bufferedReader().readText() }
                }

                if (opf != null) {
                    extractDcTag(opf, "title")?.let { overview["Title"] = it }
                    extractDcTag(opf, "creator")?.let { overview["Author"] = it }
                    extractDcTag(opf, "publisher")?.let { metadata["Publisher"] = it }
                    extractDcTag(opf, "language")?.let { metadata["Language"] = it }
                    extractDcTag(opf, "date")?.let { metadata["Date"] = it }
                    extractDcTag(opf, "identifier")?.let { metadata["Identifier"] = it }
                    extractDcTag(opf, "description")?.let { metadata["Description"] = it.take(300) }
                    extractDcTag(opf, "rights")?.let { metadata["Rights"] = it }

                    val spineCount = SPINE_ITEMREF.findAll(opf).count()
                    if (spineCount > 0) overview["Chapters"] = spineCount.toString()

                    val manifestCount = MANIFEST_ITEM.findAll(opf).count()
                    if (manifestCount > 0) overview["Manifest items"] = manifestCount.toString()
                } else {
                    overview["Note"] = "Couldn't read container.xml or OPF manifest"
                }

                val entries = zip.entries().toList()
                overview["Entries"] = entries.size.toString()
                val htmlCount = entries.count { it.name.endsWith(".xhtml", true) || it.name.endsWith(".html", true) }
                if (htmlCount > 0) overview["HTML files"] = htmlCount.toString()
                val imageCount = entries.count { it.name.matches(IMAGE_NAME) }
                if (imageCount > 0) overview["Images"] = imageCount.toString()
                val fontCount = entries.count { it.name.matches(FONT_NAME) }
                if (fontCount > 0) overview["Fonts"] = fontCount.toString()
            }
        } catch (e: Exception) {
            overview["Error"] = e.message ?: "Failed to parse EPUB"
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            structure = StructureNode.Leaf(name = file.name, value = "EPUB"),
        )
    }

    private fun extractDcTag(opf: String, tag: String): String? {
        val regex = Regex("""<dc:$tag(?:\s[^>]*)?>([^<]+)</dc:$tag>""", RegexOption.IGNORE_CASE)
        return regex.find(opf)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        val CONTAINER_OPF = Regex("""rootfile[^>]*full-path="([^"]+)"""", RegexOption.IGNORE_CASE)
        val SPINE_ITEMREF = Regex("""<itemref\b""", RegexOption.IGNORE_CASE)
        val MANIFEST_ITEM = Regex("""<item\b""", RegexOption.IGNORE_CASE)
        val IMAGE_NAME = Regex(""".*\.(jpe?g|png|gif|webp|svg)""", RegexOption.IGNORE_CASE)
        val FONT_NAME = Regex(""".*\.(ttf|otf|woff2?)""", RegexOption.IGNORE_CASE)
    }
}
