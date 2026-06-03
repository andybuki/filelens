package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.DiffKind
import com.filelens.commander.domain.model.TextDiff
import com.filelens.commander.domain.model.TextDiffLine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextDiffUseCase @Inject constructor() {

    fun diff(left: String, right: String, maxLines: Int = MAX_DIFF_LINES): TextDiff {
        val leftLines = left.split('\n')
        val rightLines = right.split('\n')
        val truncated = leftLines.size > maxLines || rightLines.size > maxLines
        val l = if (truncated) leftLines.take(maxLines) else leftLines
        val r = if (truncated) rightLines.take(maxLines) else rightLines

        // Myers' diff is overkill for the common compare-two-config-files case.
        // Use Hunt–McIlroy LCS — O(n*m) memory and time, but n,m are capped
        // by maxLines so it stays bounded.
        val lcs = buildLcsTable(l, r)
        val out = mutableListOf<TextDiffLine>()
        var added = 0
        var removed = 0
        var unchanged = 0

        var i = l.size
        var j = r.size
        while (i > 0 && j > 0) {
            when {
                l[i - 1] == r[j - 1] -> {
                    out += TextDiffLine(i, j, l[i - 1], DiffKind.UNCHANGED)
                    unchanged++
                    i--; j--
                }
                lcs[i - 1][j] >= lcs[i][j - 1] -> {
                    out += TextDiffLine(i, null, l[i - 1], DiffKind.REMOVED)
                    removed++
                    i--
                }
                else -> {
                    out += TextDiffLine(null, j, r[j - 1], DiffKind.ADDED)
                    added++
                    j--
                }
            }
        }
        while (i > 0) {
            out += TextDiffLine(i, null, l[i - 1], DiffKind.REMOVED)
            removed++
            i--
        }
        while (j > 0) {
            out += TextDiffLine(null, j, r[j - 1], DiffKind.ADDED)
            added++
            j--
        }

        return TextDiff(
            lines = out.asReversed(),
            addedCount = added,
            removedCount = removed,
            unchangedCount = unchanged,
            truncated = truncated
        )
    }

    private fun buildLcsTable(a: List<String>, b: List<String>): Array<IntArray> {
        val m = a.size
        val n = b.size
        val table = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                table[i][j] = if (a[i - 1] == b[j - 1]) {
                    table[i - 1][j - 1] + 1
                } else {
                    maxOf(table[i - 1][j], table[i][j - 1])
                }
            }
        }
        return table
    }

    private companion object {
        const val MAX_DIFF_LINES = 5_000
    }
}
