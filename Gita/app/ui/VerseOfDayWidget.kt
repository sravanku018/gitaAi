package com.aipoweredgita.app.ui

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.aipoweredgita.app.R
import com.aipoweredgita.app.database.GitaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class VerseOfDayWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_verse_of_day)

            try {
                val verse = runBlocking(Dispatchers.IO) {
                    val db = GitaDatabase.getDatabase(context)
                    db.cachedVerseDao().getRandomVerse()
                }

                if (verse != null) {
                    views.setTextViewText(R.id.widget_title, "\uD83D\uDCD6 Verse of the Day")
                    views.setTextViewText(R.id.widget_reference, "Chapter ${verse.chapterNo}, Verse ${verse.verseNo}")
                    views.setTextViewText(R.id.widget_verse, verse.verse.take(150) + if (verse.verse.length > 150) "..." else "")
                    if (verse.translation.isNotBlank()) {
                        views.setTextViewText(R.id.widget_translation, verse.translation.take(120) + if (verse.translation.length > 120) "..." else "")
                    }
                } else {
                    views.setTextViewText(R.id.widget_title, "Bhagavad Gita AI")
                    views.setTextViewText(R.id.widget_verse, "Open the app to load verses")
                }
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_title, "Bhagavad Gita AI")
                views.setTextViewText(R.id.widget_verse, "Tap to open app")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
