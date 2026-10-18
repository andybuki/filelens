package com.filelens.commander.data

import org.junit.Assert.assertEquals
import org.junit.Test

class IncomingFilesTest {

    @Test
    fun `keeps ordinary names`() {
        assertEquals("IMG_0001.jpg", IncomingFiles.safeName("IMG_0001.jpg"))
    }

    @Test
    fun `strips directory traversal`() {
        assertEquals("passwd", IncomingFiles.safeName("../../etc/passwd"))
        assertEquals("evil.txt", IncomingFiles.safeName("..\\..\\evil.txt"))
    }

    @Test
    fun `never produces a hidden or empty name`() {
        assertEquals("shared_file", IncomingFiles.safeName(null))
        assertEquals("shared_file", IncomingFiles.safeName(".."))
        assertEquals("env", IncomingFiles.safeName(".env"))
    }

    @Test
    fun `replaces reserved characters`() {
        assertEquals("a_b_c.pdf", IncomingFiles.safeName("a:b*c.pdf"))
    }
}
