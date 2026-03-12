package com.example.locationwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class LocationWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            LocationUpdateService.start(context)
        }
        WidgetUpdater.updateAllWidgets(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.example.locationwidget.ACTION_REFRESH"

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val city = prefs.getString(KEY_CITY, "Brak danych") ?: "Brak danych"
            val postal = prefs.getString(KEY_POSTAL, "—") ?: "—"
            val lastUpdate = prefs.getString(KEY_LAST_UPDATE, "Jeszcze nie odświeżono") ?: "Jeszcze nie odświeżono"
            val status = prefs.getString(KEY_STATUS, "Dotknij, aby odświeżyć") ?: "Dotknij, aby odświeżyć"

            val views = RemoteViews(context.packageName, R.layout.widget_location)
            views.setTextViewText(R.id.widgetCity, city)
            views.setTextViewText(R.id.widgetPostal, postal)
            views.setTextViewText(R.id.widgetUpdatedAt, "Aktualizacja: $lastUpdate")
            views.setTextViewText(R.id.widgetStatus, status)

            val refreshIntent = Intent(context, LocationWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widgetRoot, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun updateAll(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, LocationWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(componentName)
            widgetIds.forEach { updateWidget(context, appWidgetManager, it) }
        }
    }
}
