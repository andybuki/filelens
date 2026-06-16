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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipFile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Browser extensions:
 *  - .xpi (Firefox) is a plain ZIP.
 *  - .crx (Chrome) is a CRX2/CRX3 wrapper that begins with "Cr24", followed by
 *    a version and header length. The ZIP starts after the header.
 *
 * We surface the manifest.json — name, version, host permissions, and the
 * permission list — and flag dangerous combinations.
 */
@Singleton
class BrowserExtensionInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.ARCHIVE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val ext = file.extension.lowercase()
        overview["Format"] = when (ext) {
            "xpi" -> "Firefox extension (XPI)"
            "crx" -> "Chrome extension (CRX)"
            else -> "Browser extension"
        }

        try {
            val zipSource = if (ext == "crx") stripCrxHeader(file) else file
            ZipFile(zipSource).use { zip ->
                val entries = zip.entries().asSequence().toList()
                overview["Total files"] = entries.count { !it.isDirectory }.toString()

                val manifestEntry = zip.getEntry("manifest.json")
                if (manifestEntry == null) {
                    overview["Note"] = "No manifest.json found"
                } else {
                    val text = zip.getInputStream(manifestEntry).bufferedReader().readText()
                    parseManifest(text, overview, metadata, risks)
                }

                val tree = StructureNode.Branch(
                    name = "Contents",
                    children = entries.take(40).map {
                        StructureNode.Leaf(name = it.name, value = formatSize(it.size.coerceAtLeast(0)), size = it.size)
                    } + if (entries.size > 40) listOf(StructureNode.Leaf(name = "… ${entries.size - 40} more")) else emptyList()
                )
                return@withContext InspectorResult(
                    overview = overview,
                    metadata = metadata,
                    risks = risks,
                    structure = tree
                )
            }
        } catch (e: Exception) {
            overview["Error"] = "Failed to read extension: ${e.message?.take(120)}"
            InspectorResult(overview = overview, metadata = metadata, risks = risks)
        }
    }

    private fun parseManifest(
        text: String,
        overview: MutableMap<String, String>,
        metadata: MutableMap<String, String>,
        risks: MutableList<RiskItem>
    ) {
        val obj = try { JSONObject(text) } catch (_: Exception) {
            overview["Manifest"] = "Invalid JSON"
            return
        }
        obj.optString("name").takeIf { it.isNotBlank() }?.let { overview["Name"] = it }
        obj.optString("version").takeIf { it.isNotBlank() }?.let { overview["Version"] = it }
        obj.opt("manifest_version")?.let { overview["Manifest version"] = it.toString() }
        obj.optString("description").takeIf { it.isNotBlank() }?.let { metadata["Description"] = it.take(300) }
        obj.optString("author").takeIf { it.isNotBlank() }?.let { metadata["Author"] = it }
        obj.optString("homepage_url").takeIf { it.isNotBlank() }?.let { metadata["Homepage"] = it }

        val perms = obj.optJSONArray("permissions").toStringList()
        val hostPerms = obj.optJSONArray("host_permissions").toStringList()
        val optionalHosts = obj.optJSONArray("optional_host_permissions").toStringList()
        val contentScripts = obj.optJSONArray("content_scripts")

        if (perms.isNotEmpty()) {
            overview["Permissions"] = perms.size.toString()
            metadata["Permissions"] = perms.joinToString("\n")
        }
        if (hostPerms.isNotEmpty()) {
            overview["Host permissions"] = hostPerms.size.toString()
            metadata["Host permissions"] = hostPerms.joinToString("\n")
        }
        if (optionalHosts.isNotEmpty()) {
            metadata["Optional host permissions"] = optionalHosts.joinToString("\n")
        }

        val contentScriptMatches = mutableListOf<String>()
        if (contentScripts != null) {
            for (i in 0 until contentScripts.length()) {
                val cs = contentScripts.optJSONObject(i) ?: continue
                cs.optJSONArray("matches")?.toStringList()?.let { contentScriptMatches.addAll(it) }
            }
            if (contentScriptMatches.isNotEmpty()) {
                overview["Content scripts inject into"] = contentScriptMatches.distinct().size.toString()
                metadata["Content script matches"] = contentScriptMatches.distinct().take(15).joinToString("\n")
            }
        }

        val allHosts = hostPerms + contentScriptMatches
        val matchesAllUrls = allHosts.any { it == "<all_urls>" || it == "*://*/*" || it == "http://*/*" || it == "https://*/*" }
        val sensitive = perms.intersect(setOf(
            "webRequest", "webRequestBlocking", "tabs", "cookies", "history",
            "bookmarks", "downloads", "clipboardRead", "nativeMessaging",
            "debugger", "proxy", "privacy", "management", "geolocation",
            "browsingData"
        ))

        if (matchesAllUrls) {
            risks += RiskItem(
                id = "ext-all-urls",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.PRIVACY,
                title = "Runs on every site",
                description = "Host pattern matches all URLs.",
                whyItMatters = "Extensions that run on every page see and can modify all browsing activity, including private sites.",
                recommendedAction = "Install only from sources you trust. Audit before granting permanent permission.",
                detector = "BrowserExtensionInspector"
            )
        }
        if ("nativeMessaging" in perms) {
            risks += RiskItem(
                id = "ext-native-messaging",
                severity = RiskSeverity.DANGER,
                category = RiskCategory.SECURITY,
                title = "Talks to a native helper",
                description = "Permission 'nativeMessaging' is requested.",
                whyItMatters = "Native messaging lets the extension exchange data with a desktop binary, bypassing the browser sandbox.",
                recommendedAction = "Verify the companion app and the vendor before installing.",
                detector = "BrowserExtensionInspector"
            )
        }
        if ("debugger" in perms) {
            risks += RiskItem(
                id = "ext-debugger",
                severity = RiskSeverity.DANGER,
                category = RiskCategory.SECURITY,
                title = "Requests the debugger API",
                description = "Permission 'debugger' is requested.",
                whyItMatters = "The debugger permission can read and inject anything in any tab, including DRM and credentials.",
                recommendedAction = "Only allow for developer tooling you control.",
                detector = "BrowserExtensionInspector"
            )
        }
        if (sensitive.any { it in setOf("cookies", "history", "tabs") } && matchesAllUrls) {
            risks += RiskItem(
                id = "ext-broad-data",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.PRIVACY,
                title = "Broad access to browsing data",
                description = "Sensitive permissions combined with all-sites access: ${sensitive.joinToString(", ")}.",
                whyItMatters = "Cookies, history, and tabs APIs plus all-sites injection enable end-to-end tracking of browsing behaviour.",
                recommendedAction = "Reserve for first-party productivity tools you understand.",
                detector = "BrowserExtensionInspector"
            )
        }
    }

    /**
     * CRX header: "Cr24" + uint32 version + uint32 headerLen + headerBytes + ZIP.
     * We copy the trailing ZIP to a temp file and return that — ZipFile insists
     * on real files. Caller cleans up via the returned File.deleteOnExit().
     */
    private fun stripCrxHeader(file: File): File {
        FileInputStream(file).use { stream ->
            val sig = ByteArray(4)
            if (stream.read(sig) < 4) return file
            if (String(sig, Charsets.US_ASCII) != "Cr24") return file
            val header = ByteArray(8)
            if (stream.read(header) < 8) return file
            val version = ((header[0].toInt() and 0xFF)) or
                ((header[1].toInt() and 0xFF) shl 8) or
                ((header[2].toInt() and 0xFF) shl 16) or
                ((header[3].toInt() and 0xFF) shl 24)
            val zipStartOffset = when (version) {
                2 -> {
                    // CRX2: public key length + signature length, then ZIP.
                    val pubKeyLen = readLE(header, 4)
                    val sigLen = readLE(stream)
                    if (stream.skip(pubKeyLen.toLong() + sigLen.toLong()) != pubKeyLen.toLong() + sigLen.toLong()) return file
                    null
                }
                3 -> {
                    // CRX3: single header length, ZIP after.
                    val headerLen = ((header[4].toInt() and 0xFF)) or
                        ((header[5].toInt() and 0xFF) shl 8) or
                        ((header[6].toInt() and 0xFF) shl 16) or
                        ((header[7].toInt() and 0xFF) shl 24)
                    if (stream.skip(headerLen.toLong()) != headerLen.toLong()) return file
                    null
                }
                else -> return file
            }
            // Drain remaining bytes to a temp file.
            val tmp = File.createTempFile("crx-zip-", ".zip")
            tmp.deleteOnExit()
            tmp.outputStream().use { out -> stream.copyTo(out) }
            return tmp
        }
    }

    private fun readLE(buf: ByteArray, offset: Int): Int =
        (buf[offset].toInt() and 0xFF) or
            ((buf[offset + 1].toInt() and 0xFF) shl 8) or
            ((buf[offset + 2].toInt() and 0xFF) shl 16) or
            ((buf[offset + 3].toInt() and 0xFF) shl 24)

    private fun readLE(stream: java.io.InputStream): Int {
        val b = ByteArray(4)
        if (stream.read(b) < 4) return 0
        return readLE(b, 0)
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        val out = mutableListOf<String>()
        for (i in 0 until length()) {
            val v = opt(i)
            if (v is String) out.add(v)
        }
        return out
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        return "%.1f MB".format(kb / 1024.0)
    }
}
