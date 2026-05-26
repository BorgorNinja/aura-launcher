package dev.aura.launcher.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.Log
import android.widget.RemoteViews

private const val TAG = "AuraWidgetHost"

/**
 * AppWidgetHostView subclass that wraps every RemoteViews operation in a
 * try-catch.  Several widget providers (e.g. Pinterest Wallpaper Wall, some
 * media players) send RemoteViews that contain custom/third-party view classes
 * or large bitmaps.  When those fail to inflate, Android throws on the main
 * thread inside updateAppWidget() — completely outside the AndroidView factory
 * try-catch — crashing the entire launcher Activity.
 */
class SafeAppWidgetHostView(context: Context) : AppWidgetHostView(context) {

    var onError: (() -> Unit)? = null

    override fun updateAppWidget(remoteViews: RemoteViews?) {
        try {
            super.updateAppWidget(remoteViews)
        } catch (e: Exception) {
            Log.e(TAG, "updateAppWidget failed for widget $appWidgetId", e)
            onError?.invoke()
        }
    }

    override fun prepareView(view: android.view.View?) {
        try {
            super.prepareView(view)
        } catch (e: Exception) {
            Log.e(TAG, "prepareView failed for widget $appWidgetId", e)
        }
    }
}

/**
 * AppWidgetHost subclass that always returns a SafeAppWidgetHostView so that
 * no widget can crash the launcher, regardless of how broken its RemoteViews are.
 */
class SafeAppWidgetHost(context: Context, hostId: Int) : AppWidgetHost(context, hostId) {
    override fun onCreateView(
        context:     Context,
        appWidgetId: Int,
        appWidget:   AppWidgetProviderInfo?
    ): AppWidgetHostView = SafeAppWidgetHostView(context)
}
