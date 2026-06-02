package com.filelens.commander.inspector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaInspector @Inject constructor(
    @ApplicationContext private val context: Context
) : FileInspector {

    override val supportedCategories = setOf(FileCategory.AUDIO, FileCategory.VIDEO)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()
        var preview: PreviewContent? = null

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

                // Grab a representative frame near the start (~1s, falls back to 0).
                val frameUs = if ((durationMs ?: 0L) > 1500L) 1_000_000L else 0L
                val frame = runCatching {
                    retriever.getFrameAtTime(frameUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                }.getOrNull()
                preview = frame?.let { saveThumbnail(it, "video_thumb_${file.nameHash()}.png") }
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

            // Embedded album art for audio (and for video files that ship one).
            if (preview == null) {
                val artBytes = runCatching { retriever.embeddedPicture }.getOrNull()
                if (artBytes != null && artBytes.isNotEmpty()) {
                    val bitmap = runCatching {
                        BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                    }.getOrNull()
                    if (bitmap != null) {
                        preview = saveThumbnail(bitmap, "audio_art_${file.nameHash()}.png")
                    }
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
            risks = risks,
            previewContent = preview
        )
    }

    private fun saveThumbnail(source: Bitmap, fileName: String): PreviewContent.ImageContent? {
        val (target, scaled) = scaleToMaxEdge(source, MAX_EDGE_PX)
        return runCatching {
            val out = File(context.cacheDir, fileName)
            FileOutputStream(out).use { stream ->
                target.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            if (scaled) target.recycle()
            PreviewContent.ImageContent(
                path = out.absolutePath,
                width = target.width,
                height = target.height
            )
        }.getOrNull()
    }

    private fun scaleToMaxEdge(source: Bitmap, maxEdge: Int): Pair<Bitmap, Boolean> {
        val w = source.width
        val h = source.height
        val maxSide = maxOf(w, h)
        if (maxSide <= maxEdge) return source to false
        val ratio = maxEdge.toFloat() / maxSide
        val tw = (w * ratio).toInt().coerceAtLeast(1)
        val th = (h * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(source, tw, th, true)
        return scaled to true
    }

    private fun File.nameHash(): String {
        // Stable, short identifier so the cache file name stays predictable
        // across runs without leaking the full original path.
        return absolutePath.hashCode().toUInt().toString(16)
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

    private companion object {
        const val MAX_EDGE_PX = 1024
    }
}
