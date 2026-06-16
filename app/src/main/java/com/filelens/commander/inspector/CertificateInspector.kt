package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskCategory
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CertificateInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.CERTIFICATE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val ext = file.extension.lowercase()
        overview["Format"] = formatLabel(ext)

        val bytes = try {
            file.readBytes()
        } catch (e: Exception) {
            overview["Error"] = "Could not read file: ${e.message?.take(120)}"
            return@withContext InspectorResult(overview = overview)
        }

        val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull()
        val isPem = text != null && text.contains("-----BEGIN ")

        if (isPem) {
            inspectPem(text!!, overview, metadata, risks)
        } else {
            inspectBinary(file, bytes, ext, overview, metadata, risks)
        }

        val preview = if (isPem) {
            PreviewContent.TextContent(
                content = text!!.take(64_000),
                language = "PEM",
                lineCount = text.count { it == '\n' } + 1
            )
        } else null

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            previewContent = preview
        )
    }

    private fun formatLabel(ext: String): String = when (ext) {
        "pem" -> "PEM"
        "crt" -> "X.509 certificate (CRT)"
        "cer" -> "X.509 certificate (CER)"
        "der" -> "DER-encoded certificate"
        "key" -> "Private key"
        "pub" -> "SSH public key"
        "ppk" -> "PuTTY private key"
        "p12", "pfx" -> "PKCS#12 keystore"
        "jks" -> "Java KeyStore"
        else -> "Certificate / key"
    }

    private fun inspectPem(
        text: String,
        overview: MutableMap<String, String>,
        metadata: MutableMap<String, String>,
        risks: MutableList<RiskItem>
    ) {
        val blocks = pemBlockRegex.findAll(text).toList()
        if (blocks.isEmpty()) {
            overview["Note"] = "No PEM blocks found."
            return
        }
        overview["PEM blocks"] = blocks.size.toString()
        val labels = blocks.map { it.groupValues[1] }.distinct()
        overview["Block types"] = labels.joinToString(", ")

        val hasPrivateKey = labels.any { it.contains("PRIVATE KEY") }
        val hasEncryptedPrivateKey = labels.any { it == "ENCRYPTED PRIVATE KEY" }
        if (hasPrivateKey) {
            risks += if (hasEncryptedPrivateKey) {
                RiskItem(
                    id = "cert-encrypted-private-key",
                    severity = RiskSeverity.WARNING,
                    category = RiskCategory.SECURITY,
                    title = "Encrypted private key present",
                    description = "An encrypted PRIVATE KEY block is included.",
                    whyItMatters = "Encrypted keys still leak if the passphrase is weak or known. Keep the file in a secure location.",
                    recommendedAction = "Verify the passphrase strength and rotate the key if the file may have been shared.",
                    detector = "CertificateInspector"
                )
            } else {
                RiskItem(
                    id = "cert-plain-private-key",
                    severity = RiskSeverity.DANGER,
                    category = RiskCategory.SECURITY,
                    title = "Unencrypted private key",
                    description = "A plaintext PRIVATE KEY block is included.",
                    whyItMatters = "Anyone with this file can impersonate the owner of the certificate. Do not share or commit it.",
                    recommendedAction = "Re-encrypt with a passphrase or move the key to a hardware token / secrets manager.",
                    detector = "CertificateInspector"
                )
            }
        }

        // Decode each CERTIFICATE block and surface the first cert in detail.
        val certs = blocks
            .filter { it.groupValues[1] == "CERTIFICATE" }
            .mapNotNull { runCatching { parseCertificate(it.groupValues[2]) }.getOrNull() }

        if (certs.isNotEmpty()) {
            overview["Certificates"] = certs.size.toString()
            describeCertificate(certs.first(), overview, metadata, risks)
            if (certs.size > 1) {
                metadata["Chain"] = certs.joinToString("\n") {
                    "${shortName(it.subjectX500Principal.name)} → ${shortName(it.issuerX500Principal.name)}"
                }
            }
        }
    }

    private fun inspectBinary(
        file: File,
        bytes: ByteArray,
        ext: String,
        overview: MutableMap<String, String>,
        metadata: MutableMap<String, String>,
        risks: MutableList<RiskItem>
    ) {
        // DER-encoded certificate or container — attempt X.509 parse first.
        val cert = runCatching {
            val factory = CertificateFactory.getInstance("X.509")
            factory.generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
        }.getOrNull()

        if (cert != null) {
            describeCertificate(cert, overview, metadata, risks)
            return
        }

        when (ext) {
            "p12", "pfx" -> {
                overview["Note"] = "PKCS#12 keystore — protected by a passphrase. Subject details cannot be read without it."
                // Heuristic password-empty check is unreliable across providers; surface as INFO instead of warning.
                risks += RiskItem(
                    id = "cert-pkcs12",
                    severity = RiskSeverity.WARNING,
                    category = RiskCategory.SECURITY,
                    title = "PKCS#12 keystore",
                    description = "Contains certificates + private keys, encrypted with a passphrase.",
                    whyItMatters = "A leaked .p12/.pfx can unlock both the identity and its private key.",
                    recommendedAction = "Treat this file like a password file. Do not share or commit it.",
                    detector = "CertificateInspector"
                )
            }
            "jks", "keystore" -> {
                overview["Note"] = "Java KeyStore — protected by a passphrase. Use keytool to enumerate entries."
                risks += RiskItem(
                    id = "cert-jks",
                    severity = RiskSeverity.WARNING,
                    category = RiskCategory.SECURITY,
                    title = "Java KeyStore",
                    description = "Stores certificates and private keys for the JVM.",
                    whyItMatters = "JKS files contain signing keys. Exposure allows impersonation or app re-signing.",
                    recommendedAction = "Store keystores outside of source control with a strong passphrase.",
                    detector = "CertificateInspector"
                )
            }
            "ppk" -> {
                overview["Note"] = "PuTTY private key — convert via PuTTYgen for OpenSSH use."
                risks += RiskItem(
                    id = "cert-ppk",
                    severity = RiskSeverity.DANGER,
                    category = RiskCategory.SECURITY,
                    title = "PuTTY private key",
                    description = "Contains an SSH private key in PuTTY format.",
                    whyItMatters = "Anyone with this file (and the optional passphrase) can connect as you.",
                    recommendedAction = "Keep the key offline; rotate if it has been shared.",
                    detector = "CertificateInspector"
                )
            }
            "key" -> {
                overview["Note"] = "Binary key file — unable to parse as X.509."
                risks += RiskItem(
                    id = "cert-binary-key",
                    severity = RiskSeverity.WARNING,
                    category = RiskCategory.SECURITY,
                    title = "Private key material",
                    description = "Binary key file (not X.509 / not PEM).",
                    whyItMatters = "Private keys grant impersonation if leaked.",
                    recommendedAction = "Do not share this file. Confirm its purpose before keeping it on a shared device.",
                    detector = "CertificateInspector"
                )
            }
            "pub" -> {
                // SSH public key is text-shaped; we shouldn't normally arrive here.
                overview["Note"] = "Binary .pub — expected text. May be corrupted."
            }
            else -> {
                overview["Note"] = "Unrecognised certificate / key container."
            }
        }
        overview["Size"] = formatSize(bytes.size.toLong())
    }

    private fun describeCertificate(
        cert: X509Certificate,
        overview: MutableMap<String, String>,
        metadata: MutableMap<String, String>,
        risks: MutableList<RiskItem>
    ) {
        overview["Subject"] = shortName(cert.subjectX500Principal.name)
        overview["Issuer"] = shortName(cert.issuerX500Principal.name)
        overview["Serial"] = cert.serialNumber.toString(16).uppercase().chunked(2).joinToString(":")
        overview["Valid from"] = formatDate(cert.notBefore)
        overview["Valid until"] = formatDate(cert.notAfter)

        val now = Date()
        val expired = cert.notAfter.before(now)
        val notYetValid = cert.notBefore.after(now)
        val daysLeft = ((cert.notAfter.time - now.time) / (1000L * 60 * 60 * 24))

        when {
            expired -> risks += RiskItem(
                id = "cert-expired",
                severity = RiskSeverity.DANGER,
                category = RiskCategory.SECURITY,
                title = "Certificate has expired",
                description = "Expired ${formatDate(cert.notAfter)}.",
                whyItMatters = "Expired certificates are no longer trusted. Servers using them fail validation.",
                recommendedAction = "Renew the certificate before relying on it.",
                detector = "CertificateInspector"
            )
            notYetValid -> risks += RiskItem(
                id = "cert-not-yet-valid",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.SECURITY,
                title = "Certificate not yet valid",
                description = "Becomes valid ${formatDate(cert.notBefore)}.",
                whyItMatters = "Clients will reject the certificate until the start date passes.",
                recommendedAction = "Wait until the validity period begins, or fix the system clock.",
                detector = "CertificateInspector"
            )
            daysLeft in 0..30 -> risks += RiskItem(
                id = "cert-expiring-soon",
                severity = RiskSeverity.WARNING,
                category = RiskCategory.SECURITY,
                title = "Certificate expires soon",
                description = "$daysLeft day(s) until expiry.",
                whyItMatters = "Outages happen when certificates lapse without rotation.",
                recommendedAction = "Schedule renewal before ${formatDate(cert.notAfter)}.",
                detector = "CertificateInspector"
            )
        }

        cert.sigAlgName?.let { algo ->
            metadata["Signature algorithm"] = algo
            val upper = algo.uppercase(Locale.ROOT)
            if (upper.contains("MD5") || upper.contains("MD2") || upper.startsWith("SHA1") || upper.contains("WITHSHA1")) {
                risks += RiskItem(
                    id = "cert-weak-signature",
                    severity = RiskSeverity.WARNING,
                    category = RiskCategory.SECURITY,
                    title = "Weak signature algorithm",
                    description = "Signed with $algo.",
                    whyItMatters = "MD5 and SHA-1 are vulnerable to collision attacks and are no longer trusted by modern clients.",
                    recommendedAction = "Re-issue the certificate with SHA-256 or stronger.",
                    detector = "CertificateInspector"
                )
            }
        }

        when (val key = cert.publicKey) {
            is RSAPublicKey -> {
                val bits = key.modulus.bitLength()
                metadata["Public key"] = "RSA $bits bits"
                if (bits < 2048) {
                    risks += RiskItem(
                        id = "cert-weak-rsa",
                        severity = RiskSeverity.WARNING,
                        category = RiskCategory.SECURITY,
                        title = "RSA key shorter than 2048 bits",
                        description = "Public key length: $bits bits.",
                        whyItMatters = "Keys shorter than 2048 bits no longer provide adequate security against modern attackers.",
                        recommendedAction = "Re-issue with at least RSA-2048 or switch to ECDSA P-256+.",
                        detector = "CertificateInspector"
                    )
                }
            }
            is ECPublicKey -> {
                val bits = key.params?.curve?.field?.fieldSize ?: -1
                metadata["Public key"] = if (bits > 0) "EC $bits bits" else "EC"
                if (bits in 1..255) {
                    risks += RiskItem(
                        id = "cert-weak-ec",
                        severity = RiskSeverity.WARNING,
                        category = RiskCategory.SECURITY,
                        title = "EC curve smaller than P-256",
                        description = "EC field size: $bits bits.",
                        whyItMatters = "Sub-256-bit curves provide less than the 128-bit security commonly required today.",
                        recommendedAction = "Use P-256, P-384, or P-521.",
                        detector = "CertificateInspector"
                    )
                }
            }
            else -> {
                cert.publicKey?.algorithm?.let { metadata["Public key"] = it }
            }
        }

        metadata["SHA-256 fingerprint"] = sha256Fingerprint(cert.encoded)
        metadata["SHA-1 fingerprint"] = sha1Fingerprint(cert.encoded)

        val sans = runCatching { cert.subjectAlternativeNames }.getOrNull()
        if (!sans.isNullOrEmpty()) {
            val names = sans.mapNotNull { entry ->
                val list = entry as? List<*> ?: return@mapNotNull null
                list.getOrNull(1)?.toString()
            }
            if (names.isNotEmpty()) {
                overview["SANs"] = names.size.toString()
                metadata["Subject alternative names"] = names.take(10).joinToString("\n") +
                    if (names.size > 10) "\n… ${names.size - 10} more" else ""
            }
        }

        val selfSigned = cert.subjectX500Principal == cert.issuerX500Principal
        if (selfSigned) {
            overview["Self-signed"] = "Yes"
            risks += RiskItem(
                id = "cert-self-signed",
                severity = RiskSeverity.INFO,
                category = RiskCategory.SECURITY,
                title = "Self-signed certificate",
                description = "Subject and issuer match.",
                whyItMatters = "Self-signed certificates are not trusted by default. Common for development, but inappropriate for public services.",
                recommendedAction = "Confirm the deployment context. For public hosts, use a CA-issued certificate.",
                detector = "CertificateInspector"
            )
        }
    }

    private fun parseCertificate(base64: String): X509Certificate {
        val cleaned = base64.replace("\\s+".toRegex(), "")
        val der = android.util.Base64.decode(cleaned, android.util.Base64.DEFAULT)
        val factory = CertificateFactory.getInstance("X.509")
        return factory.generateCertificate(ByteArrayInputStream(der)) as? X509Certificate
            ?: throw CertificateException("Not an X.509 certificate")
    }

    private fun shortName(dn: String): String {
        val cn = Regex("""CN=([^,]+)""").find(dn)?.groupValues?.get(1)?.trim()
        return cn ?: dn
    }

    private fun formatDate(date: Date): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm 'UTC'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(date)
    }

    private fun sha256Fingerprint(bytes: ByteArray): String =
        hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    private fun sha1Fingerprint(bytes: ByteArray): String =
        hex(MessageDigest.getInstance("SHA-1").digest(bytes))

    private fun hex(bytes: ByteArray): String =
        bytes.joinToString(":") { "%02X".format(it) }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        return "%.1f MB".format(kb / 1024.0)
    }

    companion object {
        private val pemBlockRegex = Regex(
            """-----BEGIN ([A-Z0-9 ]+)-----\s*([A-Za-z0-9+/=\s]+?)-----END \1-----""",
            RegexOption.DOT_MATCHES_ALL
        )
    }
}
