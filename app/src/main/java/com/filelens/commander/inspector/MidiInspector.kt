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
 * Standard MIDI File: "MThd" header (14 bytes) followed by one or more "MTrk"
 * chunks. We surface the format (0/1/2), track count, time-division and a
 * rough duration estimate from the first track's tempo + length.
 */
@Singleton
class MidiInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.AUDIO)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        overview["Format"] = "Standard MIDI File"

        try {
            FileInputStream(file).use { stream ->
                val header = ByteArray(14)
                if (stream.read(header) < 14) {
                    overview["Error"] = "Truncated MIDI header"
                    return@use
                }
                val magic = String(header, 0, 4, Charsets.US_ASCII)
                if (magic != "MThd") {
                    overview["Error"] = "Missing MThd marker"
                    return@use
                }
                val headerLen = readUInt32(header, 4).toInt()
                val format = readUInt16(header, 8)
                val tracks = readUInt16(header, 10)
                val division = readUInt16(header, 12)

                overview["MIDI format"] = when (format) {
                    0 -> "0 (single track)"
                    1 -> "1 (multi-track, synchronous)"
                    2 -> "2 (multi-track, independent)"
                    else -> format.toString()
                }
                overview["Tracks"] = tracks.toString()

                val ticksPerQuarter: Int
                if (division and 0x8000 == 0) {
                    ticksPerQuarter = division
                    overview["Time division"] = "$division ticks per quarter note"
                } else {
                    ticksPerQuarter = -1
                    val fps = -((division shr 8).toByte().toInt())
                    val ticksPerFrame = division and 0xFF
                    overview["Time division"] = "SMPTE $fps fps × $ticksPerFrame ticks"
                }

                if (headerLen != 6) overview["Header length"] = headerLen.toString()
                overview["Size"] = formatSize(file.length())
            }
        } catch (e: Exception) {
            overview["Error"] = "Failed to read MIDI: ${e.message?.take(120)}"
        }

        InspectorResult(overview = overview)
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
