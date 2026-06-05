package com.filelens.commander.domain.model

enum class RemoteProtocol(val displayName: String, val defaultPort: Int) {
    FTP("FTP", 21),
    SFTP("SFTP", 22),
    SMB("SMB", 445)
}

/**
 * A saved remote-server connection. Credentials are stored in plain text in
 * DataStore for now; a follow-up should move them behind the Android Keystore
 * (EncryptedSharedPreferences or similar).
 *
 * For SMB, [shareName] is the share to mount under the host (e.g. "Public").
 * For FTP/SFTP it should be left blank.
 */
data class RemoteConnection(
    val id: String,
    val name: String,
    val protocol: RemoteProtocol,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val basePath: String,
    val shareName: String,
    val anonymous: Boolean,
    val ftpPassive: Boolean,
    val createdAt: Long,
    /**
     * Pinned SFTP host key in `keyType base64key` form. Empty means the next
     * connect runs in trust-on-first-use mode and will save whatever it sees.
     */
    val expectedHostKey: String = ""
) {
    val effectivePort: Int get() = if (port > 0) port else protocol.defaultPort

    val rootPath: String
        get() {
            val userPart = if (username.isNotBlank() && !anonymous) "$username@" else ""
            val portPart = if (port > 0 && port != protocol.defaultPort) ":$port" else ""
            return when (protocol) {
                RemoteProtocol.FTP -> "ftp://$userPart$host$portPart${normalizedBase("/")}"
                RemoteProtocol.SFTP -> "sftp://$userPart$host$portPart${normalizedBase("/")}"
                RemoteProtocol.SMB -> {
                    val share = shareName.trim('/')
                    val base = normalizedBase("")
                    "smb://$userPart$host$portPart/${share}$base".trimEnd('/')
                }
            }
        }

    private fun normalizedBase(default: String): String {
        val trimmed = basePath.trim()
        if (trimmed.isEmpty() || trimmed == "/") return default
        val withSlash = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return withSlash.trimEnd('/')
    }
}
