package com.filelens.commander.viewmodel

import com.filelens.commander.domain.usecase.FolderAnalysisUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class FolderDashboardViewModelTest {

    @get:Rule val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var vm: FolderDashboardViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = FolderDashboardViewModel(FolderAnalysisUseCase())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun analyzePopulatesSummaryAndStopsLoading() = runTest(dispatcher) {
        val dir = tmp.newFolder("scanme")
        File(dir, "a.txt").writeText("alpha")
        File(dir, "b.txt").writeText("beta beta")

        vm.analyze(dir.absolutePath)
        val state = vm.state.first { it.summary != null }

        assertTrue(state.active)
        assertFalse(state.isAnalyzing)
        assertNotNull(state.summary)
        assertEquals(2, state.summary!!.fileCount)
    }

    @Test fun analyzeOnNonExistentFolderProducesEmptySummary() = runTest(dispatcher) {
        vm.analyze("/no/such/place/that/exists")
        val state = vm.state.first { it.summary != null }
        assertEquals(0, state.summary!!.fileCount)
        assertEquals(0L, state.summary!!.totalSize)
    }

    @Test fun dismissResetsState() = runTest(dispatcher) {
        val dir = tmp.newFolder("scanme")
        vm.analyze(dir.absolutePath)
        vm.state.first { it.summary != null }
        vm.dismiss()
        assertEquals(FolderDashboardState(), vm.state.value)
    }
}
