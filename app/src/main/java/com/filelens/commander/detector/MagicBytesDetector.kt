package com.filelens.commander.detector

import com.filelens.commander.domain.model.FileCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

data class DetectionResult(
    val detectedType: String,
    val category: FileCategory,
    val mimeType: String?,
    val confidence: Float,
    val extensionMismatch: Boolean,
    val declaredExtension: String
)

@Singleton
class MagicBytesDetector @Inject constructor() {

    private data class Signature(
        val bytes: ByteArray,
        val offset: Int = 0,
        val typeName: String,
        val category: FileCategory,
        val mimeType: String?,
        val extensions: Set<String>
    ) {
        override fun equals(other: Any?): Boolean = this === other
        override fun hashCode(): Int = typeName.hashCode()
    }

    private val signatures = listOf(
        // Images
        Signature(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()), 0, "JPEG", FileCategory.IMAGE, "image/jpeg", setOf("jpg", "jpeg", "jpe", "jfif")),
        Signature(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), 0, "PNG", FileCategory.IMAGE, "image/png", setOf("png")),
        Signature(byteArrayOf(0x47, 0x49, 0x46, 0x38), 0, "GIF", FileCategory.IMAGE, "image/gif", setOf("gif")),
        Signature(byteArrayOf(0x42, 0x4D), 0, "BMP", FileCategory.IMAGE, "image/bmp", setOf("bmp")),
        Signature(byteArrayOf(0x52, 0x49, 0x46, 0x46), 0, "RIFF/WEBP", FileCategory.IMAGE, "image/webp", setOf("webp")),
        Signature(byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69), 0, "HEIF", FileCategory.IMAGE, "image/heif", setOf("heic", "heif")),

        // PDF
        Signature(byteArrayOf(0x25, 0x50, 0x44, 0x46), 0, "PDF", FileCategory.PDF, "application/pdf", setOf("pdf")),

        // Archives
        Signature(byteArrayOf(0x50, 0x4B, 0x03, 0x04), 0, "ZIP", FileCategory.ARCHIVE, "application/zip", setOf("zip", "jar", "war", "ear", "docx", "xlsx", "pptx", "odt", "ods", "odp", "apk", "aab", "epub")),
        Signature(byteArrayOf(0x50, 0x4B, 0x05, 0x06), 0, "ZIP (empty)", FileCategory.ARCHIVE, "application/zip", setOf("zip")),
        Signature(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07), 0, "RAR", FileCategory.ARCHIVE, "application/x-rar-compressed", setOf("rar")),
        Signature(byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C), 0, "7-Zip", FileCategory.ARCHIVE, "application/x-7z-compressed", setOf("7z")),
        Signature(byteArrayOf(0x1F, 0x8B.toByte()), 0, "GZIP", FileCategory.ARCHIVE, "application/gzip", setOf("gz", "tgz")),
        Signature(byteArrayOf(0x42, 0x5A, 0x68), 0, "BZIP2", FileCategory.ARCHIVE, "application/x-bzip2", setOf("bz2")),
        Signature(byteArrayOf(0xFD.toByte(), 0x37, 0x7A, 0x58, 0x5A, 0x00), 0, "XZ", FileCategory.ARCHIVE, "application/x-xz", setOf("xz")),
        Signature(byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte()), 0, "Zstandard", FileCategory.ARCHIVE, "application/zstd", setOf("zst")),

        // Database
        Signature(byteArrayOf(0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66, 0x6F, 0x72, 0x6D, 0x61, 0x74), 0, "SQLite", FileCategory.DATABASE, "application/x-sqlite3", setOf("db", "sqlite", "sqlite3")),

        // Executables
        Signature(byteArrayOf(0x7F, 0x45, 0x4C, 0x46), 0, "ELF", FileCategory.EXECUTABLE, "application/x-elf", setOf("so", "elf", "")),
        Signature(byteArrayOf(0x4D, 0x5A), 0, "PE/MZ", FileCategory.EXECUTABLE, "application/x-dosexec", setOf("exe", "dll", "msi")),
        Signature(byteArrayOf(0x64, 0x65, 0x78, 0x0A), 0, "DEX", FileCategory.EXECUTABLE, "application/x-dex", setOf("dex")),

        // Audio
        Signature(byteArrayOf(0x49, 0x44, 0x33), 0, "MP3 (ID3)", FileCategory.AUDIO, "audio/mpeg", setOf("mp3")),
        Signature(byteArrayOf(0xFF.toByte(), 0xFB.toByte()), 0, "MP3", FileCategory.AUDIO, "audio/mpeg", setOf("mp3")),
        Signature(byteArrayOf(0x66, 0x4C, 0x61, 0x43), 0, "FLAC", FileCategory.AUDIO, "audio/flac", setOf("flac")),
        Signature(byteArrayOf(0x4F, 0x67, 0x67, 0x53), 0, "OGG", FileCategory.AUDIO, "audio/ogg", setOf("ogg", "oga", "ogv")),

        // Video/containers
        Signature(byteArrayOf(0x00, 0x00, 0x00), 0, "MP4/MOV", FileCategory.VIDEO, "video/mp4", setOf("mp4", "m4v", "m4a", "mov", "3gp")),
        Signature(byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()), 0, "Matroska/WebM", FileCategory.VIDEO, "video/x-matroska", setOf("mkv", "webm")),

        // Fonts
        Signature(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0x00), 0, "TrueType", FileCategory.FONT, "font/ttf", setOf("ttf")),
        Signature(byteArrayOf(0x4F, 0x54, 0x54, 0x4F), 0, "OpenType", FileCategory.FONT, "font/otf", setOf("otf")),
        Signature(byteArrayOf(0x77, 0x4F, 0x46, 0x46), 0, "WOFF", FileCategory.FONT, "font/woff", setOf("woff")),
        Signature(byteArrayOf(0x77, 0x4F, 0x46, 0x32), 0, "WOFF2", FileCategory.FONT, "font/woff2", setOf("woff2")),

        // Office legacy
        Signature(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()), 0, "OLE2 (MS Office legacy)", FileCategory.DOCUMENT, "application/msword", setOf("doc", "xls", "ppt", "msg")),

        // WASM
        Signature(byteArrayOf(0x00, 0x61, 0x73, 0x6D), 0, "WebAssembly", FileCategory.EXECUTABLE, "application/wasm", setOf("wasm"))
    )

    suspend fun detect(file: File): DetectionResult = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        val header = readHeader(file, 64)

        if (header == null) {
            return@withContext DetectionResult(
                detectedType = "Unknown",
                category = FileCategory.UNKNOWN,
                mimeType = null,
                confidence = 0f,
                extensionMismatch = false,
                declaredExtension = extension
            )
        }

        for (sig in signatures) {
            if (matchesSignature(header, sig)) {
                val mismatch = extension.isNotEmpty() && extension !in sig.extensions
                val actualCategory = refineCategory(file, sig, extension)
                return@withContext DetectionResult(
                    detectedType = sig.typeName,
                    category = actualCategory,
                    mimeType = sig.mimeType,
                    confidence = 0.95f,
                    extensionMismatch = mismatch,
                    declaredExtension = extension
                )
            }
        }

        val textResult = detectTextFile(header, file, extension)
        if (textResult != null) return@withContext textResult

        DetectionResult(
            detectedType = "Unknown binary",
            category = FileCategory.UNKNOWN,
            mimeType = "application/octet-stream",
            confidence = 0.1f,
            extensionMismatch = false,
            declaredExtension = extension
        )
    }

    private fun refineCategory(file: File, sig: Signature, extension: String): FileCategory {
        if (sig.typeName == "ZIP" || sig.typeName == "ZIP (empty)") {
            return when (extension) {
                "docx" -> FileCategory.DOCUMENT
                "xlsx" -> FileCategory.SPREADSHEET
                "pptx" -> FileCategory.PRESENTATION
                "odt", "ods", "odp" -> FileCategory.DOCUMENT
                "jar", "war", "ear" -> FileCategory.ARCHIVE
                "apk", "aab" -> FileCategory.APK
                "epub" -> FileCategory.DOCUMENT
                else -> FileCategory.ARCHIVE
            }
        }
        if (sig.typeName == "RIFF/WEBP") {
            return when (extension) {
                "avi" -> FileCategory.VIDEO
                "wav" -> FileCategory.AUDIO
                else -> FileCategory.IMAGE
            }
        }
        if (sig.typeName == "OGG") {
            return when (extension) {
                "ogv" -> FileCategory.VIDEO
                else -> FileCategory.AUDIO
            }
        }
        return sig.category
    }

    private fun matchesSignature(header: ByteArray, sig: Signature): Boolean {
        if (header.size < sig.offset + sig.bytes.size) return false
        for (i in sig.bytes.indices) {
            if (header[sig.offset + i] != sig.bytes[i]) return false
        }
        return true
    }

    private fun detectTextFile(header: ByteArray, file: File, extension: String): DetectionResult? {
        val isLikelyText = header.all { byte ->
            val b = byte.toInt() and 0xFF
            b in 0x09..0x0D || b in 0x20..0x7E || b >= 0x80
        }

        if (!isLikelyText) return null

        val hasUtf8Bom = header.size >= 3 &&
            header[0] == 0xEF.toByte() && header[1] == 0xBB.toByte() && header[2] == 0xBF.toByte()
        val hasUtf16BeBom = header.size >= 2 && header[0] == 0xFE.toByte() && header[1] == 0xFF.toByte()
        val hasUtf16LeBom = header.size >= 2 && header[0] == 0xFF.toByte() && header[1] == 0xFE.toByte()

        val content = if (hasUtf8Bom) String(header, 3, minOf(header.size - 3, 50), Charsets.UTF_8)
        else String(header, 0, minOf(header.size, 50), Charsets.UTF_8)

        val (type, category, mime) = when {
            content.trimStart().startsWith("{") || content.trimStart().startsWith("[") ->
                Triple("JSON", FileCategory.TEXT, "application/json")
            content.trimStart().startsWith("<?xml") || content.trimStart().startsWith("<") ->
                Triple("XML", FileCategory.TEXT, "application/xml")
            content.trimStart().startsWith("---") || content.contains(Regex("^\\w+:\\s", RegexOption.MULTILINE)) ->
                Triple("YAML", FileCategory.TEXT, "application/yaml")
            content.trimStart().startsWith("#!") ->
                Triple("Script", FileCategory.CODE, "text/x-script")
            else ->
                Triple("Text", FileCategory.TEXT, "text/plain")
        }

        val codeExtensions = setOf("kt", "kts", "java", "py", "js", "ts", "jsx", "tsx", "c", "cpp", "h", "hpp",
            "rs", "go", "rb", "php", "swift", "scala", "groovy", "dart", "sh", "bash", "zsh",
            "html", "css", "scss", "less", "sql", "graphql", "proto", "r", "lua", "pl", "ex", "exs")
        val actualCategory = if (extension in codeExtensions) FileCategory.CODE else category

        return DetectionResult(
            detectedType = type,
            category = actualCategory,
            mimeType = mime,
            confidence = 0.7f,
            extensionMismatch = false,
            declaredExtension = extension
        )
    }

    private fun readHeader(file: File, size: Int): ByteArray? {
        return try {
            FileInputStream(file).use { stream ->
                val buffer = ByteArray(size)
                val bytesRead = stream.read(buffer)
                if (bytesRead <= 0) null
                else if (bytesRead < size) buffer.copyOf(bytesRead)
                else buffer
            }
        } catch (_: Exception) {
            null
        }
    }
}
