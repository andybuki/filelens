package com.filelens.commander.domain.usecase

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ChecksumUseCaseTest {

    @Rule @JvmField val tmp = TemporaryFolder()

    private val useCase = ChecksumUseCase()

    @Test fun knownContentProducesExpectedSha256AndMd5() = runBlocking {
        // Vectors for the empty string and "abc" from NIST and RFC 1321 respectively.
        val empty = tmp.newFile("empty")
        empty.writeBytes(ByteArray(0))
        val emptyHash = useCase.compute(empty)
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            emptyHash.sha256
        )
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", emptyHash.md5)

        val abc = tmp.newFile("abc")
        abc.writeText("abc")
        val abcHash = useCase.compute(abc)
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            abcHash.sha256
        )
        assertEquals("900150983cd24fb0d6963f7d28e17f72", abcHash.md5)
    }

    @Test fun unreadableOrMissingFileReturnsNulls() = runBlocking {
        val missing = tmp.newFile("missing")
        missing.delete()
        val result = useCase.compute(missing)
        assertEquals(null, result.sha256)
        assertEquals(null, result.md5)
    }

    @Test fun quickSha256MatchesFullCompute() = runBlocking {
        val file = tmp.newFile("payload")
        file.writeBytes(ByteArray(2048) { it.toByte() })
        val full = useCase.compute(file).sha256
        val quick = useCase.quickSha256(file)
        assertNotNull(full)
        assertEquals(full, quick)
    }
}
