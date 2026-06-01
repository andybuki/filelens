package com.filelens.commander.inspector

import android.media.MediaMetadataRetriever
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(FileCategory.AUDIO, FileCategory.VIDEO)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        overview["File size"] = formatBytes(file.length())
        val ext = file.extension.lowercase()
        if (ext.isNotEmpty()) overview["Format"] = ext.uppercase()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)

            val mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            mime?.let { overview["MIME type"] = it }

            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            durationMs?.let { overview["Duration"] = formatDuration(it) }

            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
            bitrate?.let { overview["Bitrate"] = "${it / 1000} kbps" }

            val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes"
            if (hasVideo) {
                val w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                val h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                if (w != null && h != null) overview["Resolution"] = "$w × $h"

                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.let {
                    metadata["Frame count"] = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.let {
                    metadata["Capture framerate"] = it
                }
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.let {
                    metadata["Rotation"] = "$it°"
                }
            }
            if (hasAudio) {
                val channels = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_NUM_TRACKS)
                channels?.let { metadata["Tracks"] = it }
            }

            mapOf(
                "Title" to MediaMetadataRetriever.METADATA_KEY_TITLE,
                "Artist" to MediaMetadataRetriever.METADATA_KEY_ARTIST,
                "Album" to MediaMetadataRetriever.METADATA_KEY_ALBUM,
                "Album artist" to MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST,
                "Composer" to MediaMetadataRetriever.METADATA_KEY_COMPOSER,
                "Genre" to MediaMetadataRetriever.METADATA_KEY_GENRE,
                "Year" to MediaMetadataRetriever.METADATA_KEY_YEAR,
                "Track number" to MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER,
                "Disc number" to MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER,
                "Author" to MediaMetadataRetriever.METADATA_KEY_AUTHOR,
                "Writer" to MediaMetadataRetriever.METADATA_KEY_WRITER,
                "Date" to MediaMetadataRetriever.METADATA_KEY_DATE,
                "Compilation" to MediaMetadataRetriever.METADATA_KEY_COMPILATION,
                "Location" to MediaMetadataRetriever.METADATA_KEY_LOCATION
            ).forEach { (label, key) ->
                retriever.extractMetadata(key)?.takeIf { it.isNotBlank() }?.let {
                    metadata[label] = it
                }
            }

            val location = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION)
            if (!location.isNullOrBlank()) {
                risks.add(RiskItem(
                    severity = RiskSeverity.WARNING,
                    title = "Contains GPS location",
                    description = "Media tagged with location: $location"
                ))
            }

            val author = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            if (!author.isNullOrBlank()) {
                risks.add(RiskItem(
                    severity = RiskSeverity.INFO,
                    title = "Contains author/artist metadata",
                    description = "Reveals: $author"
                ))
            }
        } catch (e: Exception) {
            overview["Read error"] = e.message ?: "Could not read media metadata"
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks
        )
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        if (mb < 1024) return "%.1f MB".format(mb)
        return "%.2f GB".format(mb / 1024.0)
    }
}
