package dev.aura.launcher.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import androidx.compose.runtime.compositionLocalOf

const val WIDGET_HOST_ID = 1337

/** Activity-owned SafeAppWidgetHost provided to the Compose tree. */
val LocalWidgetHost = compositionLocalOf<SafeAppWidgetHost?> { null }

fun widgetPickerIntent(host: SafeAppWidgetHost): Intent {
    val id = host.allocateAppWidgetId()
    return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }
}
