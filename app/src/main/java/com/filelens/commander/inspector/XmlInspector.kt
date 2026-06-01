package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class XmlInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.TEXT)

    suspend fun canHandle(file: File): Boolean {
        val ext = file.extension.lowercase()
        if (ext in setOf("xml", "xsl", "xslt", "xsd", "svg", "xhtml", "plist", "csproj", "sln", "vcxproj")) return true
        return try {
            file.reader().use { reader ->
                val buf = CharArray(100)
                val read = reader.read(buf)
                val start = String(buf, 0, read).trimStart()
                start.startsWith("<?xml") || start.startsWith("<!")
            }
        } catch (_: Exception) { false }
    }

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val maxSize = 2 * 1024 * 1024L
        val content = if (file.length() <= maxSize) {
            file.readText(Charsets.UTF_8)
        } else {
            overview["Warning"] = "File truncated for parsing"
            file.reader(Charsets.UTF_8).use { reader ->
                val buf = CharArray(maxSize.toInt())
                val read = reader.read(buf)
                String(buf, 0, read)
            }
        }

        try {
            val stats = XmlStats()
            val tree = parseXml(content, stats)

            overview["Root element"] = stats.rootElement ?: "unknown"
            overview["Elements"] = "%,d".format(stats.elementCount)
            overview["Attributes"] = "%,d".format(stats.attributeCount)
            overview["Text nodes"] = "%,d".format(stats.textCount)
            overview["Max depth"] = stats.maxDepth.toString()
            if (stats.namespaces.isNotEmpty()) {
                overview["Namespaces"] = stats.namespaces.joinToString(", ")
            }
            stats.encoding?.let { overview["Encoding"] = it }
            stats.version?.let { overview["XML version"] = it }

            if (stats.hasDTD) {
                risks.add(RiskItem(
                    severity = RiskSeverity.WARNING,
                    title = "Contains DTD",
                    description = "XML documents with DTD declarations may be vulnerable to XXE attacks if processed by an insecure parser."
                ))
            }

            if (stats.hasExternalEntities) {
                risks.add(RiskItem(
                    severity = RiskSeverity.DANGER,
                    title = "External entities detected",
                    description = "This XML references external entities, which could be used for XXE attacks."
                ))
            }

            val pretty = prettyPrintXml(content)

            InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                structure = tree,
                previewContent = PreviewContent.TextContent(
                    content = pretty.take(100_000),
                    language = "XML",
                    lineCount = pretty.count { it == '\n' } + 1,
                    rawContent = content.take(100_000)
                )
            )
        } catch (e: Exception) {
            overview["Error"] = "Invalid XML: ${e.message?.take(100)}"
            InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                previewContent = PreviewContent.TextContent(
                    content = content.take(10_000),
                    language = "XML",
                    lineCount = content.count { it == '\n' } + 1
                )
            )
        }
    }

    private fun prettyPrintXml(content: String): String {
        return try {
            val sb = StringBuilder(content.length + content.length / 4)
            var depth = 0
            var i = 0
            var lastWasOpen = false
            while (i < content.length) {
                if (content[i] == '<' && i + 1 < content.length) {
                    val isClose = content[i + 1] == '/'
                    val isComment = content.regionMatches(i, "<!--", 0, 4)
                    val isProlog = content[i + 1] == '?' || content[i + 1] == '!'

                    if (isClose) depth = (depth - 1).coerceAtLeast(0)
                    if (sb.isNotEmpty() && (!lastWasOpen || isClose)) {
                        if (sb.last() != '\n') sb.append('\n')
                        sb.append("  ".repeat(depth))
                    }

                    val end = content.indexOf('>', i)
                    if (end == -1) { sb.append(content.substring(i)); break }

                    val tag = content.substring(i, end + 1)
                    sb.append(tag)
                    i = end + 1

                    if (!isClose && !isProlog && !isComment && !tag.endsWith("/>")) depth++
                    lastWasOpen = !isClose && !isProlog && !isComment && !tag.endsWith("/>")
                } else {
                    val nextTag = content.indexOf('<', i)
                    if (nextTag == -1) { sb.append(content.substring(i).trim()); break }
                    val text = content.substring(i, nextTag).trim()
                    if (text.isNotEmpty()) sb.append(text)
                    i = nextTag
                    lastWasOpen = false
                }
            }
            sb.toString()
        } catch (_: Exception) { content }
    }

    private data class XmlStats(
        var rootElement: String? = null,
        var elementCount: Int = 0,
        var attributeCount: Int = 0,
        var textCount: Int = 0,
        var maxDepth: Int = 0,
        var namespaces: MutableSet<String> = mutableSetOf(),
        var encoding: String? = null,
        var version: String? = null,
        var hasDTD: Boolean = false,
        var hasExternalEntities: Boolean = false
    )

    private fun parseXml(content: String, stats: XmlStats): StructureNode? {
        if (content.contains("<!DOCTYPE")) stats.hasDTD = true
        if (content.contains("<!ENTITY") && content.contains("SYSTEM")) stats.hasExternalEntities = true

        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(StringReader(content))

        val stack = mutableListOf<MutableList<StructureNode>>()
        val nameStack = mutableListOf<String>()
        var currentDepth = 0
        var rootNode: StructureNode? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    stats.elementCount++
                    currentDepth++
                    if (currentDepth > stats.maxDepth) stats.maxDepth = currentDepth

                    val name = parser.name
                    if (stats.rootElement == null) stats.rootElement = name

                    val ns = parser.namespace
                    if (!ns.isNullOrEmpty()) stats.namespaces.add(ns)

                    val attrCount = parser.attributeCount
                    stats.attributeCount += attrCount

                    val attrDetails = if (attrCount > 0) {
                        val attrs = (0 until attrCount).map { i ->
                            "${parser.getAttributeName(i)}=\"${parser.getAttributeValue(i).take(50)}\""
                        }
                        " [${attrs.joinToString(", ")}]"
                    } else ""

                    nameStack.add("$name$attrDetails")
                    stack.add(mutableListOf())
                }
                XmlPullParser.END_TAG -> {
                    if (stack.isNotEmpty()) {
                        val children = stack.removeAt(stack.lastIndex)
                        val name = nameStack.removeAt(nameStack.lastIndex)
                        val node = if (children.isEmpty()) {
                            StructureNode.Leaf(name = name)
                        } else {
                            val displayChildren = if (children.size > 50) {
                                children.take(50) + StructureNode.Leaf(name = "... ${children.size - 50} more")
                            } else children
                            StructureNode.Branch(name = name, children = displayChildren)
                        }
                        if (stack.isEmpty()) {
                            rootNode = node
                        } else {
                            stack.last().add(node)
                        }
                    }
                    currentDepth--
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim()
                    if (!text.isNullOrEmpty() && stack.isNotEmpty()) {
                        stats.textCount++
                        stack.last().add(StructureNode.Leaf(
                            name = "text",
                            value = if (text.length > 80) "${text.take(80)}..." else text
                        ))
                    }
                }
            }
            eventType = parser.next()
        }

        return rootNode
    }
}
