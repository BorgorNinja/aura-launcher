package dev.aura.launcher.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Stub receiver — required so the system recognises Aura as a widget host
 * capable of receiving APPWIDGET_HOST_RESTORED broadcasts after backup/restore.
 */
class AuraWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // No-op for now. Extend here to restore widget state after device restore.
    }
}
