package com.filelens.commander.inspector

import android.graphics.Typeface
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FontInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.FONT)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        try {
            overview["File size"] = formatSize(file.length())

            val header = ByteArray(minOf(file.length(), 12).toInt())
            RandomAccessFile(file, "r").use { raf ->
                raf.readFully(header)
            }

            val format = detectFormat(header)
            overview["Format"] = format

            if (format == "TTF" || format == "OTF") {
                parseSfntFont(file, overview, metadata)
            }

            try {
                Typeface.createFromFile(file)
                overview["Loadable"] = "Yes"
            } catch (_: Exception) {
                overview["Loadable"] = "No"
                risks.add(
                    RiskItem(
                        severity = RiskSeverity.WARNING,
                        title = "Font failed to load",
                        description = "Android Typeface could not load this font file. It may be corrupt or unsupported."
                    )
                )
            }
        } catch (e: Exception) {
            overview["Error"] = e.message ?: "Unknown error"
        }

        val sampleText = "The quick brown fox jumps over the lazy dog\n" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ\n" +
            "abcdefghijklmnopqrstuvwxyz\n" +
            "0123456789 !@#\$%^&*()"

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            previewContent = PreviewContent.TextContent(
                content = sampleText,
                language = null,
                encoding = null,
                lineCount = 4
            )
        )
    }

    private fun detectFormat(header: ByteArray): String {
        if (header.size < 4) return "Unknown"
        val magic = header.sliceArray(0..3)
        return when {
            magic.contentEquals(byteArrayOf(0x00, 0x01, 0x00, 0x00)) -> "TTF"
            magic.contentEquals(byteArrayOf('O'.code.toByte(), 'T'.code.toByte(), 'T'.code.toByte(), 'O'.code.toByte())) -> "OTF"
            magic.contentEquals(byteArrayOf('w'.code.toByte(), 'O'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte())) -> "WOFF"
            magic.contentEquals(byteArrayOf('w'.code.toByte(), 'O'.code.toByte(), 'F'.code.toByte(), '2'.code.toByte())) -> "WOFF2"
            magic.contentEquals(byteArrayOf('t'.code.toByte(), 'r'.code.toByte(), 'u'.code.toByte(), 'e'.code.toByte())) -> "TTF"
            else -> "Unknown"
        }
    }

    private fun parseSfntFont(file: File, overview: MutableMap<String, String>, metadata: MutableMap<String, String>) {
        RandomAccessFile(file, "r").use { raf ->
            if (raf.length() < 12) return

            raf.seek(4)
            val numTables = raf.readUnsignedShort()
            overview["Number of tables"] = numTables.toString()

            raf.seek(12)
            var nameTableOffset = -1L
            var nameTableLength = -1

            for (i in 0 until numTables) {
                val tablePos = 12L + i * 16
                if (tablePos + 16 > raf.length()) break
                raf.seek(tablePos)

                val tag = ByteArray(4)
                raf.readFully(tag)
                val tagStr = String(tag, Charsets.US_ASCII)

                if (tagStr == "name") {
                    raf.skipBytes(4) // checksum
                    nameTableOffset = raf.readInt().toLong() and 0xFFFFFFFFL
                    nameTableLength = raf.readInt()
                    break
                }
            }

            if (nameTableOffset >= 0 && nameTableOffset + 6 <= raf.length()) {
                parseNameTable(raf, nameTableOffset, nameTableLength, metadata)
            }
        }
    }

    private fun parseNameTable(raf: RandomAccessFile, offset: Long, length: Int, metadata: MutableMap<String, String>) {
        if (offset + 6 > raf.length()) return
        raf.seek(offset)

        val format = raf.readUnsignedShort()
        val count = raf.readUnsignedShort()
        val stringOffset = raf.readUnsignedShort()
        val storageOffset = offset + stringOffset

        for (i in 0 until count) {
            val recordPos = offset + 6 + i * 12
            if (recordPos + 12 > raf.length()) break
            raf.seek(recordPos)

            val platformID = raf.readUnsignedShort()
            val encodingID = raf.readUnsignedShort()
            val languageID = raf.readUnsignedShort()
            val nameID = raf.readUnsignedShort()
            val nameLength = raf.readUnsignedShort()
            val nameOffset = raf.readUnsignedShort()

            if (nameID != 1 && nameID != 2) continue

            val strPos = storageOffset + nameOffset
            if (strPos + nameLength > raf.length()) continue

            raf.seek(strPos)
            val nameBytes = ByteArray(nameLength)
            raf.readFully(nameBytes)

            val charset = if (platformID == 3 || platformID == 0) {
                Charsets.UTF_16BE
            } else {
                Charsets.US_ASCII
            }
            val nameStr = String(nameBytes, charset).trim()
            if (nameStr.isEmpty()) continue

            when (nameID) {
                1 -> metadata.putIfAbsent("Font family", nameStr)
                2 -> metadata.putIfAbsent("Font style", nameStr)
            }
        }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}
