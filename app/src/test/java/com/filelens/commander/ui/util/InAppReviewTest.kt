package com.filelens.commander.ui.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InAppReviewTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000L * day

    @Test
    fun `asks once enough value and time have passed`() {
        assertTrue(InAppReview.shouldAsk(points = 6, firstSeen = now - 3 * day, lastAsked = 0, now = now))
    }

    @Test
    fun `waits for enough points`() {
        assertFalse(InAppReview.shouldAsk(points = 5, firstSeen = now - 30 * day, lastAsked = 0, now = now))
    }

    @Test
    fun `never asks on the first days of use`() {
        assertFalse(InAppReview.shouldAsk(points = 50, firstSeen = now - day, lastAsked = 0, now = now))
    }

    @Test
    fun `respects the re-ask interval`() {
        assertFalse(InAppReview.shouldAsk(points = 6, firstSeen = now - 365 * day, lastAsked = now - 30 * day, now = now))
        assertTrue(InAppReview.shouldAsk(points = 6, firstSeen = now - 365 * day, lastAsked = now - 121 * day, now = now))
    }
}
