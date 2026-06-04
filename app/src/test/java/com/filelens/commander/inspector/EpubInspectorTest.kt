package com.filelens.commander.inspector

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EpubInspectorTest {

    @Rule @JvmField val tmp = TemporaryFolder()

    private val inspector = EpubInspector()

    @Test fun parsesDublinCoreMetadataFromOpf() = runBlocking {
        val epub = tmp.newFile("book.epub")
        writeEpub(epub) { zip ->
            zip.putEntry(
                "META-INF/container.xml",
                """<?xml version="1.0"?>
                |<container xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
                |  <rootfiles><rootfile full-path="OEBPS/content.opf" /></rootfiles>
                |</container>
                """.trimMargin()
            )
            zip.putEntry(
                "OEBPS/content.opf",
                """<?xml version="1.0"?>
                |<package xmlns="http://www.idpf.org/2007/opf">
                |  <metadata xmlns:dc="http://purl.org/dc/elements/1.1/">
                |    <dc:title>The Test Book</dc:title>
                |    <dc:creator>Ada Lovelace</dc:creator>
                |    <dc:language>en</dc:language>
                |    <dc:publisher>FileLens Press</dc:publisher>
                |    <dc:identifier>urn:uuid:0001</dc:identifier>
                |  </metadata>
                |  <manifest>
                |    <item id="c1" href="chap1.xhtml" media-type="application/xhtml+xml" />
                |    <item id="c2" href="chap2.xhtml" media-type="application/xhtml+xml" />
                |  </manifest>
                |  <spine>
                |    <itemref idref="c1" />
                |    <itemref idref="c2" />
                |  </spine>
                |</package>
                """.trimMargin()
            )
            zip.putEntry("OEBPS/chap1.xhtml", "<html><body>chapter 1</body></html>")
            zip.putEntry("OEBPS/chap2.xhtml", "<html><body>chapter 2</body></html>")
        }

        val result = inspector.inspect(epub)

        assertEquals("EPUB", result.overview["Format"])
        assertEquals("The Test Book", result.overview["Title"])
        assertEquals("Ada Lovelace", result.overview["Author"])
        assertEquals("2", result.overview["Chapters"])
        assertEquals("FileLens Press", result.metadata["Publisher"])
        assertEquals("en", result.metadata["Language"])
        assertEquals("urn:uuid:0001", result.metadata["Identifier"])
    }

    @Test fun missingContainerStillReturnsResultWithNote() = runBlocking {
        val epub = tmp.newFile("broken.epub")
        writeEpub(epub) { zip ->
            zip.putEntry("notes.txt", "no container here")
        }
        val result = inspector.inspect(epub)
        assertEquals("EPUB", result.overview["Format"])
        assertNotNull(result.overview["Note"])
        assertTrue(result.overview["Entries"]!!.toInt() >= 1)
    }

    private fun writeEpub(file: File, block: (ZipOutputStream) -> Unit) {
        file.outputStream().use { fos ->
            ZipOutputStream(fos).use(block)
        }
    }

    private fun ZipOutputStream.putEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }
}
