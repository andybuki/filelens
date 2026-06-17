package com.filelens.commander.viewmodel

import com.filelens.commander.domain.usecase.ChecksumUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class HashVerifyViewModelTest {

    @get:Rule val tmp = TemporaryFolder()

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var vm: HashVerifyViewModel

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        vm = HashVerifyViewModel(ChecksumUseCase())
    }

    @After fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test fun loadComputesChecksumAndStopsComputing() = runTest {
        val file = tmp.newFile("payload.bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        vm.load(file.absolutePath, file.name)
        val state = vm.state.first { it.checksum != null }
        assertEquals(false, state.isComputing)
        assertNotNull(state.checksum!!.sha256)
        assertEquals(file.absolutePath, state.path)
        assertEquals(file.name, state.name)
    }

    @Test fun reloadingSamePathIsNoOpOnceChecksumIsKnown() = runTest {
        val file = tmp.newFile("a.bin").apply { writeBytes(byteArrayOf(7)) }
        vm.load(file.absolutePath, file.name)
        val first = vm.state.first { it.checksum != null }.checksum
        // Mutating the file would change the hash if it recomputed — a real no-op
        // preserves the original checksum object.
        file.writeBytes(byteArrayOf(99))
        vm.load(file.absolutePath, file.name)
        assertEquals(first, vm.state.value.checksum)
    }

    @Test fun setExpectedSha256MatchesProducesMatch() = runTest {
        val file = tmp.newFile("hello.txt").apply { writeText("hello") }
        vm.load(file.absolutePath, file.name)
        val sha = vm.state.first { it.checksum != null }.checksum!!.sha256!!
        vm.setExpected(sha.uppercase())
        assertEquals(Match.MATCH, vm.state.value.matches)
    }

    @Test fun setExpectedMd5MismatchProducesMismatch() = runTest {
        val file = tmp.newFile("hello.txt").apply { writeText("hello") }
        vm.load(file.absolutePath, file.name)
        vm.state.first { it.checksum != null }
        vm.setExpected("0".repeat(32))
        assertEquals(Match.MISMATCH, vm.state.value.matches)
    }

    @Test fun setExpectedWithWrongLengthStaysUnknown() = runTest {
        val file = tmp.newFile("hello.txt").apply { writeText("hello") }
        vm.load(file.absolutePath, file.name)
        vm.state.first { it.checksum != null }
        vm.setExpected("abc")
        assertEquals(Match.UNKNOWN, vm.state.value.matches)
    }

    @Test fun setExpectedBeforeChecksumIsKnownStaysUnknown() {
        vm.setExpected("a".repeat(64))
        assertEquals(Match.UNKNOWN, vm.state.value.matches)
        assertNull(vm.state.value.checksum)
    }

    @Test fun resetClearsState() = runTest {
        val file = tmp.newFile("a.bin").apply { writeBytes(byteArrayOf(1)) }
        vm.load(file.absolutePath, file.name)
        vm.state.first { it.checksum != null }
        vm.setExpected("z".repeat(64))
        vm.reset()
        assertEquals(HashVerifyState(), vm.state.value)
    }
}
