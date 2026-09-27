package dev.aura.launcher.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dev.aura.launcher.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "AuraWidgetReceiver"

/**
 * Remaps stored widget IDs after a device backup/restore.
 *
 * Restored widget host IDs are never the same as the IDs that existed on
 * the original device — Android hands back the old->new mapping via
 * EXTRA_HOST_OLD_IDS / EXTRA_HOST_NEW_IDS on this broadcast. Previously this
 * receiver was a no-op, so every widget ID persisted in SettingsRepository
 * pointed at nothing after a restore and the dashboard silently showed no
 * widgets (WidgetDashboardScreen skips any ID for which
 * AppWidgetManager.getAppWidgetInfo() returns null).
 */
class AuraWidgetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AppWidgetManager.ACTION_APPWIDGET_HOST_RESTORED) return

        val oldIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_HOST_OLD_IDS)
        val newIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_HOST_NEW_IDS)
        if (oldIds == null || newIds == null || oldIds.size != newIds.size || oldIds.isEmpty()) return

        val mapping    = oldIds.zip(newIds).toMap()
        val appContext = context.applicationContext
        val pending    = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            runCatching { SettingsRepository(appContext).remapWidgetIds(mapping) }
                .onFailure { Log.e(TAG, "Widget ID remap failed", it) }
            pending.finish()
        }
    }
}
