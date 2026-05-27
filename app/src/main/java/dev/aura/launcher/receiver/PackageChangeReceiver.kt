package dev.aura.launcher.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.aura.launcher.data.cache.AppIndexCache

/**
 * Listens for package install / uninstall / update broadcasts and keeps the
 * Room app index in sync in real-time.
 *
 * Must be registered DYNAMICALLY (not in manifest) — Android 8+ does not
 * deliver package-change broadcasts to statically-declared receivers.
 * Registration happens in AuraApplication so the receiver lives for the full
 * process lifetime (the default launcher process is always alive while awake).
 *
 * Required IntentFilter:
 *   addAction(ACTION_PACKAGE_ADDED / REMOVED / REPLACED / CHANGED)
 *   addDataScheme("package")   ← mandatory, all package broadcasts carry a URI
 */
class PackageChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pkg = intent.data?.schemeSpecificPart ?: return

        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> {
                // Skip the ADDED fired during an update; REPLACED handles that case.
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    AppIndexCache.indexPackage(context, pkg)
                }
            }
            Intent.ACTION_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_CHANGED -> {
                AppIndexCache.indexPackage(context, pkg)
            }
            Intent.ACTION_PACKAGE_REMOVED -> {
                // Skip the REMOVED fired during an update; REPLACED re-indexes after.
                if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    AppIndexCache.removePackage(context, pkg)
                }
            }
        }
    }
}
