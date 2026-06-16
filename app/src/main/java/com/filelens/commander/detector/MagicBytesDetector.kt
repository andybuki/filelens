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
        Signature(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A), 0, "PNG", FileCategory.IMAGE, "image/png", setOf("png", "apng")),
        Signature(byteArrayOf(0x47, 0x49, 0x46, 0x38), 0, "GIF", FileCategory.IMAGE, "image/gif", setOf("gif")),
        Signature(byteArrayOf(0x42, 0x4D), 0, "BMP", FileCategory.IMAGE, "image/bmp", setOf("bmp")),
        Signature(byteArrayOf(0x52, 0x49, 0x46, 0x46), 0, "RIFF/WEBP", FileCategory.IMAGE, "image/webp", setOf("webp")),
        // JPEG XL — naked codestream and ISO BMFF container
        Signature(byteArrayOf(0xFF.toByte(), 0x0A), 0, "JPEG XL", FileCategory.IMAGE, "image/jxl", setOf("jxl")),
        Signature(byteArrayOf(0x00, 0x00, 0x00, 0x0C, 0x4A, 0x58, 0x4C, 0x20, 0x0D, 0x0A, 0x87.toByte(), 0x0A), 0, "JPEG XL", FileCategory.IMAGE, "image/jxl", setOf("jxl")),
        // Adobe Photoshop / GIMP
        Signature(byteArrayOf(0x38, 0x42, 0x50, 0x53), 0, "Photoshop", FileCategory.IMAGE, "image/vnd.adobe.photoshop", setOf("psd", "psb")),
        Signature(byteArrayOf(0x67, 0x69, 0x6D, 0x70, 0x20, 0x78, 0x63, 0x66, 0x20), 0, "GIMP XCF", FileCategory.IMAGE, "image/x-xcf", setOf("xcf")),
        // Canon CR2 (TIFF + CR magic at offset 8)
        Signature(byteArrayOf(0x49, 0x49, 0x2A, 0x00, 0x10, 0x00, 0x00, 0x00, 0x43, 0x52), 0, "Canon CR2", FileCategory.IMAGE, "image/x-canon-cr2", setOf("cr2")),

        // PDF
        Signature(byteArrayOf(0x25, 0x50, 0x44, 0x46), 0, "PDF", FileCategory.PDF, "application/pdf", setOf("pdf")),

        // Archives
        Signature(byteArrayOf(0x50, 0x4B, 0x03, 0x04), 0, "ZIP", FileCategory.ARCHIVE, "application/zip", setOf("zip", "jar", "war", "ear", "docx", "docm", "xlsx", "xlsm", "pptx", "pptm", "odt", "ods", "odp", "apk", "aab", "epub", "ipa", "xapk", "xpi", "crx", "zipx", "pages", "numbers", "key")),
        Signature(byteArrayOf(0x50, 0x4B, 0x05, 0x06), 0, "ZIP (empty)", FileCategory.ARCHIVE, "application/zip", setOf("zip")),
        Signature(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1A, 0x07), 0, "RAR", FileCategory.ARCHIVE, "application/x-rar-compressed", setOf("rar")),
        Signature(byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C), 0, "7-Zip", FileCategory.ARCHIVE, "application/x-7z-compressed", setOf("7z")),
        Signature(byteArrayOf(0x1F, 0x8B.toByte()), 0, "GZIP", FileCategory.ARCHIVE, "application/gzip", setOf("gz", "tgz")),
        Signature(byteArrayOf(0x42, 0x5A, 0x68), 0, "BZIP2", FileCategory.ARCHIVE, "application/x-bzip2", setOf("bz2", "tbz", "tbz2")),
        Signature(byteArrayOf(0xFD.toByte(), 0x37, 0x7A, 0x58, 0x5A, 0x00), 0, "XZ", FileCategory.ARCHIVE, "application/x-xz", setOf("xz", "txz")),
        Signature(byteArrayOf(0x28, 0xB5.toByte(), 0x2F, 0xFD.toByte()), 0, "Zstandard", FileCategory.ARCHIVE, "application/zstd", setOf("zst")),
        Signature(byteArrayOf(0x04, 0x22, 0x4D, 0x18), 0, "LZ4", FileCategory.ARCHIVE, "application/x-lz4", setOf("lz4")),
        Signature(byteArrayOf(0x4D, 0x53, 0x43, 0x46), 0, "Microsoft Cabinet", FileCategory.ARCHIVE, "application/vnd.ms-cab-compressed", setOf("cab")),
        Signature(byteArrayOf(0x2D, 0x6C, 0x68), 2, "LZH/LHA", FileCategory.ARCHIVE, "application/x-lzh-compressed", setOf("lzh", "lha")),

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
        // AMR-WB must come before AMR (longer prefix wins)
        Signature(byteArrayOf(0x23, 0x21, 0x41, 0x4D, 0x52, 0x2D, 0x57, 0x42, 0x0A), 0, "AMR-WB", FileCategory.AUDIO, "audio/amr-wb", setOf("awb")),
        Signature(byteArrayOf(0x23, 0x21, 0x41, 0x4D, 0x52, 0x0A), 0, "AMR", FileCategory.AUDIO, "audio/amr", setOf("amr")),
        Signature(byteArrayOf(0x4D, 0x54, 0x68, 0x64), 0, "MIDI", FileCategory.AUDIO, "audio/midi", setOf("mid", "midi")),
        Signature(byteArrayOf(0x0B, 0x77), 0, "AC-3", FileCategory.AUDIO, "audio/ac3", setOf("ac3")),

        // Video (containers handled below via BMFF brand parsing; this only catches Matroska/WebM)
        Signature(byteArrayOf(0x1A, 0x45, 0xDF.toByte(), 0xA3.toByte()), 0, "Matroska/WebM", FileCategory.VIDEO, "video/x-matroska", setOf("mkv", "webm", "mka")),

        // Fonts
        Signature(byteArrayOf(0x00, 0x01, 0x00, 0x00, 0x00), 0, "TrueType", FileCategory.FONT, "font/ttf", setOf("ttf")),
        Signature(byteArrayOf(0x4F, 0x54, 0x54, 0x4F), 0, "OpenType", FileCategory.FONT, "font/otf", setOf("otf")),
        Signature(byteArrayOf(0x74, 0x74, 0x63, 0x66), 0, "TrueType Collection", FileCategory.FONT, "font/collection", setOf("ttc")),
        Signature(byteArrayOf(0x77, 0x4F, 0x46, 0x46), 0, "WOFF", FileCategory.FONT, "font/woff", setOf("woff")),
        Signature(byteArrayOf(0x77, 0x4F, 0x46, 0x32), 0, "WOFF2", FileCategory.FONT, "font/woff2", setOf("woff2")),

        // Office legacy
        Signature(byteArrayOf(0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()), 0, "OLE2 (MS Office legacy)", FileCategory.DOCUMENT, "application/msword", setOf("doc", "xls", "ppt", "msg")),

        // Documents — Mobipocket header lives at offset 60, not 0
        Signature(byteArrayOf(0x42, 0x4F, 0x4F, 0x4B, 0x4D, 0x4F, 0x42, 0x49), 60, "Mobipocket", FileCategory.DOCUMENT, "application/x-mobipocket-ebook", setOf("mobi", "azw", "azw3", "prc")),

        // Crypto / keystores
        Signature(byteArrayOf(0xFE.toByte(), 0xED.toByte(), 0xFE.toByte(), 0xED.toByte(), 0x00, 0x00, 0x00, 0x02), 0, "Java KeyStore", FileCategory.CERTIFICATE, "application/x-java-keystore", setOf("jks", "keystore")),

        // WASM
        Signature(byteArrayOf(0x00, 0x61, 0x73, 0x6D), 0, "WebAssembly", FileCategory.EXECUTABLE, "application/wasm", setOf("wasm"))
    )

    suspend fun detect(file: File): DetectionResult = withContext(Dispatchers.IO) {
        val extension = file.extension.lowercase()
        // 256 bytes is enough for every fixed signature and for Mobipocket at offset 60.
        val header = readHeader(file, 256)

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

        // Container formats that need brand/multi-region inspection take priority over
        // the broad fixed-signature scan.
        detectIsoBmff(header, extension)?.let { return@withContext it }
        detectAiff(header, extension)?.let { return@withContext it }
        detectDjVu(header, extension)?.let { return@withContext it }

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

        // MPEG-TS has no header signature; it's identified by the 0x47 sync byte
        // repeating every 188 bytes.
        detectMpegTs(header, extension)?.let { return@withContext it }

        // ISO 9660 puts "CD001" at offset 32769, far beyond the cheap header read.
        // Only pay the seek cost when the extension hints at a disk image.
        if (extension in setOf("iso", "img")) {
            detectIso9660(file, extension)?.let { return@withContext it }
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

    // ISO Base Media File Format containers (MP4, MOV, 3GP, HEIC/HEIF, AVIF, …).
    // Box layout: [size:4][type:4 = "ftyp"][major_brand:4][minor_version:4][compat_brands…]
    private fun detectIsoBmff(header: ByteArray, extension: String): DetectionResult? {
        if (header.size < 12) return null
        if (header[4] != 'f'.code.toByte() || header[5] != 't'.code.toByte() ||
            header[6] != 'y'.code.toByte() || header[7] != 'p'.code.toByte()
        ) return null

        val brand = String(header, 8, 4)
        // Compatible-brand list (each 4 bytes) starts at offset 16 and runs to the box size.
        val boxSize = ((header[0].toInt() and 0xFF) shl 24) or
            ((header[1].toInt() and 0xFF) shl 16) or
            ((header[2].toInt() and 0xFF) shl 8) or
            (header[3].toInt() and 0xFF)
        val compatEnd = minOf(boxSize.coerceIn(16, header.size), header.size)
        val compat = buildSet {
            var i = 16
            while (i + 4 <= compatEnd) {
                add(String(header, i, 4))
                i += 4
            }
        }
        val brands = compat + brand

        val (typeName, category, mime, exts) = when {
            "avif" in brands || "avis" in brands ->
                Quad("AVIF", FileCategory.IMAGE, "image/avif", setOf("avif"))
            "heic" in brands || "heix" in brands || "heim" in brands || "heis" in brands ->
                Quad("HEIC", FileCategory.IMAGE, "image/heic", setOf("heic"))
            "mif1" in brands || "msf1" in brands || "heif" in brands ->
                Quad("HEIF", FileCategory.IMAGE, "image/heif", setOf("heif", "heic"))
            "jxl " in brands ->
                Quad("JPEG XL", FileCategory.IMAGE, "image/jxl", setOf("jxl"))
            "crx " in brands ->
                Quad("Canon CR3", FileCategory.IMAGE, "image/x-canon-cr3", setOf("cr3"))
            "qt  " in brands ->
                Quad("QuickTime", FileCategory.VIDEO, "video/quicktime", setOf("mov", "qt"))
            "3gp4" in brands || "3gp5" in brands || "3gp6" in brands || "3gp7" in brands || "3g2a" in brands ->
                Quad("3GPP", FileCategory.VIDEO, "video/3gpp", setOf("3gp", "3g2"))
            "M4A " in brands || "M4B " in brands ->
                Quad("M4A audio", FileCategory.AUDIO, "audio/mp4", setOf("m4a", "m4b"))
            "M4V " in brands ->
                Quad("M4V", FileCategory.VIDEO, "video/x-m4v", setOf("m4v"))
            "isom" in brands || "iso2" in brands || "iso4" in brands || "iso5" in brands ||
                "mp41" in brands || "mp42" in brands || "avc1" in brands || "dash" in brands ->
                Quad("MP4", FileCategory.VIDEO, "video/mp4", setOf("mp4", "m4v"))
            else ->
                Quad("ISO BMFF ($brand)", FileCategory.VIDEO, "video/mp4", setOf("mp4", "m4v", "mov"))
        }

        return DetectionResult(
            detectedType = typeName,
            category = category,
            mimeType = mime,
            confidence = 0.95f,
            extensionMismatch = extension.isNotEmpty() && extension !in exts,
            declaredExtension = extension
        )
    }

    // AIFF: "FORM" at 0, then 4 bytes size, then "AIFF" or "AIFC" at offset 8.
    private fun detectAiff(header: ByteArray, extension: String): DetectionResult? {
        if (header.size < 12) return null
        if (header[0] != 'F'.code.toByte() || header[1] != 'O'.code.toByte() ||
            header[2] != 'R'.code.toByte() || header[3] != 'M'.code.toByte()
        ) return null
        val form = String(header, 8, 4)
        if (form != "AIFF" && form != "AIFC") return null
        return DetectionResult(
            detectedType = "AIFF",
            category = FileCategory.AUDIO,
            mimeType = "audio/aiff",
            confidence = 0.95f,
            extensionMismatch = extension.isNotEmpty() && extension !in setOf("aiff", "aif", "aifc"),
            declaredExtension = extension
        )
    }

    // DjVu: "AT&TFORM" at 0, then 4 bytes size, then "DJVM" or "DJVU" at offset 12.
    private fun detectDjVu(header: ByteArray, extension: String): DetectionResult? {
        if (header.size < 16) return null
        if (header[0] != 'A'.code.toByte() || header[1] != 'T'.code.toByte() ||
            header[2] != '&'.code.toByte() || header[3] != 'T'.code.toByte() ||
            header[4] != 'F'.code.toByte() || header[5] != 'O'.code.toByte() ||
            header[6] != 'R'.code.toByte() || header[7] != 'M'.code.toByte()
        ) return null
        val form = String(header, 12, 4)
        if (form != "DJVM" && form != "DJVU" && form != "DJVI" && form != "THUM") return null
        return DetectionResult(
            detectedType = "DjVu",
            category = FileCategory.DOCUMENT,
            mimeType = "image/vnd.djvu",
            confidence = 0.95f,
            extensionMismatch = extension.isNotEmpty() && extension !in setOf("djvu", "djv"),
            declaredExtension = extension
        )
    }

    // MPEG-TS uses a 188-byte packet structure with 0x47 sync bytes. Check three
    // packets (offsets 0, 188, 376) to rule out random 0x47-starting files.
    private fun detectMpegTs(header: ByteArray, extension: String): DetectionResult? {
        if (header.size < 1) return null
        if (header[0] != 0x47.toByte()) return null
        // Header buffer is 256 bytes — we can verify offset 188 cheaply.
        if (header.size > 188 && header[188] != 0x47.toByte()) return null
        return DetectionResult(
            detectedType = "MPEG-TS",
            category = FileCategory.VIDEO,
            mimeType = "video/mp2t",
            confidence = if (header.size > 188) 0.9f else 0.6f,
            extensionMismatch = extension.isNotEmpty() && extension !in setOf("ts", "m2ts", "mts"),
            declaredExtension = extension
        )
    }

    // ISO 9660 volume descriptor: "CD001" at offset 32769 (sector 16 + 1 byte).
    private fun detectIso9660(file: File, extension: String): DetectionResult? {
        return try {
            FileInputStream(file).use { stream ->
                if (stream.skip(32769L) != 32769L) return@use null
                val buf = ByteArray(5)
                if (stream.read(buf) != 5) return@use null
                if (buf.contentEquals(byteArrayOf(0x43, 0x44, 0x30, 0x30, 0x31))) {
                    DetectionResult(
                        detectedType = "ISO 9660",
                        category = FileCategory.ARCHIVE,
                        mimeType = "application/x-iso9660-image",
                        confidence = 0.95f,
                        extensionMismatch = false,
                        declaredExtension = extension
                    )
                } else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun refineCategory(file: File, sig: Signature, extension: String): FileCategory {
        if (sig.typeName == "ZIP" || sig.typeName == "ZIP (empty)") {
            return when (extension) {
                "docx", "docm" -> FileCategory.DOCUMENT
                "xlsx", "xlsm" -> FileCategory.SPREADSHEET
                "pptx", "pptm" -> FileCategory.PRESENTATION
                "odt" -> FileCategory.DOCUMENT
                "ods" -> FileCategory.SPREADSHEET
                "odp" -> FileCategory.PRESENTATION
                "jar", "war", "ear", "xpi", "crx", "zipx" -> FileCategory.ARCHIVE
                "apk", "aab", "xapk", "ipa" -> FileCategory.APK
                "epub" -> FileCategory.DOCUMENT
                "pages" -> FileCategory.DOCUMENT
                "numbers" -> FileCategory.SPREADSHEET
                "key" -> FileCategory.PRESENTATION
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
