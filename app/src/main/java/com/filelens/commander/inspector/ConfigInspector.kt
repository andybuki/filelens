package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskCategory
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.Risks
import com.filelens.commander.domain.model.StructureNode
import com.filelens.commander.domain.usecase.SecretScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConfigInspector @Inject constructor(
    private val secretScanner: SecretScanner
) : FileInspector {

    override val supportedCategories = setOf(FileCategory.TEXT)

    fun handles(file: File): Boolean {
        val name = file.name.lowercase()
        if (name.startsWith(".env")) return true
        if (name in filenameOnly) return true
        val ext = file.extension.lowercase()
        return ext in supportedExtensions
    }

    private val supportedExtensions = setOf(
        "env", "properties", "ini", "yaml", "yml", "toml", "conf", "cfg",
        "tf", "tfvars", "hcl", "nix",
        "editorconfig", "gitignore", "gitattributes"
    )

    private val filenameOnly = setOf(
        "dockerfile", "containerfile",
        "makefile", "gnumakefile",
        "cmakelists.txt",
        "build", "build.bazel", "workspace", "workspace.bazel",
        ".editorconfig", ".gitignore", ".gitattributes",
        ".prettierrc", ".eslintrc", ".npmrc", ".yarnrc",
        "procfile"
    )

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val maxBytes = 1 * 1024 * 1024L
        val content = if (file.length() <= maxBytes) {
            file.readText()
        } else {
            overview["Note"] = "Preview truncated (${formatSize(file.length())})"
            file.reader().use { it.readText().take(maxBytes.toInt()) }
        }

        val format = detectFormat(file)
        overview["Config format"] = format
        val framework = detectFramework(file, content)
        if (framework != null) overview["Likely framework"] = framework

        val pairs = parsePairs(content, format)
        overview["Keys"] = pairs.size.toString()
        if (pairs.isNotEmpty()) {
            val duplicates = pairs.groupBy { it.key }.filterValues { it.size > 1 }
            val empties = pairs.count { it.value.isBlank() }
            if (duplicates.isNotEmpty()) overview["Duplicate keys"] = duplicates.size.toString()
            if (empties > 0) overview["Empty values"] = empties.toString()
        }

        val urls = mutableSetOf<String>()
        val ports = mutableSetOf<Int>()
        val httpInsecure = mutableListOf<String>()
        val productionMarkers = mutableListOf<String>()
        val debugFlags = mutableListOf<String>()

        val urlRegex = Regex("https?://[^\\s\"'<>]+")
        val portRegex = Regex("(?i)port\\s*[=:]\\s*(\\d{2,5})")
        val prodMarker = Regex("(?i)\\b(prod|production)\\b")
        val debugMarker = Regex("(?i)(debug|verbose)\\s*[=:]\\s*(true|on|1|yes)")

        for (line in content.lineSequence()) {
            for (m in urlRegex.findAll(line)) {
                urls += m.value
                if (m.value.startsWith("http://") &&
                    !m.value.contains("localhost") &&
                    !m.value.contains("127.0.0.1") &&
                    !m.value.contains("0.0.0.0")) {
                    httpInsecure += m.value
                }
            }
            portRegex.find(line)?.let {
                it.groupValues[1].toIntOrNull()?.let { p -> ports += p }
            }
            if (prodMarker.containsMatchIn(line)) productionMarkers += line.trim().take(120)
            if (debugMarker.containsMatchIn(line)) debugFlags += line.trim().take(120)
        }

        if (urls.isNotEmpty()) overview["URLs"] = urls.size.toString()
        if (ports.isNotEmpty()) overview["Ports"] = ports.sorted().joinToString(", ")
        if (httpInsecure.isNotEmpty()) {
            risks.add(RiskItem(
                id = "config-insecure-http",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.SECURITY,
                title = "Insecure http:// URLs",
                description = httpInsecure.distinct().take(3).joinToString("\n"),
                whyItMatters = "Plain HTTP traffic can be intercepted on shared networks. Hard-coded HTTP endpoints in a config often ship into production by accident.",
                recommendedAction = "Switch to https:// where possible. Confirm the endpoint is intentional if it must stay HTTP.",
                evidence = mapOf("Count" to httpInsecure.size.toString()),
                detector = "ConfigInspector"
            ))
        }
        if (productionMarkers.isNotEmpty()) {
            risks.add(RiskItem(
                id = "config-production-marker",
                severity = RiskSeverity.INFO,
                category = RiskCategory.OTHER,
                title = "Production markers",
                description = "Lines mentioning prod / production: ${productionMarkers.size}",
                whyItMatters = "Production endpoints, hosts, or profile flags in a config increase the blast radius if the file is leaked.",
                recommendedAction = "Confirm production credentials are not committed to source control.",
                detector = "ConfigInspector"
            ))
        }
        if (debugFlags.isNotEmpty()) {
            risks.add(RiskItem(
                id = "config-debug-flag",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.SECURITY,
                title = "Debug / verbose flag enabled",
                description = debugFlags.first(),
                whyItMatters = "Debug mode usually disables security checks, exposes stack traces, and leaks internal paths. Dangerous to ship to production.",
                recommendedAction = "Disable debug before deploying or sharing the file.",
                evidence = mapOf("Lines" to debugFlags.size.toString()),
                detector = "ConfigInspector"
            ))
        }

        val secrets = secretScanner.scan(content, location = file.name)
        if (secrets.isNotEmpty()) {
            risks.add(Risks.secretsFound(secrets.size, secrets.map { it.kind }.distinct()))
        }

        val structure = buildStructure(pairs)

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            structure = structure,
            previewContent = PreviewContent.TextContent(
                content = content.take(100_000),
                language = languageForFormat(format),
                lineCount = content.count { it == '\n' } + 1
            ),
            secrets = secrets
        )
    }

    private data class KvPair(val key: String, val value: String, val lineNumber: Int)

    private fun detectFormat(file: File): String {
        val name = file.name.lowercase()
        if (name.startsWith(".env")) return ".env"
        if (name == "dockerfile" || name == "containerfile") return "Dockerfile"
        if (name == "makefile" || name == "gnumakefile") return "Makefile"
        if (name == "cmakelists.txt") return "CMake"
        if (name == "build" || name == "build.bazel" || name == "workspace" || name == "workspace.bazel") return "Bazel"
        if (name == "procfile") return "Procfile"
        if (name == ".editorconfig" || name == "editorconfig") return "EditorConfig"
        if (name == ".gitignore" || name == "gitignore") return "Git ignore"
        if (name == ".gitattributes" || name == "gitattributes") return "Git attributes"
        return when (file.extension.lowercase()) {
            "yaml", "yml" -> "YAML"
            "toml" -> "TOML"
            "ini" -> "INI"
            "cfg" -> "INI / cfg"
            "conf" -> "conf"
            "properties" -> "Java properties"
            "env" -> ".env"
            "tf", "tfvars" -> "Terraform"
            "hcl" -> "HCL"
            "nix" -> "Nix"
            else -> file.extension
        }
    }

    private fun languageForFormat(format: String): String = when (format) {
        "YAML" -> "YAML"
        "TOML" -> "TOML"
        "INI", "INI / cfg", "conf" -> "INI"
        "Java properties", ".env" -> "Properties"
        "Terraform", "HCL", "Nix" -> "HCL"
        "Dockerfile" -> "Dockerfile"
        "Makefile", "CMake", "Bazel" -> "Makefile"
        else -> "Text"
    }

    private fun detectFramework(file: File, content: String): String? {
        val name = file.name.lowercase()
        return when {
            name == "application.yml" || name == "application.yaml" || name == "application.properties" -> "Spring Boot"
            name == "build.gradle" || name == "build.gradle.kts" || name == "settings.gradle" || name == "settings.gradle.kts" || name == "gradle.properties" -> "Gradle"
            name == "package.json" -> "npm / yarn / pnpm"
            name == "pyproject.toml" || name == "setup.cfg" || name == "tox.ini" -> "Python project"
            name == "cargo.toml" -> "Rust / Cargo"
            name == "go.mod" -> "Go modules"
            name == "docker-compose.yml" || name == "docker-compose.yaml" || name == "compose.yml" || name == "compose.yaml" -> "Docker Compose"
            name.endsWith(".tf") -> "Terraform"
            name.contains("kustomization") -> "Kustomize / Kubernetes"
            name.endsWith(".yml") && content.contains("apiVersion:") && content.contains("kind:") -> "Kubernetes manifest"
            name.endsWith(".yml") && content.contains("jobs:") && content.contains("uses: actions/") -> "GitHub Actions workflow"
            name == ".env" || name.startsWith(".env.") -> "Dotenv"
            else -> null
        }
    }

    private fun parsePairs(content: String, format: String): List<KvPair> {
        val out = mutableListOf<KvPair>()
        when (format) {
            "YAML", "TOML", "INI", "INI / cfg", "conf", "Java properties", ".env", "Dockerfile" -> {
                content.lineSequence().forEachIndexed { idx, raw ->
                    val line = raw.trim()
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith(";") || line.startsWith("//")) return@forEachIndexed
                    if (line.startsWith("[") && line.endsWith("]")) return@forEachIndexed
                    val sepIndex = findSeparator(line, format)
                    if (sepIndex > 0) {
                        val key = line.substring(0, sepIndex).trim().trimEnd('"').trimStart('"')
                        val value = line.substring(sepIndex + 1).trim()
                        if (key.isNotEmpty()) {
                            out += KvPair(key, value, idx + 1)
                        }
                    }
                }
            }
            else -> {}
        }
        return out
    }

    private fun findSeparator(line: String, format: String): Int {
        return when (format) {
            "YAML" -> {
                val i = line.indexOf(':')
                if (i > 0 && (i == line.length - 1 || line.getOrNull(i + 1) == ' ' || line.getOrNull(i + 1) == null)) i else -1
            }
            else -> {
                val eq = line.indexOf('=')
                val co = line.indexOf(':')
                when {
                    eq > 0 && (co < 0 || eq < co) -> eq
                    co > 0 -> co
                    else -> -1
                }
            }
        }
    }

    private fun buildStructure(pairs: List<KvPair>): StructureNode? {
        if (pairs.isEmpty()) return null
        val leaves = pairs.take(200).map { kv ->
            StructureNode.Leaf(name = kv.key, value = if (kv.value.length > 60) kv.value.take(60) + "…" else kv.value)
        }
        return StructureNode.Branch(
            name = "Keys",
            children = if (pairs.size > 200) leaves + StructureNode.Leaf("… ${pairs.size - 200} more") else leaves,
            details = "${pairs.size} keys"
        )
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        return "%.1f MB".format(kb / 1024.0)
    }
}
