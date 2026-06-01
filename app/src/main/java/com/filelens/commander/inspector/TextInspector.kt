package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.Risks
import com.filelens.commander.domain.model.SecretMatch
import com.filelens.commander.domain.model.StructureNode
import com.filelens.commander.domain.usecase.SecretScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextInspector @Inject constructor(
    private val secretScanner: SecretScanner
) : FileInspector {

    override val supportedCategories = setOf(FileCategory.TEXT, FileCategory.CODE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val maxPreviewBytes = 64 * 1024L
        val bytes = if (file.length() <= maxPreviewBytes) {
            file.readBytes()
        } else {
            file.inputStream().use { stream ->
                val buf = ByteArray(maxPreviewBytes.toInt())
                val read = stream.read(buf)
                buf.copyOf(read)
            }
        }

        val encoding = detectEncoding(bytes)
        overview["Encoding"] = encoding

        val content = when (encoding) {
            "UTF-8 with BOM" -> String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
            "UTF-16 BE" -> String(bytes, Charsets.UTF_16BE)
            "UTF-16 LE" -> String(bytes, Charsets.UTF_16LE)
            else -> String(bytes, Charsets.UTF_8)
        }

        val lines = content.lines()
        val lineCount = lines.size
        val wordCount = content.split(Regex("\\s+")).count { it.isNotEmpty() }
        val charCount = content.length

        overview["Lines"] = "%,d".format(lineCount)
        overview["Words"] = "%,d".format(wordCount)
        overview["Characters"] = "%,d".format(charCount)

        if (file.length() > maxPreviewBytes) {
            overview["Note"] = "Preview truncated (file is ${formatSize(file.length())})"
        }

        val ext = file.extension.lowercase()
        val language = detectLanguage(ext)
        language?.let { overview["Language"] = it }

        val outline = extractOutline(content, ext)

        val secrets = secretScanner.scan(content, location = file.name)
        if (secrets.isNotEmpty()) {
            risks.add(Risks.secretsFound(secrets.size, secrets.map { it.kind }.distinct()))
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            structure = outline,
            previewContent = PreviewContent.TextContent(
                content = content,
                language = language,
                encoding = encoding,
                lineCount = lineCount
            ),
            secrets = secrets
        )
    }

    private fun detectEncoding(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return "UTF-8 with BOM"
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return "UTF-16 BE"
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return "UTF-16 LE"
        }
        return "UTF-8"
    }

    private fun detectLanguage(ext: String): String? = when (ext) {
        "kt", "kts" -> "Kotlin"
        "java" -> "Java"
        "py" -> "Python"
        "js" -> "JavaScript"
        "ts" -> "TypeScript"
        "jsx" -> "JSX"
        "tsx" -> "TSX"
        "c" -> "C"
        "cpp", "cc", "cxx" -> "C++"
        "h", "hpp" -> "C/C++ Header"
        "rs" -> "Rust"
        "go" -> "Go"
        "rb" -> "Ruby"
        "php" -> "PHP"
        "swift" -> "Swift"
        "scala" -> "Scala"
        "groovy" -> "Groovy"
        "dart" -> "Dart"
        "r" -> "R"
        "lua" -> "Lua"
        "pl", "pm" -> "Perl"
        "ex", "exs" -> "Elixir"
        "sh", "bash" -> "Shell"
        "zsh" -> "Zsh"
        "fish" -> "Fish"
        "ps1" -> "PowerShell"
        "bat", "cmd" -> "Batch"
        "html", "htm" -> "HTML"
        "css" -> "CSS"
        "scss" -> "SCSS"
        "less" -> "Less"
        "sass" -> "Sass"
        "sql" -> "SQL"
        "graphql", "gql" -> "GraphQL"
        "proto" -> "Protocol Buffers"
        "json" -> "JSON"
        "xml" -> "XML"
        "yaml", "yml" -> "YAML"
        "toml" -> "TOML"
        "ini", "cfg" -> "INI"
        "md", "markdown" -> "Markdown"
        "txt" -> "Plain text"
        "log" -> "Log"
        "csv" -> "CSV"
        "tsv" -> "TSV"
        "properties" -> "Properties"
        "env" -> "Environment"
        "conf", "config" -> "Config"
        else -> null
    }

    private fun extractOutline(content: String, ext: String): StructureNode? {
        val items = mutableListOf<StructureNode>()

        when (ext) {
            "kt", "kts" -> {
                val patterns = listOf(
                    Regex("""^\s*(class|object|interface|enum\s+class|sealed\s+class|data\s+class|abstract\s+class)\s+(\w+)""", RegexOption.MULTILINE),
                    Regex("""^\s*(fun)\s+(\w+)""", RegexOption.MULTILINE),
                    Regex("""^\s*(val|var)\s+(\w+)""", RegexOption.MULTILINE)
                )
                patterns.forEach { pattern ->
                    pattern.findAll(content).forEach { match ->
                        items.add(StructureNode.Leaf(
                            name = "${match.groupValues[1]} ${match.groupValues[2]}",
                            value = "line ${content.substring(0, match.range.first).count { it == '\n' } + 1}"
                        ))
                    }
                }
            }
            "java" -> {
                val classPattern = Regex("""^\s*(public\s+|private\s+|protected\s+)?(abstract\s+|static\s+|final\s+)*(class|interface|enum)\s+(\w+)""", RegexOption.MULTILINE)
                val methodPattern = Regex("""^\s*(public|private|protected)\s+[\w<>\[\],\s]+\s+(\w+)\s*\(""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[3]} ${match.groupValues[4]}"))
                }
                methodPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "method ${match.groupValues[2]}"))
                }
            }
            "py" -> {
                val classPattern = Regex("""^class\s+(\w+)""", RegexOption.MULTILINE)
                val funcPattern = Regex("""^def\s+(\w+)""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "class ${match.groupValues[1]}"))
                }
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "def ${match.groupValues[1]}"))
                }
            }
            "js", "ts", "jsx", "tsx" -> {
                val classPattern = Regex("""^\s*(export\s+)?(default\s+)?class\s+(\w+)""", RegexOption.MULTILINE)
                val funcPattern = Regex("""^\s*(export\s+)?(default\s+)?(async\s+)?function\s+(\w+)""", RegexOption.MULTILINE)
                val arrowPattern = Regex("""^\s*(export\s+)?(const|let|var)\s+(\w+)\s*=\s*(async\s+)?\(""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "class ${match.groupValues[3]}"))
                }
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "function ${match.groupValues[4]}"))
                }
                arrowPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "const ${match.groupValues[3]}"))
                }
            }
            "rs" -> {
                val patterns = listOf(
                    Regex("""^\s*pub\s+(struct|enum|trait|fn|impl|mod|type)\s+(\w+)""", RegexOption.MULTILINE),
                    Regex("""^\s*(struct|enum|trait|fn|impl|mod|type)\s+(\w+)""", RegexOption.MULTILINE)
                )
                val seen = mutableSetOf<String>()
                patterns.forEach { pattern ->
                    pattern.findAll(content).forEach { match ->
                        val key = "${match.groupValues[1]} ${match.groupValues[2]}"
                        if (seen.add(key)) {
                            items.add(StructureNode.Leaf(name = key))
                        }
                    }
                }
            }
            "go" -> {
                val funcPattern = Regex("""^func\s+(\(.*?\)\s+)?(\w+)""", RegexOption.MULTILINE)
                val typePattern = Regex("""^type\s+(\w+)\s+(struct|interface)""", RegexOption.MULTILINE)
                typePattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[2]} ${match.groupValues[1]}"))
                }
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "func ${match.groupValues[2]}"))
                }
            }
            "c", "cpp", "cc", "cxx", "h", "hpp" -> {
                val funcPattern = Regex("""^[\w:*&<>\s]+\s+(\w+)\s*\([^)]*\)\s*\{?""", RegexOption.MULTILINE)
                val classPattern = Regex("""^\s*(class|struct)\s+(\w+)""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[1]} ${match.groupValues[2]}"))
                }
                funcPattern.findAll(content).take(50).forEach { match ->
                    val name = match.groupValues[1]
                    if (name !in setOf("if", "else", "for", "while", "switch", "return", "include", "define")) {
                        items.add(StructureNode.Leaf(name = "func $name"))
                    }
                }
            }
            "rb" -> {
                val classPattern = Regex("""^\s*(class|module)\s+(\w+(?:::\w+)*)""", RegexOption.MULTILINE)
                val methodPattern = Regex("""^\s*def\s+(self\.)?(\w+[?!=]?)""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[1]} ${match.groupValues[2]}"))
                }
                methodPattern.findAll(content).forEach { match ->
                    val prefix = if (match.groupValues[1].isNotEmpty()) "self." else ""
                    items.add(StructureNode.Leaf(name = "def $prefix${match.groupValues[2]}"))
                }
            }
            "php" -> {
                val classPattern = Regex("""^\s*(abstract\s+)?(class|interface|trait|enum)\s+(\w+)""", RegexOption.MULTILINE)
                val funcPattern = Regex("""^\s*(public\s+|private\s+|protected\s+)?(static\s+)?function\s+(\w+)""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[2]} ${match.groupValues[3]}"))
                }
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "function ${match.groupValues[3]}"))
                }
            }
            "swift" -> {
                val classPattern = Regex("""^\s*(class|struct|enum|protocol|actor|extension)\s+(\w+)""", RegexOption.MULTILINE)
                val funcPattern = Regex("""^\s*(func)\s+(\w+)""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[1]} ${match.groupValues[2]}"))
                }
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "func ${match.groupValues[2]}"))
                }
            }
            "scala" -> {
                val classPattern = Regex("""^\s*(class|object|trait|case\s+class|case\s+object|abstract\s+class|enum)\s+(\w+)""", RegexOption.MULTILINE)
                val defPattern = Regex("""^\s*def\s+(\w+)""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[1]} ${match.groupValues[2]}"))
                }
                defPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "def ${match.groupValues[1]}"))
                }
            }
            "dart" -> {
                val classPattern = Regex("""^\s*(abstract\s+)?(class|mixin|enum|extension)\s+(\w+)""", RegexOption.MULTILINE)
                val funcPattern = Regex("""^\s*[\w<>\[\]?]+\s+(\w+)\s*\(""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[2]} ${match.groupValues[3]}"))
                }
                funcPattern.findAll(content).take(50).forEach { match ->
                    val name = match.groupValues[1]
                    if (name !in setOf("if", "else", "for", "while", "switch", "return", "import", "class", "mixin", "enum", "extension")) {
                        items.add(StructureNode.Leaf(name = "func $name"))
                    }
                }
            }
            "pl", "pm" -> {
                val packagePattern = Regex("""^\s*package\s+([\w:]+)""", RegexOption.MULTILINE)
                val subPattern = Regex("""^\s*sub\s+(\w+)""", RegexOption.MULTILINE)
                packagePattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "package ${match.groupValues[1]}"))
                }
                subPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "sub ${match.groupValues[1]}"))
                }
            }
            "ex", "exs" -> {
                val modulePattern = Regex("""^\s*defmodule\s+([\w.]+)""", RegexOption.MULTILINE)
                val funcPattern = Regex("""^\s*(def|defp|defmacro|defmacrop)\s+(\w+[?!]?)""", RegexOption.MULTILINE)
                modulePattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "defmodule ${match.groupValues[1]}"))
                }
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[1]} ${match.groupValues[2]}"))
                }
            }
            "lua" -> {
                val funcPattern = Regex("""^\s*(local\s+)?function\s+([\w.:\-]+)""", RegexOption.MULTILINE)
                val assignFuncPattern = Regex("""^\s*(local\s+)?([\w.:\-]+)\s*=\s*function\s*\(""", RegexOption.MULTILINE)
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "function ${match.groupValues[2]}"))
                }
                assignFuncPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "function ${match.groupValues[2]}"))
                }
            }
            "r" -> {
                val funcPattern = Regex("""^\s*(\w+)\s*<-\s*function\s*\(""", RegexOption.MULTILINE)
                val classPattern = Regex("""^\s*setClass\s*\(\s*["'](\w+)["']""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "class ${match.groupValues[1]}"))
                }
                funcPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "function ${match.groupValues[1]}"))
                }
            }
            "groovy" -> {
                val classPattern = Regex("""^\s*(class|interface|trait|enum)\s+(\w+)""", RegexOption.MULTILINE)
                val defPattern = Regex("""^\s*def\s+(\w+)\s*\(""", RegexOption.MULTILINE)
                classPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "${match.groupValues[1]} ${match.groupValues[2]}"))
                }
                defPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "def ${match.groupValues[1]}"))
                }
            }
            "html", "htm" -> {
                val headingPattern = Regex("""<(h[1-6])[^>]*>\s*(.*?)\s*</\1>""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
                val formPattern = Regex("""<form[^>]*(?:id\s*=\s*["']([^"']+)["']|name\s*=\s*["']([^"']+)["'])[^>]*>""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
                val scriptPattern = Regex("""<script[^>]*(?:src\s*=\s*["']([^"']+)["'])?[^>]*>""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
                val namedDivPattern = Regex("""<(div|section|nav|main|header|footer|aside|article)\s+[^>]*id\s*=\s*["']([^"']+)["'][^>]*>""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
                headingPattern.findAll(content).forEach { match ->
                    val tag = match.groupValues[1].lowercase()
                    val text = match.groupValues[2].replace(Regex("<[^>]+>"), "").trim()
                    if (text.isNotEmpty()) {
                        items.add(StructureNode.Leaf(name = "<$tag> $text"))
                    }
                }
                namedDivPattern.findAll(content).forEach { match ->
                    val tag = match.groupValues[1].lowercase()
                    items.add(StructureNode.Leaf(name = "<$tag> #${match.groupValues[2]}"))
                }
                formPattern.findAll(content).forEach { match ->
                    val id = match.groupValues[1].ifEmpty { match.groupValues[2] }
                    items.add(StructureNode.Leaf(name = "<form> ${if (id.isNotEmpty()) "#$id" else ""}".trim()))
                }
                scriptPattern.findAll(content).forEach { match ->
                    val src = match.groupValues[1]
                    items.add(StructureNode.Leaf(name = if (src.isNotEmpty()) "<script> $src" else "<script> inline"))
                }
            }
            "css", "scss", "less", "sass" -> {
                val selectorPattern = Regex("""^([.#@][\w\-]+(?:\s*[,>+~]\s*[.#@]?[\w\-]+)*)\s*\{""", RegexOption.MULTILINE)
                selectorPattern.findAll(content).take(50).forEach { match ->
                    items.add(StructureNode.Leaf(name = match.groupValues[1].trim()))
                }
            }
            "sql" -> {
                val createPattern = Regex("""^\s*CREATE\s+(TABLE|VIEW|INDEX|FUNCTION|PROCEDURE|TRIGGER|TYPE)\s+(?:IF\s+NOT\s+EXISTS\s+)?(?:[\w.]+\.)?(\w+)""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
                val alterPattern = Regex("""^\s*ALTER\s+(TABLE)\s+(?:[\w.]+\.)?(\w+)""", setOf(RegexOption.MULTILINE, RegexOption.IGNORE_CASE))
                createPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "CREATE ${match.groupValues[1].uppercase()} ${match.groupValues[2]}"))
                }
                alterPattern.findAll(content).forEach { match ->
                    items.add(StructureNode.Leaf(name = "ALTER ${match.groupValues[1].uppercase()} ${match.groupValues[2]}"))
                }
            }
            "sh", "bash", "zsh", "fish" -> {
                val funcPattern = Regex("""^\s*(?:function\s+)?(\w+)\s*\(\s*\)""", RegexOption.MULTILINE)
                val funcKeyword = Regex("""^\s*function\s+(\w+)""", RegexOption.MULTILINE)
                val seen = mutableSetOf<String>()
                funcPattern.findAll(content).forEach { match ->
                    val name = match.groupValues[1]
                    if (seen.add(name)) {
                        items.add(StructureNode.Leaf(name = "function $name"))
                    }
                }
                funcKeyword.findAll(content).forEach { match ->
                    val name = match.groupValues[1]
                    if (seen.add(name)) {
                        items.add(StructureNode.Leaf(name = "function $name"))
                    }
                }
            }
        }

        if (items.isEmpty()) return null
        return StructureNode.Branch(name = "Outline", children = items.take(100))
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        return "%.1f MB".format(mb)
    }
}
