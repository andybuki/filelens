package com.filelens.commander.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts a "operation in progress" notification with a progress bar so long
 * copy/move/delete batches give the user something to look at when they
 * background the app.
 *
 * Notifications silently no-op when the runtime POST_NOTIFICATIONS permission
 * is missing (Android 13+); the in-app snackbar still reports completion.
 */
@Singleton
class OperationNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {
    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            val existing = mgr?.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "File operations",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Progress for copy, move, delete, and batch operations"
                    setSound(null, null)
                    enableVibration(false)
                }
                mgr?.createNotificationChannel(channel)
            }
        }
    }

    fun showProgress(id: Int, title: String, progress: Int, total: Int) {
        if (!hasPermission()) return
        val builder = baseBuilder(title)
            .setContentText(if (total > 0) "$progress / $total" else "Working…")
            .setProgress(total.coerceAtLeast(1), progress.coerceAtMost(total), total <= 0)
            .setOngoing(true)
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun showComplete(id: Int, title: String, summary: String) {
        if (!hasPermission()) return
        val builder = baseBuilder(title)
            .setContentText(summary)
            .setProgress(0, 0, false)
            .setOngoing(false)
            .setAutoCancel(true)
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    fun cancel(id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    private fun baseBuilder(title: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setSilent(true)
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val CHANNEL_ID = "filelens_operations"
        // Per-operation IDs so concurrent operations don't overwrite each other.
        const val ID_COPY = 1001
        const val ID_MOVE = 1002
        const val ID_DELETE = 1003
        const val ID_BATCH_RENAME = 1004
        const val ID_FOLDER_SYNC = 1005
    }
}
