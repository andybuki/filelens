package com.filelens.commander.inspector

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.IMAGE)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val width = options.outWidth
        val height = options.outHeight

        if (width > 0 && height > 0) {
            overview["Resolution"] = "${width}×${height}"
            overview["Megapixels"] = "%.1f MP".format(width.toLong() * height / 1_000_000.0)
        }
        options.outMimeType?.let { overview["Format"] = it }

        try {
            val exif = ExifInterface(file.absolutePath)

            exif.getAttribute(ExifInterface.TAG_MAKE)?.let { metadata["Camera make"] = it }
            exif.getAttribute(ExifInterface.TAG_MODEL)?.let { metadata["Camera model"] = it }
            exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { metadata["Date taken"] = it }
            exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { metadata["Date modified"] = it }
            exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)?.let { metadata["EXIF width"] = it }
            exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)?.let { metadata["EXIF height"] = it }
            exif.getAttribute(ExifInterface.TAG_ORIENTATION)?.let {
                metadata["Orientation"] = describeOrientation(it.toIntOrNull() ?: 0)
            }
            exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { metadata["Exposure"] = "${it}s" }
            exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { metadata["Aperture"] = "f/$it" }
            @Suppress("DEPRECATION")
            exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)?.let { metadata["ISO"] = it }
            exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
                val parts = it.split("/")
                if (parts.size == 2) {
                    val mm = parts[0].toDoubleOrNull()?.div(parts[1].toDoubleOrNull() ?: 1.0)
                    if (mm != null) metadata["Focal length"] = "%.1f mm".format(mm)
                } else {
                    metadata["Focal length"] = it
                }
            }
            exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let {
                metadata["White balance"] = if (it == "0") "Auto" else "Manual"
            }
            exif.getAttribute(ExifInterface.TAG_FLASH)?.let { metadata["Flash"] = describeFlash(it.toIntOrNull() ?: 0) }
            exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let { metadata["Software"] = it }
            exif.getAttribute(ExifInterface.TAG_ARTIST)?.let { metadata["Artist"] = it }
            exif.getAttribute(ExifInterface.TAG_COPYRIGHT)?.let { metadata["Copyright"] = it }

            val latLong = exif.latLong
            if (latLong != null) {
                metadata["GPS latitude"] = "%.6f".format(latLong[0])
                metadata["GPS longitude"] = "%.6f".format(latLong[1])
                exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.let { metadata["GPS altitude"] = it }
                exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)?.let { metadata["GPS date"] = it }

                risks.add(
                    RiskItem(
                        severity = RiskSeverity.WARNING,
                        title = "Contains GPS location",
                        description = "This image contains GPS coordinates (%.4f, %.4f). Consider removing location metadata before sharing.".format(latLong[0], latLong[1])
                    )
                )
            }

            exif.getAttribute(ExifInterface.TAG_ARTIST)?.let {
                risks.add(
                    RiskItem(
                        severity = RiskSeverity.INFO,
                        title = "Contains artist info",
                        description = "Artist: $it"
                    )
                )
            }
        } catch (_: Exception) {
            metadata["EXIF"] = "Could not read EXIF data"
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            previewContent = PreviewContent.ImageContent(
                path = file.absolutePath,
                width = if (width > 0) width else null,
                height = if (height > 0) height else null
            )
        )
    }

    private fun describeOrientation(orientation: Int): String = when (orientation) {
        ExifInterface.ORIENTATION_NORMAL -> "Normal"
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flipped horizontal"
        ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped vertical"
        ExifInterface.ORIENTATION_TRANSPOSE -> "Transposed"
        ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90°"
        ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse"
        ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270°"
        else -> "Unknown ($orientation)"
    }

    private fun describeFlash(flash: Int): String = when {
        flash and 0x01 == 0 -> "No flash"
        flash and 0x06 == 0x06 -> "Flash fired, return detected"
        flash and 0x01 == 1 -> "Flash fired"
        else -> "Unknown ($flash)"
    }
}
