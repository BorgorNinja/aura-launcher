package dev.aura.launcher.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent

/**
 * Minimal AccessibilityService used solely to call GLOBAL_ACTION_LOCK_SCREEN.
 * The user must enable it once in Settings → Accessibility → Aura Launcher.
 */
class LockScreenService : AccessibilityService() {

    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    companion object {
        @Volatile private var instance: LockScreenService? = null

        /** Returns true if the lock was triggered, false if the service isn't enabled. */
        fun lock(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) ?: false

        fun isEnabled(context: Context): Boolean {
            val flat = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            val comp = ComponentName(context, LockScreenService::class.java).flattenToString()
            return flat.split(":").any { it.equals(comp, ignoreCase = true) }
        }

        fun openSettings(context: Context) {
            context.startActivity(
                android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}
