package com.example.jinotas.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.example.jinotas.R
import com.example.jinotas.widget.WidgetService.Companion.CONTENT

class WidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            title: String? = null,
            textContent: String? = null
        ) {
            val sharedPreferences =
                context.getSharedPreferences("WidgetPrefs", Context.MODE_PRIVATE)
            val widgetTitle = title ?: sharedPreferences.getString(
                "widget_note_${appWidgetId}_title", context.getString(R.string.note_title_widget)
            )
            val widgetContent = textContent ?: sharedPreferences.getString(
                "widget_note_${appWidgetId}_content",
                context.getString(R.string.note_textcontent_widget)
            )

//            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
//                setTextViewText(R.id.widget_title, widgetTitle)
//                setTextViewText(R.id.widget_textcontent, widgetContent)
//
//                val configIntent = Intent(context, ConfigActivity::class.java).apply {
//                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
//                }
//                val configPendingIntent = PendingIntent.getActivity(
//                    context,
//                    appWidgetId,
//                    configIntent,
//                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//                )
//                setOnClickPendingIntent(R.id.widget_layout_view, configPendingIntent)
//            }
//
//            appWidgetManager.updateAppWidget(appWidgetId, views)
            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.widget_layout).apply {
                setTextViewText(R.id.widget_title, widgetTitle)
                val configIntent = Intent(context, ConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                val configPendingIntent = PendingIntent.getActivity(
                    context,
                    appWidgetId,
                    configIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_layout_view, configPendingIntent)
            }
            val svcIntent = Intent(context, WidgetService::class.java)
            svcIntent.putExtra(CONTENT, widgetContent)
            svcIntent.data = Uri.parse(svcIntent.toUri(Intent.URI_INTENT_SCHEME))
            views.setRemoteAdapter(R.id.lvContent, svcIntent)

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
