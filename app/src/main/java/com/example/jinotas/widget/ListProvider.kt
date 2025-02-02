package com.example.jinotas.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.jinotas.R
import com.example.jinotas.widget.WidgetService.Companion.CONTENT

class ListProvider(private val context: Context, intent: Intent) :
    RemoteViewsService.RemoteViewsFactory {
    private val content: String

    init {
        content = intent.getStringExtra(CONTENT) ?: ""
    }

    override fun onCreate() {}

    override fun onDataSetChanged() {}

    override fun onDestroy() {}

    override fun getCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    /*
     *Similar to getView of Adapter where instead of View
     *we return RemoteViews
     *
     */
    override fun getViewAt(position: Int): RemoteViews {
        val remoteView = RemoteViews(
            context.packageName, R.layout.widget_list_item
        )

        remoteView.setTextViewText(R.id.widget_textcontent, content)
        return remoteView
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }
}