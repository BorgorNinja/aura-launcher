package dev.aura.launcher.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Notification-access listener that tracks which installed packages
 * currently have at least one active, user-visible notification — powers
 * the "Notification Dots" setting (a small badge on app icons in the
 * drawer and dock).
 *
 * Requires the user to grant Notification Access once — a special-access
 * permission granted via Settings, not a runtime permission. Mirrors the
 * enable-check / open-settings pattern already used by LockScreenService
 * for its accessibility-service permission.
 */
class NotificationDotService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        _connected.value = true
        refreshFromActiveNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isBadgeable(sbn)) {
            _activePackages.update { it + sbn.packageName }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Another notification from the same package may still be active,
        // so recompute from the live list rather than blindly removing.
        refreshFromActiveNotifications()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _connected.value = false
        _activePackages.value = emptySet()
    }

    private fun refreshFromActiveNotifications() {
        _activePackages.value = runCatching {
            activeNotifications.filter { isBadgeable(it) }.map { it.packageName }.toSet()
        }.getOrDefault(emptySet())
    }

    /**
     * Excludes notifications that shouldn't earn a dot: ongoing / foreground
     * -service notifications (media playback, sync, VPN, etc.) are
     * persistent background noise, not something that needs a glance from
     * the home screen — and group summaries would double-count against the
     * child notification that already counts.
     */
    private fun isBadgeable(sbn: StatusBarNotification): Boolean {
        val flags = sbn.notification.flags
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false
        if (flags and Notification.FLAG_GROUP_SUMMARY != 0) return false
        return true
    }

    companion object {
        private val _activePackages = MutableStateFlow<Set<String>>(emptySet())

        private val _connected = MutableStateFlow(false)

        /** True while the system has this listener bound (access granted + service alive). */
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        /** Packages with at least one active, badge-worthy notification. */
        val activePackages: StateFlow<Set<String>> = _activePackages.asStateFlow()

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            val comp = ComponentName(context, NotificationDotService::class.java).flattenToString()
            return flat.split(":").any { it.equals(comp, ignoreCase = true) }
        }

        fun openSettings(context: Context) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
