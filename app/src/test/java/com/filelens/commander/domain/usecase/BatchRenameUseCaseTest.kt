package com.filelens.commander.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BatchRenameUseCaseTest {

    private val useCase = BatchRenameUseCase()

    @Test fun sequenceTokenWithPadding() {
        val plan = useCase.plan(
            sourcePaths = listOf("/a/foo.txt", "/a/bar.txt", "/a/baz.txt"),
            rule = BatchRenameUseCase.Rule(pattern = "img_{n:03}{ext}")
        )
        assertEquals("img_001.txt", plan.items[0].newName)
        assertEquals("img_002.txt", plan.items[1].newName)
        assertEquals("img_003.txt", plan.items[2].newName)
        assertTrue(plan.collisions.isEmpty())
    }

    @Test fun namePlaceholderPreservesOriginalStem() {
        val plan = useCase.plan(
            sourcePaths = listOf("/a/hello.kt"),
            rule = BatchRenameUseCase.Rule(pattern = "prefix-{name}{ext}")
        )
        assertEquals("prefix-hello.kt", plan.items[0].newName)
    }

    @Test fun literalFindReplaceAppliesBeforeNameSubstitution() {
        val plan = useCase.plan(
            sourcePaths = listOf("/a/Hello World.txt", "/a/Hi World.txt"),
            rule = BatchRenameUseCase.Rule(pattern = "{name}{ext}", find = "World", replace = "Universe")
        )
        assertEquals("Hello Universe.txt", plan.items[0].newName)
        assertEquals("Hi Universe.txt", plan.items[1].newName)
    }

    @Test fun regexFindReplaceWorks() {
        val plan = useCase.plan(
            sourcePaths = listOf("/a/IMG_1234.jpg"),
            rule = BatchRenameUseCase.Rule(
                pattern = "{name}{ext}",
                find = "IMG_(\\d+)",
                replace = "photo_$1",
                useRegex = true
            )
        )
        assertEquals("photo_1234.jpg", plan.items[0].newName)
    }

    @Test fun collisionsAreDetectedWhenTwoTargetsMatch() {
        val plan = useCase.plan(
            sourcePaths = listOf("/dir/a.txt", "/dir/b.txt"),
            rule = BatchRenameUseCase.Rule(pattern = "same.txt")
        )
        assertEquals(1, plan.collisions.size)
        assertEquals("/dir/same.txt", plan.collisions.first())
    }

    @Test fun emptyPatternMarksItemsInvalid() {
        val plan = useCase.plan(
            sourcePaths = listOf("/a/foo.txt"),
            rule = BatchRenameUseCase.Rule(pattern = "")
        )
        assertFalse(plan.items.first().isValid)
        assertEquals("Empty name", plan.items.first().reason)
    }

    @Test fun slashInPatternIsRejected() {
        val plan = useCase.plan(
            sourcePaths = listOf("/a/foo.txt"),
            rule = BatchRenameUseCase.Rule(pattern = "sub/foo.txt")
        )
        assertFalse(plan.items.first().isValid)
    }
}
