package com.filelens.commander.domain.model

enum class DiffKind { ADDED, REMOVED, CHANGED, UNCHANGED }

data class FileChecksum(
    val md5: String?,
    val sha256: String?
)

data class FileCompareSummary(
    val leftPath: String,
    val rightPath: String,
    val leftName: String,
    val rightName: String,
    val leftSize: Long,
    val rightSize: Long,
    val leftModified: Long,
    val rightModified: Long,
    val leftChecksum: FileChecksum,
    val rightChecksum: FileChecksum,
    val identical: Boolean
)

data class MetadataDiffEntry(
    val key: String,
    val leftValue: String?,
    val rightValue: String?,
    val kind: DiffKind
)

data class TextDiffLine(
    val leftLineNumber: Int?,
    val rightLineNumber: Int?,
    val content: String,
    val kind: DiffKind
)

data class TextDiff(
    val lines: List<TextDiffLine>,
    val addedCount: Int,
    val removedCount: Int,
    val unchangedCount: Int,
    val truncated: Boolean = false
)

data class StructuralDiffEntry(
    val path: String,
    val leftValue: String?,
    val rightValue: String?,
    val kind: DiffKind
)

data class StructuralDiff(
    val format: String,
    val entries: List<StructuralDiffEntry>,
    val unchangedCount: Int = 0
)

sealed class FileCompareContent {
    data object None : FileCompareContent()
    data class Text(val diff: TextDiff) : FileCompareContent()
    data class Structural(val diff: StructuralDiff) : FileCompareContent()
    data class Binary(val message: String) : FileCompareContent()
}

data class FileCompareResult(
    val summary: FileCompareSummary,
    val metadata: List<MetadataDiffEntry>,
    val content: FileCompareContent
)

enum class FolderEntryDiffKind { LEFT_ONLY, RIGHT_ONLY, BOTH_SAME, BOTH_CHANGED }

data class FolderEntryDiff(
    val relativePath: String,
    val isDirectory: Boolean,
    val kind: FolderEntryDiffKind,
    val leftSize: Long?,
    val rightSize: Long?,
    val leftModified: Long?,
    val rightModified: Long?
)

data class RenamedPair(
    val leftRelativePath: String,
    val rightRelativePath: String,
    val sharedChecksum: String,
    val size: Long
)

data class FolderCompareResult(
    val leftPath: String,
    val rightPath: String,
    val entries: List<FolderEntryDiff>,
    val renamedCandidates: List<RenamedPair> = emptyList()
)
