package com.filelens.commander.domain.usecase

import com.filelens.commander.detector.MagicBytesDetector
import com.filelens.commander.domain.model.DiffKind
import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.FileChecksum
import com.filelens.commander.domain.model.FileCompareContent
import com.filelens.commander.domain.model.FileCompareResult
import com.filelens.commander.domain.model.FileCompareSummary
import com.filelens.commander.domain.model.MetadataDiffEntry
import com.filelens.commander.inspector.InspectorDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompareFilesUseCase @Inject constructor(
    private val checksumUseCase: ChecksumUseCase,
    private val textDiffUseCase: TextDiffUseCase,
    private val structuralDiffUseCase: StructuralDiffUseCase,
    private val inspectorDispatcher: InspectorDispatcher,
    private val magicBytesDetector: MagicBytesDetector
) {

    suspend fun compare(leftPath: String, rightPath: String): FileCompareResult? = coroutineScope {
        val left = File(leftPath)
        val right = File(rightPath)
        if (!left.isFile || !right.isFile) return@coroutineScope null

        val leftChecksumDeferred = async { checksumUseCase.compute(left) }
        val rightChecksumDeferred = async { checksumUseCase.compute(right) }
        val leftMetaDeferred = async { extractMetadata(left) }
        val rightMetaDeferred = async { extractMetadata(right) }
        val contentDeferred = async { compareContent(left, right) }

        val leftChecksum = leftChecksumDeferred.await()
        val rightChecksum = rightChecksumDeferred.await()
        val leftMeta = leftMetaDeferred.await()
        val rightMeta = rightMetaDeferred.await()
        val content = contentDeferred.await()

        val summary = FileCompareSummary(
            leftPath = leftPath,
            rightPath = rightPath,
            leftName = left.name,
            rightName = right.name,
            leftSize = left.length(),
            rightSize = right.length(),
            leftModified = left.lastModified(),
            rightModified = right.lastModified(),
            leftChecksum = leftChecksum,
            rightChecksum = rightChecksum,
            identical = leftChecksum.sha256 != null &&
                leftChecksum.sha256 == rightChecksum.sha256
        )

        FileCompareResult(
            summary = summary,
            metadata = diffMetadata(leftMeta, rightMeta),
            content = content
        )
    }

    private suspend fun extractMetadata(file: File): Map<String, String> = withContext(Dispatchers.IO) {
        val detection = magicBytesDetector.detect(file)
        val category = if (detection.confidence > 0.5f) detection.category else classifyByExtensionFallback(file)
        val extension = file.extension
        val result = inspectorDispatcher.inspect(file, category, extension)
        // Both overview and metadata blocks contribute. Overview entries get a
        // visible prefix so identical keys don't collide.
        buildMap<String, String> {
            for ((k, v) in result.overview) put("Overview · $k", v)
            for ((k, v) in result.metadata) put(k, v)
        }
    }

    private fun classifyByExtensionFallback(file: File): FileCategory =
        com.filelens.commander.domain.model.classifyByExtension(file.extension)

    private fun diffMetadata(left: Map<String, String>, right: Map<String, String>): List<MetadataDiffEntry> {
        val keys = (left.keys + right.keys).toSortedSet()
        val entries = mutableListOf<MetadataDiffEntry>()
        for (key in keys) {
            val l = left[key]
            val r = right[key]
            val kind = when {
                key !in left -> DiffKind.ADDED
                key !in right -> DiffKind.REMOVED
                l != r -> DiffKind.CHANGED
                else -> DiffKind.UNCHANGED
            }
            entries += MetadataDiffEntry(key = key, leftValue = l, rightValue = r, kind = kind)
        }
        return entries
    }

    private suspend fun compareContent(left: File, right: File): FileCompareContent =
        withContext(Dispatchers.IO) {
            val leftExt = left.extension.lowercase()
            val rightExt = right.extension.lowercase()
            val sharedExt = leftExt.takeIf { it == rightExt }

            // Files larger than the cap go straight to a binary-only message —
            // text diff and JSON/XML parsing both load full contents into memory.
            if (left.length() > MAX_TEXT_BYTES || right.length() > MAX_TEXT_BYTES) {
                return@withContext FileCompareContent.Binary(
                    "Files exceed ${MAX_TEXT_BYTES / 1024} KB content-diff limit"
                )
            }

            val leftText = readTextSafe(left)
            val rightText = readTextSafe(right) ?: return@withContext FileCompareContent.Binary(
                "Cannot read content of ${right.name}"
            )
            if (leftText == null) {
                return@withContext FileCompareContent.Binary("Cannot read content of ${left.name}")
            }

            // Skip binary-looking content even if extensions are text-ish.
            if (looksBinary(leftText) || looksBinary(rightText)) {
                return@withContext FileCompareContent.Binary("Binary content — comparing by checksum only")
            }

            when (sharedExt) {
                "json" -> structuralDiffUseCase.diffJson(leftText, rightText)?.let {
                    return@withContext FileCompareContent.Structural(it)
                }
                "xml", "xsl", "xslt", "xsd", "svg", "xhtml", "plist" ->
                    structuralDiffUseCase.diffXml(leftText, rightText)?.let {
                        return@withContext FileCompareContent.Structural(it)
                    }
            }

            FileCompareContent.Text(textDiffUseCase.diff(leftText, rightText))
        }

    private fun readTextSafe(file: File): String? = try {
        if (file.length() == 0L) "" else file.readText(Charsets.UTF_8)
    } catch (_: Exception) {
        null
    }

    private fun looksBinary(text: String): Boolean {
        if (text.isEmpty()) return false
        val sampleSize = minOf(text.length, 4096)
        var control = 0
        for (i in 0 until sampleSize) {
            val c = text[i].code
            if (c == 0) return true
            // Count non-printable control chars (excluding tab/newline/cr).
            if (c < 32 && c != 9 && c != 10 && c != 13) control++
        }
        return control > sampleSize / 32
    }

    private companion object {
        const val MAX_TEXT_BYTES = 5L * 1024 * 1024
    }
}
