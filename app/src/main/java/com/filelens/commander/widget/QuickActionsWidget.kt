package com.filelens.commander.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.filelens.commander.MainActivity
import app.com.filelens.R

/** One-row home-screen widget with shortcuts into FileLens. */
class QuickActionsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.widget_quick_actions).apply {
            setOnClickPendingIntent(R.id.widget_brand, launch(context, null, 0))
            setOnClickPendingIntent(R.id.widget_search, launch(context, ACTION_SEARCH, 1))
            setOnClickPendingIntent(R.id.widget_downloads, launch(context, ACTION_DOWNLOADS, 2))
            setOnClickPendingIntent(R.id.widget_images, launch(context, ACTION_IMAGES, 3))
            setOnClickPendingIntent(R.id.widget_files, launch(context, ACTION_FILES, 4))
        }
        manager.updateAppWidget(ids, views)
    }

    private fun launch(context: Context, action: String?, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        if (action != null) intent.putExtra(EXTRA_ACTION, action)
        return PendingIntent.getActivity(
            context, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        const val EXTRA_ACTION = "com.filelens.widget.ACTION"
        const val ACTION_SEARCH = "search"
        const val ACTION_DOWNLOADS = "downloads"
        const val ACTION_IMAGES = "images"
        const val ACTION_FILES = "files"
    }
}
