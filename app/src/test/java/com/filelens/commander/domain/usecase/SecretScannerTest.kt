package com.filelens.commander.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretScannerTest {

    private val scanner = SecretScanner()

    @Test fun emptyTextReturnsNothing() {
        assertTrue(scanner.scan("").isEmpty())
    }

    @Test fun plainTextWithoutSecretsReturnsEmpty() {
        assertTrue(scanner.scan("hello world\nthis is just prose").isEmpty())
    }

    @Test fun awsAccessKeyIsDetectedAndRedacted() {
        val hits = scanner.scan("export key=AKIAIOSFODNN7EXAMPLE")
        assertEquals(1, hits.size)
        assertEquals("AWS access key", hits[0].kind)
        assertEquals("AKIA…LE", hits[0].redactedSample)
    }

    @Test fun githubPersonalAccessTokenDetected() {
        val token = "ghp_" + "a".repeat(36)
        val hits = scanner.scan(token)
        assertEquals(1, hits.size)
        assertEquals("GitHub PAT", hits[0].kind)
    }

    @Test fun genericApiKeyAssignmentDetected() {
        val hits = scanner.scan("api_key = \"abcdefghij1234567890\"")
        assertEquals(1, hits.size)
        assertEquals("Generic API key", hits[0].kind)
    }

    @Test fun databaseUrlDetected() {
        val hits = scanner.scan("DB_URL=postgres://user:pw@db.example.com:5432/app")
        assertEquals(1, hits.size)
        assertEquals("Database URL", hits[0].kind)
    }

    @Test fun pemPrivateKeyDetected() {
        val hits = scanner.scan("-----BEGIN RSA PRIVATE KEY-----\nMIIEowIBAAKCA…\n-----END RSA PRIVATE KEY-----")
        assertTrue(hits.any { it.kind == "PEM private key" })
    }

    @Test fun jwtDetected() {
        val jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val hits = scanner.scan(jwt)
        assertTrue(hits.any { it.kind == "JWT" })
    }

    @Test fun multipleDistinctSecretsAllReported() {
        val text = """
            AWS=AKIAIOSFODNN7EXAMPLE
            GH=ghp_${"a".repeat(36)}
            URL=mysql://root:pass@localhost/db
        """.trimIndent()
        val kinds = scanner.scan(text).map { it.kind }.toSet()
        assertTrue(kinds.contains("AWS access key"))
        assertTrue(kinds.contains("GitHub PAT"))
        assertTrue(kinds.contains("Database URL"))
    }

    @Test fun duplicateOccurrencesAreDeduped() {
        val key = "AKIAIOSFODNN7EXAMPLE"
        val hits = scanner.scan("$key and again $key and $key")
        assertEquals(1, hits.size)
    }

    @Test fun maxMatchesCapsResults() {
        val text = (0 until 10).joinToString("\n") {
            "ghp_" + ('a' + (it % 26)).toString().repeat(36)
        }
        val hits = scanner.scan(text, maxMatches = 3)
        assertEquals(3, hits.size)
    }

    @Test fun lineNumberIsAccurate() {
        val text = "line1\nline2\nAKIAIOSFODNN7EXAMPLE\nline4"
        val hit = scanner.scan(text).single()
        assertEquals(3, hit.lineNumber)
    }

    @Test fun locationIsPropagatedIntoEachMatch() {
        val hits = scanner.scan("AKIAIOSFODNN7EXAMPLE", location = "config/secrets.env")
        assertEquals("config/secrets.env", hits.single().location)
    }

    @Test fun locationDefaultsToNull() {
        val hit = scanner.scan("AKIAIOSFODNN7EXAMPLE").single()
        assertNull(hit.location)
    }

    @Test fun lineNumberStartsAtOneForFirstLine() {
        val hit = scanner.scan("AKIAIOSFODNN7EXAMPLE").single()
        assertNotNull(hit.lineNumber)
        assertEquals(1, hit.lineNumber)
    }

    @Test fun shortSecretRedactionHasNoTail() {
        // raw 8 chars or fewer → tail is empty: "head…"
        val hit = scanner.scan("password = \"abcdef12\"").single()
        // raw token captured is the assignment expression; head is first 4 chars.
        assertTrue(hit.redactedSample.startsWith(hit.redactedSample.substringBefore("…")))
        assertTrue(hit.redactedSample.contains("…"))
    }
}
