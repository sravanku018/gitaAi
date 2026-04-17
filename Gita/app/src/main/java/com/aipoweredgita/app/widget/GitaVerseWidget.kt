package com.aipoweredgita.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aipoweredgita.app.MainActivity
import com.aipoweredgita.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Widget that displays a random Bhagavad Gita verse each day
 */
class GitaVerseWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Update each widget instance
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Called when the first widget is created
        super.onEnabled(context)
    }

    override fun onDisabled(context: Context) {
        // Called when the last widget is removed
        super.onDisabled(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_WIDGET_REFRESH) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, GitaVerseWidget::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_WIDGET_REFRESH = "com.aipoweredgita.app.WIDGET_REFRESH"

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_verse_layout)

            // Get verse for today


            // Set the chapter and verse number
            // Load verse text - try cache first, then internet if needed
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                    
                    var verseData = database.cachedVerseDao().getRandomVerse()
                    
                    // Auto-reset if exhausted
                    if (verseData == null && database.cachedVerseDao().getCachedCount() > 0) {
                         database.randomVerseHistoryDao().clearHistory()
                         verseData = database.cachedVerseDao().getRandomVerse()
                    }

                    // Record history if found
                    if (verseData != null) {
                        try {
                            database.randomVerseHistoryDao().insertShownVerse(
                                com.aipoweredgita.app.database.RandomVerseHistory(
                                    chapterNo = verseData.chapterNo,
                                    verseNo = verseData.verseNo
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    var verseText: String? = null
                    val displayChapter = verseData?.chapterNo ?: 2
                    val displayVerse = verseData?.verseNo ?: 47

                    if (verseData != null) {
                         // Use Telugu verse text - replace \n with spaces and limit to 100 chars for widget
                        val cleanedText = verseData.verse.replace("\\n", " ").replace("\n", " ")
                        verseText = if (cleanedText.length > 100) {
                            cleanedText.take(97) + "..."
                        } else {
                            cleanedText
                        }
                    }

                    withContext(Dispatchers.Main) {
                         // Update chapter/verse title
                        views.setTextViewText(
                            R.id.widget_chapter_verse,
                            "$displayChapter:$displayVerse"
                        )

                        if (verseText != null) {
                            views.setTextViewText(R.id.widget_verse_text, verseText)
                        } else {
                            views.setTextViewText(
                                R.id.widget_verse_text,
                                "Download verses in app to enable widget"
                            )
                        }

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        views.setTextViewText(
                            R.id.widget_verse_text,
                            "Error loading verse. Please open the app."
                        )
                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }

            // Set up click to open app
            val appIntent = Intent(context, MainActivity::class.java)
            val appPendingIntent = PendingIntent.getActivity(
                context,
                0,
                appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, appPendingIntent)

            // Set up refresh click (content description handled in layout by text label)
            val refreshIntent = Intent(context, GitaVerseWidget::class.java).apply {
                action = ACTION_WIDGET_REFRESH
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh, refreshPendingIntent)

            // Update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Returns a chapter and verse number based on the current day
         * This ensures the same verse is shown all day, but changes daily
         */

    }
}
