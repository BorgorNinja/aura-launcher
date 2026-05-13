package dev.aura.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.runtime.compositionLocalOf

const val WIDGET_HOST_ID = 1337

/** Activity-owned AppWidgetHost provided to the Compose tree. */
val LocalWidgetHost = compositionLocalOf<AppWidgetHost?> { null }

fun widgetPickerIntent(host: AppWidgetHost): Intent {
    val id = host.allocateAppWidgetId()
    return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }
}
