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
 * Inspects ISO 9660 / DMG / raw IMG disk images. We don't try to enumerate the
 * full directory tree — that requires sector-level parsing — but we surface the
 * volume identifier and basic geometry so the file isn't just opaque binary.
 */
@Singleton
class DiskImageInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.ARCHIVE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val ext = file.extension.lowercase()
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()

        overview["Format"] = when (ext) {
            "iso" -> "ISO 9660 disk image"
            "dmg" -> "Apple Disk Image (DMG)"
            "img" -> "Raw disk image (IMG)"
            "vhd" -> "Virtual Hard Disk (VHD)"
            "vmdk" -> "VMware Virtual Disk (VMDK)"
            "qcow2" -> "QEMU QCOW2 image"
            else -> "Disk image"
        }
        overview["Size"] = formatSize(file.length())

        try {
            when (ext) {
                "iso", "img" -> readIso9660(file, overview, metadata)
                "dmg" -> readDmgTrailer(file, overview, metadata)
                "vhd" -> readVhdFooter(file, overview, metadata)
                "qcow2" -> readQcow2(file, overview, metadata)
            }
        } catch (e: Exception) {
            overview["Note"] = "Header probe failed: ${e.message?.take(120)}"
        }

        InspectorResult(overview = overview, metadata = metadata)
    }

    // ISO 9660 primary volume descriptor lives at sector 16, offset 32768 + 1 (type = 1).
    private fun readIso9660(file: File, overview: MutableMap<String, String>, metadata: MutableMap<String, String>) {
        FileInputStream(file).use { stream ->
            if (stream.skip(32768L) != 32768L) return
            val buf = ByteArray(2048)
            if (stream.read(buf) < 2048) return
            if (buf[0].toInt() != 1) return
            val cd001 = String(buf, 1, 5, Charsets.US_ASCII)
            if (cd001 != "CD001") return

            overview["Volume kind"] = "ISO 9660 Primary"
            extractAscii(buf, 8, 32)?.let { metadata["System identifier"] = it }
            extractAscii(buf, 40, 32)?.let { overview["Volume label"] = it }
            extractAscii(buf, 318, 128)?.let { metadata["Publisher"] = it }
            extractAscii(buf, 446, 128)?.let { metadata["Application"] = it }
            extractAscii(buf, 574, 37)?.let { metadata["Copyright file"] = it }
        }
    }

    // DMG: a 512-byte koly trailer at the end of the file with "koly" magic.
    private fun readDmgTrailer(file: File, overview: MutableMap<String, String>, metadata: MutableMap<String, String>) {
        if (file.length() < 512) return
        FileInputStream(file).use { stream ->
            if (stream.skip(file.length() - 512) != file.length() - 512) return
            val trailer = ByteArray(512)
            if (stream.read(trailer) < 512) return
            val magic = String(trailer, 0, 4, Charsets.US_ASCII)
            if (magic != "koly") return
            overview["Trailer"] = "Apple UDIF (koly)"
            val version = readUInt32(trailer, 4).toInt()
            metadata["UDIF version"] = version.toString()
            val sectorCount = readUInt64(trailer, 200)
            if (sectorCount > 0) {
                overview["Logical sectors"] = "%,d".format(sectorCount)
                overview["Logical size"] = formatSize(sectorCount * 512)
            }
        }
    }

    private fun readVhdFooter(file: File, overview: MutableMap<String, String>, metadata: MutableMap<String, String>) {
        if (file.length() < 512) return
        FileInputStream(file).use { stream ->
            if (stream.skip(file.length() - 512) != file.length() - 512) return
            val footer = ByteArray(512)
            if (stream.read(footer) < 512) return
            val cookie = String(footer, 0, 8, Charsets.US_ASCII)
            if (cookie != "conectix") return
            overview["Footer"] = "conectix (VHD)"
            val features = readUInt32(footer, 8).toInt()
            metadata["Feature flags"] = "0x%08X".format(features)
            val diskTypeCode = readUInt32(footer, 60).toInt()
            overview["Disk type"] = when (diskTypeCode) {
                2 -> "Fixed"
                3 -> "Dynamic"
                4 -> "Differencing"
                else -> "Type $diskTypeCode"
            }
            val originalSize = readUInt64(footer, 40)
            if (originalSize > 0) overview["Disk size"] = formatSize(originalSize)
        }
    }

    private fun readQcow2(file: File, overview: MutableMap<String, String>, metadata: MutableMap<String, String>) {
        FileInputStream(file).use { stream ->
            val head = ByteArray(32)
            if (stream.read(head) < 32) return
            val magic = String(head, 0, 4, Charsets.US_ASCII)
            if (magic.toByteArray()[0] != 0x51.toByte()) return
            val version = readUInt32(head, 4).toInt()
            overview["QCOW2 version"] = version.toString()
            val virtualSize = readUInt64(head, 24)
            if (virtualSize > 0) overview["Virtual size"] = formatSize(virtualSize)
        }
    }

    private fun extractAscii(buf: ByteArray, offset: Int, length: Int): String? {
        if (offset + length > buf.size) return null
        return String(buf, offset, length, Charsets.US_ASCII).trim()
            .takeIf { it.isNotEmpty() && it.any { c -> c.isLetterOrDigit() } }
    }

    private fun readUInt32(buf: ByteArray, offset: Int): Long =
        (((buf[offset].toInt() and 0xFF).toLong() shl 24) or
            ((buf[offset + 1].toInt() and 0xFF).toLong() shl 16) or
            ((buf[offset + 2].toInt() and 0xFF).toLong() shl 8) or
            (buf[offset + 3].toInt() and 0xFF).toLong()) and 0xFFFFFFFFL

    private fun readUInt64(buf: ByteArray, offset: Int): Long {
        var v = 0L
        for (i in 0 until 8) {
            v = (v shl 8) or (buf[offset + i].toLong() and 0xFF)
        }
        return v
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }
}
