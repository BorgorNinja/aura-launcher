package dev.aura.launcher

import android.app.Application
import dev.aura.launcher.data.cache.AppIndexCache

/**
 * AuraApplication
 *
 * Deliberately lightweight. No third-party SDKs, no reflection-heavy DI frameworks.
 * Room database and DataStore are initialized lazily on first access.
 */
class AuraApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Warm the app index cache on a background thread so the home screen
        // renders immediately without blocking the main thread.
        AppIndexCache.warmAsync(this)
    }
}
