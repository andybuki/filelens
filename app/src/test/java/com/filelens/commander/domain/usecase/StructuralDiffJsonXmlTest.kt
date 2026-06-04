package com.filelens.commander.domain.usecase

import com.filelens.commander.domain.model.DiffKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The JSON/XML branches of StructuralDiff use Android's org.json and
 * XmlPullParser, so they need Robolectric to run on the JVM. The lightweight
 * keyValue branch is covered in StructuralDiffUseCaseTest.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StructuralDiffJsonXmlTest {

    private val useCase = StructuralDiffUseCase()

    @Test fun jsonValueChangeShowsAsChangedEntry() {
        val left = """{"a": 1, "b": "x"}"""
        val right = """{"a": 1, "b": "y"}"""
        val diff = useCase.diffJson(left, right)
        assertNotNull(diff)
        val changed = diff!!.entries.single { it.path == "b" }
        assertEquals(DiffKind.CHANGED, changed.kind)
        assertEquals("x", changed.leftValue)
        assertEquals("y", changed.rightValue)
        assertTrue(diff.entries.none { it.path == "a" })
    }

    @Test fun jsonNestedObjectsFlattenedWithDottedPaths() {
        val left = """{"user": {"name": "Ada"}}"""
        val right = """{"user": {"name": "Grace"}}"""
        val diff = useCase.diffJson(left, right)!!
        val entry = diff.entries.single()
        assertEquals("user.name", entry.path)
    }

    @Test fun jsonArraysIndexedByPosition() {
        val left = """{"xs": [1, 2, 3]}"""
        val right = """{"xs": [1, 9, 3]}"""
        val diff = useCase.diffJson(left, right)!!
        val entry = diff.entries.single()
        assertEquals("xs[1]", entry.path)
    }

    @Test fun jsonInvalidInputReturnsNullSoCallerCanFallBack() {
        val diff = useCase.diffJson("not json", "still not json")
        assertEquals(null, diff)
    }

    @Test fun xmlAttributeChangeDetected() {
        val left = "<root><item id=\"1\">v1</item></root>"
        val right = "<root><item id=\"2\">v1</item></root>"
        val diff = useCase.diffXml(left, right)!!
        val attr = diff.entries.single { it.path.endsWith("@id") }
        assertEquals(DiffKind.CHANGED, attr.kind)
    }

    @Test fun xmlElementTextChange() {
        val left = "<doc><title>Old</title></doc>"
        val right = "<doc><title>New</title></doc>"
        val diff = useCase.diffXml(left, right)!!
        val entry = diff.entries.single { it.path == "doc/title" }
        assertEquals(DiffKind.CHANGED, entry.kind)
        assertEquals("Old", entry.leftValue)
        assertEquals("New", entry.rightValue)
    }
}
