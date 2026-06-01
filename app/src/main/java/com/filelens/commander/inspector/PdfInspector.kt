package com.filelens.commander.inspector

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.RiskSeverity
import com.filelens.commander.domain.model.StructureNode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfInspector @Inject constructor(
    @ApplicationContext private val context: Context
) : FileInspector {

    override val supportedCategories = setOf(FileCategory.PDF)

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = linkedMapOf<String, String>()
        val metadata = linkedMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()
        var preview: PreviewContent? = null
        var structure: StructureNode? = null

        val rawHeader = readHeaderInfo(file)
        rawHeader.version?.let { overview["PDF version"] = it }
        overview["File size"] = formatBytes(file.length())

        if (rawHeader.encrypted) {
            risks.add(RiskItem(
                severity = RiskSeverity.WARNING,
                title = "Encrypted PDF",
                description = "This PDF is password-protected. Content cannot be inspected without the password."
            ))
            overview["Encrypted"] = "Yes"
        }

        if (rawHeader.containsJavaScript) {
            risks.add(RiskItem(
                severity = RiskSeverity.DANGER,
                title = "Embedded JavaScript",
                description = "This PDF contains JavaScript actions. JavaScript in PDFs has been used for exploits and tracking."
            ))
        }

        if (rawHeader.containsLaunchAction) {
            risks.add(RiskItem(
                severity = RiskSeverity.DANGER,
                title = "Launch actions",
                description = "This PDF can launch external programs or open files when viewed."
            ))
        }

        if (rawHeader.containsEmbeddedFiles) {
            risks.add(RiskItem(
                severity = RiskSeverity.WARNING,
                title = "Embedded files",
                description = "This PDF contains embedded files/attachments."
            ))
        }

        if (rawHeader.containsForm) {
            metadata["Has form fields"] = "Yes"
        }

        rawHeader.author?.let { metadata["Author"] = it }
        rawHeader.title?.let { metadata["Title"] = it }
        rawHeader.subject?.let { metadata["Subject"] = it }
        rawHeader.creator?.let { metadata["Creator"] = it }
        rawHeader.producer?.let { metadata["Producer"] = it }
        rawHeader.creationDate?.let { metadata["Created"] = it }
        rawHeader.modDate?.let { metadata["Modified"] = it }

        if ((metadata["Author"] ?: "").isNotEmpty()) {
            risks.add(RiskItem(
                severity = RiskSeverity.INFO,
                title = "Contains author metadata",
                description = "Document reveals author: ${metadata["Author"]}"
            ))
        }

        if (!rawHeader.encrypted) {
            try {
                val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                PdfRenderer(pfd).use { renderer ->
                    overview["Page count"] = renderer.pageCount.toString()
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            overview["First page size"] = "${page.width} × ${page.height}"
                            try {
                                val maxWidth = 1024
                                val scale = if (page.width > maxWidth) {
                                    maxWidth.toFloat() / page.width.toFloat()
                                } else 1f
                                val targetWidth = (page.width * scale).toInt().coerceAtLeast(1)
                                val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                                val bitmap = Bitmap.createBitmap(
                                    targetWidth,
                                    targetHeight,
                                    Bitmap.Config.ARGB_8888
                                )
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(
                                    bitmap,
                                    null,
                                    null,
                                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                                )
                                val thumbFile = File(
                                    context.cacheDir,
                                    "pdf_thumb_${file.name}.png"
                                )
                                FileOutputStream(thumbFile).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                bitmap.recycle()
                                preview = PreviewContent.ImageContent(
                                    path = thumbFile.absolutePath,
                                    width = targetWidth,
                                    height = targetHeight
                                )
                            } catch (e: Exception) {
                                overview["Thumbnail error"] = e.message ?: "Could not render page"
                            }
                        }
                    }
                    val pages = (0 until minOf(renderer.pageCount, 200)).map { i ->
                        StructureNode.Leaf(name = "Page ${i + 1}")
                    }
                    structure = StructureNode.Branch(
                        name = "Pages",
                        children = pages,
                        details = "${renderer.pageCount} pages"
                    )
                }
            } catch (e: Exception) {
                overview["Render error"] = e.message ?: "Could not open PDF"
            }
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            structure = structure,
            previewContent = preview
        )
    }

    private data class HeaderInfo(
        val version: String? = null,
        val encrypted: Boolean = false,
        val containsJavaScript: Boolean = false,
        val containsLaunchAction: Boolean = false,
        val containsEmbeddedFiles: Boolean = false,
        val containsForm: Boolean = false,
        val author: String? = null,
        val title: String? = null,
        val subject: String? = null,
        val creator: String? = null,
        val producer: String? = null,
        val creationDate: String? = null,
        val modDate: String? = null
    )

    private fun readHeaderInfo(file: File): HeaderInfo {
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val headerBytes = ByteArray(8)
                raf.readFully(headerBytes)
                val header = String(headerBytes, Charsets.US_ASCII)
                val version = if (header.startsWith("%PDF-")) header.substring(5).trim() else null

                val sampleSize = minOf(file.length(), 2L * 1024 * 1024).toInt()
                raf.seek(0)
                val sample = ByteArray(sampleSize)
                raf.readFully(sample)
                val text = String(sample, Charsets.ISO_8859_1)

                HeaderInfo(
                    version = version,
                    encrypted = text.contains("/Encrypt"),
                    containsJavaScript = text.contains("/JavaScript") || text.contains("/JS"),
                    containsLaunchAction = text.contains("/Launch"),
                    containsEmbeddedFiles = text.contains("/EmbeddedFile") || text.contains("/Filespec"),
                    containsForm = text.contains("/AcroForm"),
                    author = extractInfoField(text, "Author"),
                    title = extractInfoField(text, "Title"),
                    subject = extractInfoField(text, "Subject"),
                    creator = extractInfoField(text, "Creator"),
                    producer = extractInfoField(text, "Producer"),
                    creationDate = extractInfoField(text, "CreationDate"),
                    modDate = extractInfoField(text, "ModDate")
                )
            }
        } catch (_: Exception) {
            HeaderInfo()
        }
    }

    private fun extractInfoField(text: String, key: String): String? {
        val pattern = Regex("/$key\\s*\\(([^)]{1,200})\\)")
        val match = pattern.find(text) ?: return null
        val raw = match.groupValues[1]
        return raw.trim().ifEmpty { null }
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
