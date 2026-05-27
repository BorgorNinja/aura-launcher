package dev.aura.launcher

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import dev.aura.launcher.data.cache.AppIndexCache
import dev.aura.launcher.receiver.PackageChangeReceiver

class AuraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Warm the Room index on startup so the drawer is populated immediately.
        AppIndexCache.warmAsync(this)

        // Register package-change receiver dynamically.
        // Static manifest receivers do NOT receive package broadcasts on API 26+,
        // so this is the only way to get real-time install/uninstall updates.
        // The receiver stays alive for the full process lifetime — for a default
        // launcher the process is always running while the device is awake.
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            // REQUIRED: package broadcasts carry a "package:" URI in their data field.
            // Without this, the receiver never fires.
            addDataScheme("package")
        }
        registerReceiver(PackageChangeReceiver(), filter)
    }
}
