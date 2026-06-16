package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Photoshop / Large-Document file header: "8BPS" + version (1=PSD, 2=PSB),
 * 6 reserved bytes, channel count, height, width, bit-depth, color mode.
 * We only read the 26-byte header — layer parsing is out of scope.
 */
@Singleton
class PsdInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.IMAGE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        try {
            FileInputStream(file).use { stream ->
                val header = ByteArray(26)
                if (stream.read(header) < 26) {
                    overview["Error"] = "Truncated Photoshop header"
                    return@use
                }
                val magic = String(header, 0, 4, Charsets.US_ASCII)
                if (magic != "8BPS") {
                    overview["Error"] = "Not a Photoshop file"
                    return@use
                }
                val version = readUInt16(header, 4)
                val channels = readUInt16(header, 12)
                val height = readUInt32(header, 14).toInt()
                val width = readUInt32(header, 18).toInt()
                val depth = readUInt16(header, 22)
                val colorMode = readUInt16(header, 24)

                overview["Format"] = if (version == 2) "Photoshop Large (PSB)" else "Photoshop (PSD)"
                overview["Resolution"] = "${width}×${height}"
                overview["Megapixels"] = "%.1f MP".format(width.toLong() * height / 1_000_000.0)
                overview["Channels"] = channels.toString()
                overview["Bit depth"] = "$depth bpp"
                overview["Color mode"] = colorModeName(colorMode)
                overview["Size"] = formatSize(file.length())
            }
        } catch (e: Exception) {
            overview["Error"] = "Failed to read PSD: ${e.message?.take(120)}"
        }
        InspectorResult(overview = overview)
    }

    private fun colorModeName(code: Int): String = when (code) {
        0 -> "Bitmap"
        1 -> "Grayscale"
        2 -> "Indexed"
        3 -> "RGB"
        4 -> "CMYK"
        7 -> "Multichannel"
        8 -> "Duotone"
        9 -> "Lab"
        else -> "Mode $code"
    }

    private fun readUInt16(buf: ByteArray, offset: Int): Int =
        ((buf[offset].toInt() and 0xFF) shl 8) or (buf[offset + 1].toInt() and 0xFF)

    private fun readUInt32(buf: ByteArray, offset: Int): Long =
        (((buf[offset].toInt() and 0xFF).toLong() shl 24) or
            ((buf[offset + 1].toInt() and 0xFF).toLong() shl 16) or
            ((buf[offset + 2].toInt() and 0xFF).toLong() shl 8) or
            (buf[offset + 3].toInt() and 0xFF).toLong()) and 0xFFFFFFFFL

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        return "%.1f MB".format(kb / 1024.0)
    }
}
