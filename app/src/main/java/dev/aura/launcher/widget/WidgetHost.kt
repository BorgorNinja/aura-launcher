package dev.aura.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

const val WIDGET_HOST_ID = 1337

/**
 * Remembers an AppWidgetHost and manages its lifecycle.
 * Call startListening() / stopListening() in Activity onResume/onPause.
 */
@Composable
fun rememberWidgetHost(): AppWidgetHost {
    val context = LocalContext.current
    val host = remember { AppWidgetHost(context, WIDGET_HOST_ID) }
    DisposableEffect(Unit) {
        host.startListening()
        onDispose { host.stopListening() }
    }
    return host
}

/**
 * Renders a single widget by its allocated appWidgetId.
 */
@Composable
fun WidgetView(
    appWidgetId: Int,
    host:        AppWidgetHost,
    modifier:    Modifier = Modifier
) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
    val info    = manager.getAppWidgetInfo(appWidgetId) ?: return

    AndroidView(
        factory  = { ctx ->
            host.createView(ctx, appWidgetId, info) as AppWidgetHostView
        },
        modifier = modifier
    )
}

/**
 * Returns an intent to launch the system widget picker.
 * Start this for result with PICK_WIDGET_REQUEST.
 * On result, allocate an ID via host.allocateAppWidgetId() and bind it.
 */
fun widgetPickerIntent(host: AppWidgetHost): Intent {
    val id = host.allocateAppWidgetId()
    return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
    }
}

const val PICK_WIDGET_REQUEST = 2001
