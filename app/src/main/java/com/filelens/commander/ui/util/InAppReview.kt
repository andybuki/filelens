package com.filelens.commander.ui.util

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Asks for a Play rating once the user has clearly got value from the app.
 * Points come from finished inspections (1) and Safe Shares (3); the prompt is
 * only attempted while the user is looking at an inspection result, never
 * while a share sheet covers the app. Play applies its own quota on top.
 */
object InAppReview {

    private const val PREFS = "in_app_review"
    private const val KEY_POINTS = "points"
    private const val KEY_FIRST_SEEN = "first_seen"
    private const val KEY_LAST_ASKED = "last_asked"

    const val POINTS_INSPECTION = 1
    const val POINTS_SAFE_SHARE = 3
    const val POINTS_NEEDED = 6
    const val MIN_AGE_MS = 2L * 24 * 60 * 60 * 1000
    const val REASK_AFTER_MS = 120L * 24 * 60 * 60 * 1000

    fun recordSafeShare(context: Context) {
        addPoints(context, POINTS_SAFE_SHARE)
    }

    fun onInspectionCompleted(activity: Activity) {
        val prefs = addPoints(activity, POINTS_INSPECTION)
        val now = System.currentTimeMillis()
        if (!shouldAsk(
                points = prefs.getInt(KEY_POINTS, 0),
                firstSeen = prefs.getLong(KEY_FIRST_SEEN, now),
                lastAsked = prefs.getLong(KEY_LAST_ASKED, 0L),
                now = now
            )
        ) return
        prefs.edit {
            putLong(KEY_LAST_ASKED, now)
            putInt(KEY_POINTS, 0)
        }
        val manager = ReviewManagerFactory.create(activity)
        manager.requestReviewFlow().addOnCompleteListener { request ->
            if (request.isSuccessful && !activity.isFinishing) {
                manager.launchReviewFlow(activity, request.result)
            }
        }
    }

    fun shouldAsk(points: Int, firstSeen: Long, lastAsked: Long, now: Long): Boolean =
        points >= POINTS_NEEDED &&
            now - firstSeen >= MIN_AGE_MS &&
            (lastAsked == 0L || now - lastAsked >= REASK_AFTER_MS)

    private fun addPoints(context: Context, points: Int) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).also { prefs ->
            prefs.edit {
                if (!prefs.contains(KEY_FIRST_SEEN)) putLong(KEY_FIRST_SEEN, System.currentTimeMillis())
                putInt(KEY_POINTS, prefs.getInt(KEY_POINTS, 0) + points)
            }
        }
}
