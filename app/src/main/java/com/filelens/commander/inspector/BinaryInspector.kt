package com.filelens.commander.inspector

import com.filelens.commander.domain.model.FileCategory
import com.filelens.commander.domain.model.InspectorResult
import com.filelens.commander.domain.model.PreviewContent
import com.filelens.commander.domain.model.RiskItem
import com.filelens.commander.domain.model.StructureNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BinaryInspector @Inject constructor() : FileInspector {

    override val supportedCategories = setOf(
        FileCategory.UNKNOWN,
        FileCategory.EXECUTABLE,
        FileCategory.DATABASE
    )

    override suspend fun inspect(file: File): InspectorResult = withContext(Dispatchers.IO) {
        val overview = mutableMapOf<String, String>()
        val metadata = mutableMapOf<String, String>()
        val risks = mutableListOf<RiskItem>()

        val headerSize = minOf(file.length(), 4096L).toInt()
        val header = file.inputStream().use { stream ->
            val buf = ByteArray(headerSize)
            val read = stream.read(buf)
            if (read < headerSize) buf.copyOf(read) else buf
        }

        val entropy = calculateEntropy(header)
        overview["Header entropy"] = "%.2f / 8.00".format(entropy)

        if (entropy > 7.5) {
            overview["Likely content"] = "Encrypted or compressed"
            risks.add(RiskItem(
                com.filelens.commander.domain.model.RiskSeverity.INFO,
                "High entropy",
                "File appears to be encrypted or compressed (entropy: %.2f). Content cannot be meaningfully previewed.".format(entropy)
            ))
        } else if (entropy < 1.0 && file.length() > 1024) {
            overview["Likely content"] = "Sparse / mostly empty"
        }

        val strings = extractStrings(header, minLength = 6)
        if (strings.isNotEmpty()) {
            overview["Readable strings"] = "${strings.size} found in header"
            val structureChildren = strings.take(50).map { str ->
                StructureNode.Leaf(name = str)
            }
            val structure = StructureNode.Branch(
                name = "Strings (first ${structureChildren.size})",
                children = if (strings.size > 50) {
                    structureChildren + StructureNode.Leaf(name = "... ${strings.size - 50} more")
                } else structureChildren
            )

            return@withContext InspectorResult(
                overview = overview,
                metadata = metadata,
                risks = risks,
                structure = structure,
                previewContent = PreviewContent.HexContent(
                    bytes = header.copyOf(minOf(header.size, 512)),
                    totalSize = file.length()
                )
            )
        }

        InspectorResult(
            overview = overview,
            metadata = metadata,
            risks = risks,
            previewContent = PreviewContent.HexContent(
                bytes = header.copyOf(minOf(header.size, 512)),
                totalSize = file.length()
            )
        )
    }

    private fun calculateEntropy(data: ByteArray): Double {
        if (data.isEmpty()) return 0.0
        val freq = IntArray(256)
        for (b in data) freq[b.toInt() and 0xFF]++
        var entropy = 0.0
        val len = data.size.toDouble()
        for (f in freq) {
            if (f == 0) continue
            val p = f / len
            entropy -= p * (Math.log(p) / Math.log(2.0))
        }
        return entropy
    }

    private fun extractStrings(data: ByteArray, minLength: Int): List<String> {
        val strings = mutableListOf<String>()
        val current = StringBuilder()
        for (b in data) {
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) {
                current.append(c.toChar())
            } else {
                if (current.length >= minLength) {
                    strings.add(current.toString())
                }
                current.clear()
            }
        }
        if (current.length >= minLength) {
            strings.add(current.toString())
        }
        return strings
    }
}
