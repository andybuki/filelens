package com.filelens.commander.viewmodel

import com.filelens.commander.domain.usecase.BatchRenameUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class BatchRenameViewModelTest {

    @get:Rule val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var vm: BatchRenameViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = BatchRenameViewModel(BatchRenameUseCase())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun startWithEmptyListLeavesStateUntouched() {
        val before = vm.state.value
        vm.start(emptyList())
        assertEquals(before, vm.state.value)
    }

    @Test fun startBuildsInitialPlanFromDefaultRule() {
        vm.start(listOf("/a/foo.txt", "/a/bar.txt"))
        val state = vm.state.value
        assertEquals(2, state.sourcePaths.size)
        assertTrue(state.plan.items.all { it.isValid })
        assertEquals("foo.txt", state.plan.items[0].newName)
        assertEquals("bar.txt", state.plan.items[1].newName)
    }

    @Test fun updateRuleReplanWithoutLosingSourcePaths() {
        vm.start(listOf("/a/foo.txt", "/a/bar.txt"))
        vm.updateRule(BatchRenameUseCase.Rule(pattern = "img_{n:02}{ext}"))
        val items = vm.state.value.plan.items
        assertEquals("img_01.txt", items[0].newName)
        assertEquals("img_02.txt", items[1].newName)
    }

    @Test fun commitRenamesRealFilesAndMarksDone() = runTest(dispatcher) {
        // SharedFlow emit suspends without subscribers; drain in the background.
        // Pinning runTest to the unconfined dispatcher makes backgroundScope.launch
        // start eagerly, so the collector is subscribed before commit() emits.
        backgroundScope.launch { vm.events.collect {} }

        val foo = tmp.newFile("foo.txt").apply { writeText("hello") }
        val bar = tmp.newFile("bar.txt").apply { writeText("hi") }
        vm.start(listOf(foo.absolutePath, bar.absolutePath))
        vm.updateRule(BatchRenameUseCase.Rule(pattern = "renamed_{n}{ext}"))

        var doneCalled = false
        vm.commit { doneCalled = true }
        val state = vm.state.first { it.done }

        assertTrue(doneCalled)
        assertFalse(state.applying)
        assertTrue(File(tmp.root, "renamed_1.txt").exists())
        assertTrue(File(tmp.root, "renamed_2.txt").exists())
        assertFalse(foo.exists())
    }

    @Test fun commitEmitsSummaryEvent() = runTest(dispatcher) {
        val foo = tmp.newFile("a.txt").apply { writeText("x") }
        vm.start(listOf(foo.absolutePath))
        vm.updateRule(BatchRenameUseCase.Rule(pattern = "z{ext}"))

        val received = CompletableDeferred<String>()
        backgroundScope.launch {
            vm.events.collect { if (!received.isCompleted) received.complete(it) }
        }
        vm.commit { }
        assertTrue(received.await().startsWith("Renamed"))
    }

    @Test fun resetClearsState() {
        vm.start(listOf("/a/foo.txt"))
        assertEquals(1, vm.state.value.sourcePaths.size)
        vm.reset()
        assertEquals(BatchRenameState(), vm.state.value)
    }
}
