package com.filelens.commander.inspector

import com.filelens.commander.detector.MagicBytesDetector
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RiskScannerTest {

    @get:Rule val tmp = TemporaryFolder()

    private val scanner = RiskScanner(MagicBytesDetector())

    @Test fun nonExistentFileReturnsNoRisks() = runBlocking {
        val ghost = File(tmp.root, "does-not-exist.zip")
        assertTrue(scanner.scanFile(ghost).isEmpty())
    }

    @Test fun zipWithExecutableEntryReportsRisk() = runBlocking {
        val zip = tmp.newFile("payload.zip")
        writeZip(zip) { zos ->
            zos.putNextEntry(ZipEntry("readme.txt"))
            zos.write("hello".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("setup.exe"))
            zos.write(ByteArray(64))
            zos.closeEntry()
        }
        val risks = scanner.scanFile(zip).map { it.id }
        assertTrue("expected zip-executables in $risks", risks.contains("zip-executables"))
    }

    @Test fun zipBombDetected() = runBlocking {
        val zip = tmp.newFile("bomb.zip")
        writeZip(zip) { zos ->
            zos.putNextEntry(ZipEntry("zeros.bin"))
            // 8 MB of zeros deflates to ~8 KB → ratio ~1000×, well past the
            // 100× threshold, and compressed size stays above the 1 KB gate.
            val chunk = ByteArray(64 * 1024)
            repeat(128) { zos.write(chunk) }
            zos.closeEntry()
        }
        val risks = scanner.scanFile(zip).map { it.id }
        assertTrue("expected zip-bomb in $risks", risks.contains("zip-bomb"))
    }

    @Test fun extensionMismatchOnRenamedZip() = runBlocking {
        val disguised = tmp.newFile("notes.txt")
        writeZip(disguised) { zos ->
            zos.putNextEntry(ZipEntry("inner.txt"))
            zos.write("hi".toByteArray())
            zos.closeEntry()
        }
        val risks = scanner.scanFile(disguised).map { it.id }
        assertTrue("expected extension-mismatch in $risks", risks.contains("extension-mismatch"))
    }

    @Test fun textFileWithApiKeyReportsSecretsRisk() = runBlocking {
        val txt = tmp.newFile("config.txt")
        txt.writeText("api_key=abcdefghij1234567890ABCDEF\nother=value\n")
        val risks = scanner.scanFile(txt).map { it.id }
        assertTrue("expected secrets-found in $risks", risks.contains("secrets-found"))
    }

    @Test fun plainTextWithoutSecretsReportsNothing() = runBlocking {
        val txt = tmp.newFile("notes.txt")
        txt.writeText("just a plain note\nno credentials here\n")
        assertTrue(scanner.scanFile(txt).isEmpty())
    }

    @Test fun xlsxWithVbaMacroReportsOfficeMacros() = runBlocking {
        val xlsx = tmp.newFile("book.xlsx")
        writeZip(xlsx) { zos ->
            zos.putNextEntry(ZipEntry("xl/vbaProject.bin"))
            zos.write(ByteArray(32))
            zos.closeEntry()
        }
        val risks = scanner.scanFile(xlsx).map { it.id }
        assertTrue("expected office-macros in $risks", risks.contains("office-macros"))
    }

    @Test fun docxWithTrackedChangesReported() = runBlocking {
        val docx = tmp.newFile("draft.docx")
        writeZip(docx) { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write("""<?xml version="1.0"?><doc><w:ins author="alice">added</w:ins></doc>""".toByteArray())
            zos.closeEntry()
        }
        val risks = scanner.scanFile(docx).map { it.id }
        assertTrue("expected office-tracked-changes in $risks", risks.contains("office-tracked-changes"))
    }

    @Test fun docxWithCommentsReported() = runBlocking {
        val docx = tmp.newFile("draft.docx")
        writeZip(docx) { zos ->
            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write("""<?xml version="1.0"?><doc/>""".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("word/comments.xml"))
            zos.write("""<?xml version="1.0"?><comments/>""".toByteArray())
            zos.closeEntry()
        }
        val risks = scanner.scanFile(docx).map { it.id }
        assertTrue("expected office-comments in $risks", risks.contains("office-comments"))
    }

    @Test fun scanFolderAggregatesRisksPerFile() = runBlocking {
        val dir = tmp.newFolder("scanme")

        val riskyZip = File(dir, "payload.zip")
        writeZip(riskyZip) { zos ->
            zos.putNextEntry(ZipEntry("setup.exe"))
            zos.write(ByteArray(16))
            zos.closeEntry()
        }

        val cleanTxt = File(dir, "boring.txt")
        cleanTxt.writeText("just text\n")

        val results = scanner.scanFolder(dir)
        assertNotNull(results[riskyZip.absolutePath])
        assertTrue(results[riskyZip.absolutePath]!!.any { it.id == "zip-executables" })
        // Files with no risks must be omitted entirely.
        assertFalse(results.containsKey(cleanTxt.absolutePath))
    }

    @Test fun scanFolderOnRegularFileReturnsEmpty() = runBlocking {
        val notADir = tmp.newFile("solo.txt").apply { writeText("x") }
        assertEquals(emptyMap<String, Any>(), scanner.scanFolder(notADir))
    }

    private fun writeZip(target: File, block: (ZipOutputStream) -> Unit) {
        ZipOutputStream(FileOutputStream(target)).use(block)
    }
}
