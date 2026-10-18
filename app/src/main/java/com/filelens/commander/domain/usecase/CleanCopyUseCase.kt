package com.filelens.commander.domain.usecase

import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CleanCopyUseCase @Inject constructor() {

    suspend fun createCleanedCopy(sourcePath: String, targetDir: String): CleanResult = withContext(Dispatchers.IO) {
        try {
            val source = File(sourcePath)
            val ext = source.extension.lowercase()
            val cleanName = "${source.nameWithoutExtension}_clean.${source.extension}"
            val target = File(targetDir, cleanName)

            source.copyTo(target, overwrite = true)

            val strippedFields = mutableListOf<String>()

            when (ext) {
                "jpg", "jpeg", "png", "webp", "tiff", "tif" -> {
                    stripExif(target, strippedFields)
                }
                "docx", "xlsx", "pptx" -> {
                    stripOoxmlMetadata(target, strippedFields)
                }
            }

            CleanResult.Success(target.absolutePath, strippedFields)
        } catch (e: Exception) {
            CleanResult.Error(e.message ?: "Failed to create cleaned copy")
        }
    }

    /**
     * Removes only location data in place, leaving camera, date and
     * orientation intact. Returns true if any location data was found.
     */
    fun stripLocation(file: File): Boolean {
        val exif = ExifInterface(file.absolutePath)
        var found = false
        for (tag in GPS_TAGS) {
            if (exif.getAttribute(tag) != null) {
                exif.setAttribute(tag, null)
                found = true
            }
        }
        // XMP can carry its own copy of the coordinates.
        val xmp = exif.getAttribute(ExifInterface.TAG_XMP)
        if (xmp != null && xmp.contains("GPS", ignoreCase = true)) {
            exif.setAttribute(ExifInterface.TAG_XMP, null)
            found = true
        }
        if (found) exif.saveAttributes()
        return found
    }

    private fun stripExif(file: File, stripped: MutableList<String>) {
        try {
            val exif = ExifInterface(file.absolutePath)
            val gpsFields = GPS_TAGS
            val personalFields = listOf(
                ExifInterface.TAG_ARTIST, ExifInterface.TAG_COPYRIGHT,
                ExifInterface.TAG_SOFTWARE, ExifInterface.TAG_CAMERA_OWNER_NAME,
                ExifInterface.TAG_BODY_SERIAL_NUMBER, ExifInterface.TAG_LENS_SERIAL_NUMBER
            )

            for (tag in gpsFields) {
                if (exif.getAttribute(tag) != null) {
                    exif.setAttribute(tag, null)
                    stripped.add("GPS data")
                }
            }
            for (tag in personalFields) {
                if (exif.getAttribute(tag) != null) {
                    exif.setAttribute(tag, null)
                    stripped.add(tag)
                }
            }

            exif.saveAttributes()
            val distinct = stripped.distinct()
            stripped.clear()
            stripped.addAll(distinct)
        } catch (_: Exception) {}
    }

    private fun stripOoxmlMetadata(file: File, stripped: MutableList<String>) {
        try {
            val tempFile = File(file.parent, ".tmp_${file.name}")
            val zipIn = java.util.zip.ZipFile(file)
            val zipOut = java.util.zip.ZipOutputStream(tempFile.outputStream())

            for (entry in zipIn.entries()) {
                if (entry.name == "docProps/core.xml" || entry.name == "docProps/app.xml") {
                    stripped.add("Office metadata (${entry.name})")
                    val newEntry = java.util.zip.ZipEntry(entry.name)
                    zipOut.putNextEntry(newEntry)
                    val emptyXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"/>"""
                    zipOut.write(emptyXml.toByteArray())
                    zipOut.closeEntry()
                    continue
                }
                val newEntry = java.util.zip.ZipEntry(entry.name)
                zipOut.putNextEntry(newEntry)
                zipIn.getInputStream(entry).use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }

            zipOut.close()
            zipIn.close()
            tempFile.renameTo(file)
        } catch (_: Exception) {}
    }

    companion object {
        /** Formats whose EXIF block ExifInterface can rewrite in place. */
        val EXIF_WRITABLE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")

        val GPS_TAGS = listOf(
            ExifInterface.TAG_GPS_VERSION_ID,
            ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE, ExifInterface.TAG_GPS_LONGITUDE_REF,
            ExifInterface.TAG_GPS_ALTITUDE, ExifInterface.TAG_GPS_ALTITUDE_REF,
            ExifInterface.TAG_GPS_TIMESTAMP, ExifInterface.TAG_GPS_DATESTAMP,
            ExifInterface.TAG_GPS_SATELLITES, ExifInterface.TAG_GPS_STATUS,
            ExifInterface.TAG_GPS_MEASURE_MODE, ExifInterface.TAG_GPS_DOP,
            ExifInterface.TAG_GPS_SPEED, ExifInterface.TAG_GPS_SPEED_REF,
            ExifInterface.TAG_GPS_TRACK, ExifInterface.TAG_GPS_TRACK_REF,
            ExifInterface.TAG_GPS_IMG_DIRECTION, ExifInterface.TAG_GPS_IMG_DIRECTION_REF,
            ExifInterface.TAG_GPS_MAP_DATUM,
            ExifInterface.TAG_GPS_DEST_LATITUDE, ExifInterface.TAG_GPS_DEST_LATITUDE_REF,
            ExifInterface.TAG_GPS_DEST_LONGITUDE, ExifInterface.TAG_GPS_DEST_LONGITUDE_REF,
            ExifInterface.TAG_GPS_DEST_BEARING, ExifInterface.TAG_GPS_DEST_BEARING_REF,
            ExifInterface.TAG_GPS_DEST_DISTANCE, ExifInterface.TAG_GPS_DEST_DISTANCE_REF,
            ExifInterface.TAG_GPS_PROCESSING_METHOD, ExifInterface.TAG_GPS_AREA_INFORMATION,
            ExifInterface.TAG_GPS_DIFFERENTIAL, ExifInterface.TAG_GPS_H_POSITIONING_ERROR
        )
    }

    sealed class CleanResult {
        data class Success(val cleanPath: String, val strippedFields: List<String>) : CleanResult()
        data class Error(val message: String) : CleanResult()
    }
}
