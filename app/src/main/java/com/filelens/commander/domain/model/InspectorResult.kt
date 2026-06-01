package com.filelens.commander.domain.model

data class InspectorResult(
    val overview: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val risks: List<RiskItem> = emptyList(),
    val structure: StructureNode? = null,
    val previewContent: PreviewContent? = null
)

data class RiskItem(
    val severity: RiskSeverity,
    val title: String,
    val description: String
)

enum class RiskSeverity {
    INFO,
    WARNING,
    DANGER
}

sealed class PreviewContent {
    data class TextContent(
        val content: String,
        val language: String? = null,
        val encoding: String? = null,
        val lineCount: Int = 0,
        val rawContent: String? = null
    ) : PreviewContent()

    data class ImageContent(
        val path: String,
        val width: Int? = null,
        val height: Int? = null
    ) : PreviewContent()

    data class HexContent(
        val bytes: ByteArray,
        val totalSize: Long = 0
    ) : PreviewContent() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is HexContent) return false
            return bytes.contentEquals(other.bytes) && totalSize == other.totalSize
        }
        override fun hashCode(): Int = bytes.contentHashCode() * 31 + totalSize.hashCode()
    }

    data class TreeContent(
        val root: StructureNode
    ) : PreviewContent()
}

sealed class StructureNode {
    abstract val name: String

    data class Branch(
        override val name: String,
        val children: List<StructureNode>,
        val details: String? = null,
        val size: Long? = null
    ) : StructureNode()

    data class Leaf(
        override val name: String,
        val value: String? = null,
        val size: Long? = null
    ) : StructureNode()
}
