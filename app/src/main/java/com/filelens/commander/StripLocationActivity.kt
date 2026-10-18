package com.filelens.commander

import android.content.ClipData
import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import app.com.filelens.R
import androidx.activity.ComponentActivity
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.lifecycleScope
import com.filelens.commander.data.IncomingFiles
import com.filelens.commander.domain.usecase.CleanCopyUseCase
import com.filelens.commander.ui.util.InAppReview
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/**
 * "Share without location" share target. Receives photos, removes GPS data
 * from private copies and immediately re-opens the system share sheet with
 * the cleaned copies. Has no UI of its own.
 */
@AndroidEntryPoint
class StripLocationActivity : ComponentActivity() {

    @Inject lateinit var cleanCopy: CleanCopyUseCase

    private data class Cleaned(val file: File, val mime: String, val hadLocation: Boolean)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            finish()
            return
        }
        val uris = IncomingFiles.urisFrom(intent)
        if (uris.isEmpty()) {
            fail()
            return
        }
        lifecycleScope.launch {
            val cleaned = withContext(Dispatchers.IO) { uris.mapNotNull(::clean) }
            // Never pass on a photo we couldn't clean: all or nothing.
            if (cleaned.size != uris.size) {
                fail()
                return@launch
            }
            val message = if (cleaned.any { it.hadLocation }) {
                R.string.strip_location_done
            } else {
                R.string.strip_location_none_found
            }
            Toast.makeText(this@StripLocationActivity, message, Toast.LENGTH_SHORT).show()
            startActivity(chooserFor(cleaned))
            InAppReview.recordSafeShare(this@StripLocationActivity)
            finish()
        }
    }

    private fun clean(uri: Uri): Cleaned? = runCatching {
        val copy = IncomingFiles.copyToCache(this, uri, CACHE_DIR)
        val mime = contentResolver.getType(uri)?.lowercase()
            ?: android.webkit.MimeTypeMap.getSingleton()
                .getMimeTypeFromExtension(copy.extension.lowercase())
            ?: ""
        val ext = mimeToExtension[mime] ?: copy.extension.lowercase()
        when {
            ext in CleanCopyUseCase.EXIF_WRITABLE_EXTENSIONS ->
                Cleaned(copy, mime.ifEmpty { "image/$ext" }, cleanCopy.stripLocation(copy))
            // GIF has no EXIF block, and re-encoding would kill the animation.
            mime == "image/gif" || ext == "gif" -> Cleaned(copy, "image/gif", hadLocation = false)
            else -> {
                // HEIC and friends: ExifInterface can't rewrite them, so
                // re-encode as JPEG, which carries no metadata at all.
                val hadLocation = runCatching { ExifInterface(copy.absolutePath).latLong != null }
                    .getOrDefault(false)
                Cleaned(reencodeAsJpeg(copy), "image/jpeg", hadLocation)
            }
        }
    }.getOrNull()

    private fun reencodeAsJpeg(source: File): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_REENCODE_EDGE) sample *= 2
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: error("Unsupported image")
        val rotation = runCatching { ExifInterface(source.absolutePath).rotationDegrees }.getOrDefault(0)
        val upright = if (rotation == 0) decoded else Bitmap.createBitmap(
            decoded, 0, 0, decoded.width, decoded.height,
            Matrix().apply { postRotate(rotation.toFloat()) }, true
        )
        val target = File(source.parentFile, source.nameWithoutExtension + ".jpg")
        target.outputStream().use { upright.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        if (target != source) source.delete()
        return target
    }

    private fun chooserFor(cleaned: List<Cleaned>): Intent {
        val uris = cleaned.map {
            FileProvider.getUriForFile(this, "$packageName.fileprovider", it.file)
        }
        val send = if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE)
                .putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
        send.type = cleaned.map { it.mime }.distinct().singleOrNull() ?: "image/*"
        send.clipData = ClipData.newRawUri(null, uris[0]).apply {
            uris.drop(1).forEach { addItem(ClipData.Item(it)) }
        }
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { send.putExtra(Intent.EXTRA_TEXT, it) }
        intent.getStringExtra(Intent.EXTRA_SUBJECT)?.let { send.putExtra(Intent.EXTRA_SUBJECT, it) }

        return Intent.createChooser(send, getString(R.string.strip_location_chooser_title)).putExtra(
            Intent.EXTRA_EXCLUDE_COMPONENTS,
            arrayOf(
                ComponentName(this, StripLocationActivity::class.java),
                ComponentName(this, MainActivity::class.java)
            )
        )
    }

    private fun fail() {
        Toast.makeText(this, R.string.strip_location_failed, Toast.LENGTH_LONG).show()
        finish()
    }

    private companion object {
        const val CACHE_DIR = "share_clean"
        const val MAX_REENCODE_EDGE = 4096
        val mimeToExtension = mapOf(
            "image/jpeg" to "jpg",
            "image/jpg" to "jpg",
            "image/png" to "png",
            "image/webp" to "webp"
        )
    }
}
