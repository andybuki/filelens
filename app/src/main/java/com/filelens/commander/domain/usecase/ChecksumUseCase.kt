package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.FileChecksum
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChecksumUseCase @Inject constructor() {

    suspend fun compute(file: File, includeMd5: Boolean = true, includeSha256: Boolean = true): FileChecksum =
        withContext(Dispatchers.IO) {
            if (!file.isFile || !file.canRead()) return@withContext FileChecksum(null, null)
            val md5Digest = if (includeMd5) MessageDigest.getInstance("MD5") else null
            val shaDigest = if (includeSha256) MessageDigest.getInstance("SHA-256") else null
            file.inputStream().use { input ->
                val buf = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    md5Digest?.update(buf, 0, n)
                    shaDigest?.update(buf, 0, n)
                }
            }
            FileChecksum(
                md5 = md5Digest?.digest()?.toHex(),
                sha256 = shaDigest?.digest()?.toHex()
            )
        }

    suspend fun quickSha256(file: File): String? = withContext(Dispatchers.IO) {
        if (!file.isFile || !file.canRead()) return@withContext null
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String {
        val sb = StringBuilder(size * 2)
        for (b in this) {
            sb.append(HEX[(b.toInt() ushr 4) and 0x0F])
            sb.append(HEX[b.toInt() and 0x0F])
        }
        return sb.toString()
    }

    private companion object {
        private val HEX = "0123456789abcdef".toCharArray()
    }
}
