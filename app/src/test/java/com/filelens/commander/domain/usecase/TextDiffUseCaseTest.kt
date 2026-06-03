package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.DiffKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextDiffUseCaseTest {

    private val useCase = TextDiffUseCase()

    @Test fun identicalInputsHaveOnlyUnchangedLines() {
        val text = "alpha\nbeta\ngamma"
        val diff = useCase.diff(text, text)
        assertEquals(0, diff.addedCount)
        assertEquals(0, diff.removedCount)
        assertEquals(3, diff.unchangedCount)
        assertTrue(diff.lines.all { it.kind == DiffKind.UNCHANGED })
    }

    @Test fun pureInsertionAtEnd() {
        val left = "alpha\nbeta"
        val right = "alpha\nbeta\ngamma"
        val diff = useCase.diff(left, right)
        assertEquals(1, diff.addedCount)
        assertEquals(0, diff.removedCount)
        assertEquals(2, diff.unchangedCount)
        val added = diff.lines.last()
        assertEquals(DiffKind.ADDED, added.kind)
        assertEquals("gamma", added.content)
        assertEquals(3, added.rightLineNumber)
    }

    @Test fun pureDeletionAtStart() {
        val left = "alpha\nbeta"
        val right = "beta"
        val diff = useCase.diff(left, right)
        assertEquals(0, diff.addedCount)
        assertEquals(1, diff.removedCount)
        val removed = diff.lines.first()
        assertEquals(DiffKind.REMOVED, removed.kind)
        assertEquals("alpha", removed.content)
    }

    @Test fun replacementLooksLikeRemoveThenAdd() {
        val diff = useCase.diff("a\nb\nc", "a\nX\nc")
        assertEquals(1, diff.addedCount)
        assertEquals(1, diff.removedCount)
        assertEquals(2, diff.unchangedCount)
    }

    @Test fun truncationKicksInWhenInputTooLong() {
        val left = (1..10).joinToString("\n") { "line$it" }
        val right = (1..10).joinToString("\n") { "line$it" }
        val diff = useCase.diff(left, right, maxLines = 5)
        assertTrue("truncated flag should be set", diff.truncated)
        assertEquals(5, diff.unchangedCount)
    }

    @Test fun emptyInputsProduceEmptyDiff() {
        val diff = useCase.diff("", "")
        // Single empty line each — counts as one unchanged line.
        assertEquals(1, diff.unchangedCount)
        assertFalse(diff.truncated)
    }
}
