package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskCategory
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VpnConfigInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.CERTIFICATE)

    fun handles(file: File): Boolean = file.extension.lowercase() in handledExtensions

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val content = try {
            file.readText()
        } catch (e: Exception) {
            return@withContext InspectorResult(
                overview = mapOf("Error" to "Could not read file: ${(e.message ?: "").take(120)}")
            )
        }

        when (file.extension.lowercase()) {
            "ovpn" -> inspectOpenVpn(content)
            "wg", "conf" -> inspectWireGuard(content)
            "mobileconfig" -> inspectMobileConfig(content)
            else -> inspectGeneric(content)
        }
    }

    private fun inspectOpenVpn(content: String): InspectorResult {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        overview["Format"] = "OpenVPN config"

        val directives = mutableMapOf<String, String>()
        content.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";") || line.startsWith("<")) return@forEach
            val space = line.indexOf(' ')
            val key = if (space < 0) line else line.substring(0, space)
            val value = if (space < 0) "" else line.substring(space + 1).trim()
            directives.putIfAbsent(key, value)
        }

        val remotes = content.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("remote ") }
            .map { it.removePrefix("remote ").trim() }
            .toList()
        if (remotes.isNotEmpty()) {
            overview["Remote endpoints"] = remotes.size.toString()
            metadata["Remotes"] = remotes.take(10).joinToString("\n")
        }
        directives["proto"]?.let { overview["Protocol"] = it }
        directives["port"]?.let { overview["Port"] = it }
        directives["cipher"]?.let { overview["Cipher"] = it }
        directives["auth"]?.let { overview["Auth digest"] = it }
        directives["dev"]?.let { overview["Device"] = it }

        val hasInlinedKey = Regex("(?m)<key>[\\s\\S]*?</key>").containsMatchIn(content)
        val hasInlinedCert = Regex("(?m)<cert>[\\s\\S]*?</cert>").containsMatchIn(content)
        val hasInlinedCa = Regex("(?m)<ca>[\\s\\S]*?</ca>").containsMatchIn(content)
        val hasTlsAuth = "tls-auth" in directives || Regex("(?m)<tls-auth>").containsMatchIn(content)
        val hasTlsCrypt = "tls-crypt" in directives || Regex("(?m)<tls-crypt>").containsMatchIn(content) ||
            Regex("(?m)<tls-crypt-v2>").containsMatchIn(content)
        val embedsPassword = "auth-user-pass" in directives && directives["auth-user-pass"]!!.isNotBlank()
        val verb = directives["verb"]?.toIntOrNull() ?: 0

        if (hasInlinedKey) overview["Inlined private key"] = "Yes"
        if (hasInlinedCert) overview["Inlined certificate"] = "Yes"
        if (hasInlinedCa) overview["Inlined CA"] = "Yes"
        overview["tls-auth / tls-crypt"] = when {
            hasTlsCrypt -> "tls-crypt"
            hasTlsAuth -> "tls-auth"
            else -> "None"
        }

        if (hasInlinedKey) {
            risks += RiskItem(
                id = "ovpn-inline-key",
                severity = RiskSeverity.DANGER,
                category = RiskCategory.SECURITY,
                title = "Private key embedded in profile",
                description = "The .ovpn file contains an inline <key> block.",
                whyItMatters = "Anyone holding the file can connect as you. Treat it like a password.",
                recommendedAction = "Store the key in a separate file with restrictive permissions, or use a hardware token.",
                detector = "VpnConfigInspector"
            )
        }
        if (!hasTlsAuth && !hasTlsCrypt) {
            risks += RiskItem(
                id = "ovpn-no-tls-auth",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.SECURITY,
                title = "No tls-auth or tls-crypt",
                description = "Profile omits both tls-auth and tls-crypt directives.",
                whyItMatters = "These directives add an HMAC firewall in front of the TLS handshake. Without them the server is more exposed to scanning and DoS.",
                recommendedAction = "Ask the server administrator to enable tls-crypt and ship a matching profile.",
                detector = "VpnConfigInspector"
            )
        }
        if (embedsPassword) {
            risks += RiskItem(
                id = "ovpn-embedded-credentials",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.SECURITY,
                title = "Credentials file referenced in profile",
                description = "auth-user-pass points to ${directives["auth-user-pass"]}.",
                whyItMatters = "Storing the credentials path inside a shareable profile can leak account material if both files travel together.",
                recommendedAction = "Keep the credentials file outside the profile and prompt at connect time.",
                detector = "VpnConfigInspector"
            )
        }
        directives["cipher"]?.let { cipher ->
            val weak = listOf("BF-CBC", "DES-CBC", "3DES", "RC2", "RC4", "NONE")
            if (weak.any { cipher.uppercase().contains(it) }) {
                risks += RiskItem(
                    id = "ovpn-weak-cipher",
                    severity = RiskSeverity.WARNING,
                    category = RiskCategory.SECURITY,
                    title = "Weak data-channel cipher",
                    description = "cipher $cipher.",
                    whyItMatters = "Legacy ciphers such as Blowfish and 3DES are no longer considered secure for VPN data.",
                    recommendedAction = "Switch the server and profile to AES-256-GCM or CHACHA20-POLY1305.",
                    detector = "VpnConfigInspector"
                )
            }
        }
        if (verb >= 6) {
            risks += RiskItem(
                id = "ovpn-debug-verbose",
                severity = RiskSeverity.INFO,
                category = RiskCategory.SECURITY,
                title = "High log verbosity",
                description = "verb $verb (debug level).",
                whyItMatters = "Verbose logs may capture connection metadata and credentials in shared log destinations.",
                recommendedAction = "Lower verb to 3 or 4 for normal operation.",
                detector = "VpnConfigInspector"
            )
        }

        return InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            previewContent = PreviewContent.TextContent(
                content = content.take(64_000),
                language = "OpenVPN",
                lineCount = content.count { it == '\n' } + 1
            )
        )
    }

    private fun inspectWireGuard(content: String): InspectorResult {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        overview["Format"] = "WireGuard config"

        val sections = mutableMapOf<String, MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        var currentName = ""
        for (raw in content.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (line.startsWith("[") && line.endsWith("]")) {
                currentName = line.substring(1, line.length - 1)
                current = sections.getOrPut(currentName) { mutableMapOf() }
                continue
            }
            val eq = line.indexOf('=')
            if (eq <= 0 || current == null) continue
            current[line.substring(0, eq).trim()] = line.substring(eq + 1).trim()
        }

        val iface = sections["Interface"]
        val peers = sections.filterKeys { it.equals("Peer", ignoreCase = true) }
        if (iface == null) {
            overview["Note"] = "No [Interface] section detected. May not be a WireGuard profile."
            return InspectorResult(overview = overview, previewContent = textPreview(content, "WireGuard"))
        }

        iface["Address"]?.let { overview["Tunnel address"] = it }
        iface["ListenPort"]?.let { overview["Listen port"] = it }
        iface["DNS"]?.let { overview["DNS"] = it }
        iface["MTU"]?.let { metadata["MTU"] = it }
        overview["Peers"] = peers.size.toString()

        val hasPrivateKey = !iface["PrivateKey"].isNullOrBlank()
        if (hasPrivateKey) {
            risks += RiskItem(
                id = "wg-private-key",
                severity = RiskSeverity.DANGER,
                category = RiskCategory.SECURITY,
                title = "WireGuard private key in profile",
                description = "The [Interface] section contains a PrivateKey value.",
                whyItMatters = "WireGuard profiles embed the device's private key. Sharing the file lets anyone impersonate this peer.",
                recommendedAction = "Store the profile in a secure location. Rotate the key if the file may have been shared.",
                detector = "VpnConfigInspector"
            )
        }

        peers.values.forEachIndexed { index, peer ->
            val endpoint = peer["Endpoint"] ?: "?"
            val allowed = peer["AllowedIPs"] ?: "?"
            metadata["Peer ${index + 1}"] = "Endpoint $endpoint  •  AllowedIPs $allowed"
            if (allowed.contains("0.0.0.0/0") || allowed.contains("::/0")) {
                risks += RiskItem(
                    id = "wg-default-route-${index}",
                    severity = RiskSeverity.INFO,
                    category = RiskCategory.PRIVACY,
                    title = "Default route via VPN",
                    description = "Peer ${index + 1} routes all traffic ($allowed).",
                    whyItMatters = "Full-tunnel routing sends every connection through the VPN provider. Make sure the provider is trusted.",
                    recommendedAction = "Confirm this matches your intent. For split-tunnel, restrict AllowedIPs.",
                    detector = "VpnConfigInspector"
                )
            }
        }

        return InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            previewContent = textPreview(content, "WireGuard")
        )
    }

    private fun inspectMobileConfig(content: String): InspectorResult {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        overview["Format"] = "Apple Configuration Profile (.mobileconfig)"

        val displayName = plistString(content, "PayloadDisplayName")
        val description = plistString(content, "PayloadDescription")
        val organization = plistString(content, "PayloadOrganization")
        val identifier = plistString(content, "PayloadIdentifier")
        displayName?.let { overview["Profile"] = it }
        organization?.let { overview["Organization"] = it }
        identifier?.let { metadata["Identifier"] = it }
        description?.let { metadata["Description"] = it }

        val payloadTypes = Regex("""<key>PayloadType</key>\s*<string>([^<]+)</string>""")
            .findAll(content)
            .map { it.groupValues[1] }
            .toList()
        val perPayloadTypes = payloadTypes.drop(1) // first is the outer Configuration profile
        overview["Payloads"] = perPayloadTypes.size.toString()
        if (perPayloadTypes.isNotEmpty()) {
            metadata["Payload types"] = perPayloadTypes
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .joinToString("\n") { "${it.key} × ${it.value}" }
        }

        val hasMdm = perPayloadTypes.any { it.contains("MDM", ignoreCase = true) }
        if (hasMdm) {
            risks += RiskItem(
                id = "mobileconfig-mdm",
                severity = RiskSeverity.DANGER,
                category = RiskCategory.SECURITY,
                title = "Profile enrols device in MDM",
                description = "A com.apple.mdm payload is present.",
                whyItMatters = "Mobile Device Management gives the issuer remote control over the device: install apps, wipe data, read network traffic.",
                recommendedAction = "Only install if you trust the organization and intend to be managed.",
                detector = "VpnConfigInspector"
            )
        }
        if (perPayloadTypes.any { it.contains("VPN", ignoreCase = true) }) {
            risks += RiskItem(
                id = "mobileconfig-vpn",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.PRIVACY,
                title = "Installs a VPN configuration",
                description = "Adds a VPN payload to the device.",
                whyItMatters = "All matching traffic routes through the configured VPN, which can read or modify it.",
                recommendedAction = "Confirm the VPN endpoint is trusted before installing.",
                detector = "VpnConfigInspector"
            )
        }
        if (perPayloadTypes.any { it.contains("Wi-Fi", ignoreCase = true) || it.contains("WIFI", ignoreCase = true) }) {
            risks += RiskItem(
                id = "mobileconfig-wifi",
                severity = RiskSeverity.INFO,
                category = RiskCategory.SECURITY,
                title = "Configures Wi-Fi networks",
                description = "Adds Wi-Fi network credentials.",
                whyItMatters = "Auto-join Wi-Fi profiles can attach the device to networks the user did not pick.",
                recommendedAction = "Review the SSIDs that the profile adds.",
                detector = "VpnConfigInspector"
            )
        }
        if (perPayloadTypes.any { it.contains("Certificate", ignoreCase = true) || it.contains("CertificateRoot", ignoreCase = true) }) {
            risks += RiskItem(
                id = "mobileconfig-root-ca",
                severity = RiskSeverity.DANGER,
                category = RiskCategory.SECURITY,
                title = "Installs trusted certificates",
                description = "Profile contains a Certificate payload — possibly a root CA.",
                whyItMatters = "A trusted root CA can sign any website. An attacker-installed root enables full traffic decryption.",
                recommendedAction = "Only accept root certificates from organizations you trust.",
                detector = "VpnConfigInspector"
            )
        }

        val removalDisallowed = Regex("""<key>PayloadRemovalDisallowed</key>\s*<(true|false)/>""")
            .find(content)?.groupValues?.get(1)
        if (removalDisallowed == "true") {
            risks += RiskItem(
                id = "mobileconfig-non-removable",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.SECURITY,
                title = "Profile cannot be removed by the user",
                description = "PayloadRemovalDisallowed is true.",
                whyItMatters = "Non-removable profiles are typical for enterprise control. Personal devices should rarely accept them.",
                recommendedAction = "Reject the profile unless the device is intended to be managed.",
                detector = "VpnConfigInspector"
            )
        }

        return InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            previewContent = textPreview(content, "XML")
        )
    }

    private fun inspectGeneric(content: String): InspectorResult = InspectorResult(
        overview = mapOf("Note" to "Unrecognised VPN profile format."),
        previewContent = textPreview(content, "Text")
    )

    private fun plistString(xml: String, key: String): String? {
        val re = Regex("""<key>\Q$key\E</key>\s*<string>([^<]*)</string>""")
        return re.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun textPreview(content: String, language: String): PreviewContent.TextContent =
        PreviewContent.TextContent(
            content = content.take(64_000),
            language = language,
            lineCount = content.count { it == '\n' } + 1
        )

    companion object {
        private val handledExtensions = setOf("ovpn", "wg", "mobileconfig")
    }
}
