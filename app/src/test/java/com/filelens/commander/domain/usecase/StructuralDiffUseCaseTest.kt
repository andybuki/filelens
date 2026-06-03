package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.DiffKind
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM-friendly slice of the structural diff: covers diffKeyValue, which is
 * what the metadata-diff path uses. The diffJson / diffXml helpers depend on
 * org.json and XmlPull, which are part of the Android runtime and need
 * Robolectric to exercise — those live in androidTest if/when we wire it.
 */
class StructuralDiffUseCaseTest {

    private val useCase = StructuralDiffUseCase()

    @Test fun keyValueDiffSurfacesAddRemoveChange() {
        val left = mapOf("a" to "1", "b" to "2", "c" to "3")
        val right = mapOf("a" to "1", "b" to "X", "d" to "4")
        val diff = useCase.diffKeyValue("config", left, right)
        assertEquals("config", diff.format)
        // Unchanged keys should not show up in entries; only the changes do.
        val a = diff.entries.firstOrNull { it.path == "a" }
        assertEquals(null, a)
        val b = diff.entries.single { it.path == "b" }
        val c = diff.entries.single { it.path == "c" }
        val d = diff.entries.single { it.path == "d" }
        assertEquals(DiffKind.CHANGED, b.kind)
        assertEquals(DiffKind.REMOVED, c.kind)
        assertEquals(DiffKind.ADDED, d.kind)
    }

    @Test fun keyValueDiffCountsUnchanged() {
        val left = mapOf("a" to "1", "b" to "2", "c" to "3")
        val right = mapOf("a" to "1", "b" to "2", "c" to "3")
        val diff = useCase.diffKeyValue("config", left, right)
        assertEquals(3, diff.unchangedCount)
        assertEquals(0, diff.entries.size)
    }

    @Test fun keyValueDiffHandlesNullValues() {
        val left = mapOf("token" to null, "expiry" to "2025")
        val right = mapOf("token" to "abc", "expiry" to "2025")
        val diff = useCase.diffKeyValue("env", left, right)
        val token = diff.entries.single()
        assertEquals(DiffKind.CHANGED, token.kind)
        assertEquals(null, token.leftValue)
        assertEquals("abc", token.rightValue)
    }
}
