package com.filelens.commander.domain.model

data class InspectorResult(
    val overview: Map<String, String> = emptyMap(),
    val metadata: Map<String, String> = emptyMap(),
    val risks: List<RiskItem> = emptyList(),
    val structure: StructureNode? = null,
    val previewContent: PreviewContent? = null,
    val trustScore: TrustScore? = null,
    val provenance: Provenance? = null,
    val secrets: List<SecretMatch> = emptyList()
)

data class SecretMatch(
    val kind: String,
    val redactedSample: String,
    val location: String? = null,
    val lineNumber: Int? = null
)

data class Provenance(
    val source: ProvenanceSource,
    val displayName: String,
    val description: String,
    val confidence: ProvenanceConfidence
)

enum class ProvenanceSource {
    DOWNLOADS,
    CAMERA,
    SCREENSHOTS,
    WHATSAPP,
    TELEGRAM,
    SIGNAL,
    BROWSER,
    BLUETOOTH,
    EMAIL,
    CLOUD,
    APP_PRIVATE,
    SYSTEM,
    SDCARD,
    OTHER
}

enum class ProvenanceConfidence { HIGH, MEDIUM, LOW }

data class TrustScore(
    val level: TrustLevel,
    val numericScore: Int,
    val topReasons: List<String>
)

enum class TrustLevel(val displayName: String) {
    TRUSTED("Trusted"),
    LOW_RISK("Low risk"),
    MEDIUM_RISK("Medium risk"),
    HIGH_RISK("High risk")
}

data class RiskItem(
    val severity: RiskSeverity,
    val title: String,
    val description: String,
    val id: String = "",
    val category: RiskCategory = RiskCategory.OTHER,
    val whyItMatters: String? = null,
    val recommendedAction: String? = null,
    val evidence: Map<String, String> = emptyMap(),
    val detector: String? = null,
    val learnMoreKey: String? = null
)

enum class RiskSeverity {
    INFO,
    WARNING,
    DANGER
}

enum class RiskCategory(val displayName: String) {
    PRIVACY("Privacy"),
    SECURITY("Security"),
    ACTIVE_CONTENT("Active content"),
    INTEGRITY("Integrity"),
    METADATA("Metadata"),
    ENCRYPTION("Encryption"),
    OTHER("Other")
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
