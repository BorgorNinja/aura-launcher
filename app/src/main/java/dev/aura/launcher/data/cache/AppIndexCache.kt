package dev.aura.launcher.data.cache

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import dev.aura.launcher.data.model.AppInfo
import dev.aura.launcher.util.IconCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * AppIndexCache — builds and maintains the Room app index.
 *
 * warmAsync()       : debounced full scan (safe to call on every onResume)
 * indexPackage()    : upsert a single newly-installed / updated package
 * removePackage()   : delete a single uninstalled package
 *
 * All work is dispatched to Dispatchers.IO and never blocks the main thread.
 */
object AppIndexCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Minimum time between full scans.  Single-package changes (install /
     * uninstall) are handled immediately by indexPackage() / removePackage()
     * via the PackageChangeReceiver, so the full scan is purely a safety net
     * for edge cases (ADB installs, system app updates) — it does not need to
     * run on every onResume.
     */
    private const val FULL_SCAN_DEBOUNCE_MS = 30_000L
    private val lastFullScanMs = AtomicLong(0L)

    // ── Full scan ─────────────────────────────────────────────────────────────

    /**
     * Schedules a full app-list scan if at least [FULL_SCAN_DEBOUNCE_MS] has
     * elapsed since the previous scan.  Safe to call on every onResume; the
     * guard prevents the expensive queryIntentActivities() disk read from
     * running on every home-button press.
     */
    fun warmAsync(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastFullScanMs.get() < FULL_SCAN_DEBOUNCE_MS) return
        lastFullScanMs.set(now)
        scope.launch { fullScan(context) }
    }

    private suspend fun fullScan(context: Context) {
        val pm  = context.packageManager
        val dao = AppDatabase.get(context).appDao()

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        // No GET_META_DATA — we only need the label and package name, not the
        // full APK metadata bundle.  GET_META_DATA forces a read of every app's
        // AndroidManifest metadata from disk, which is the main cost of this
        // call. Removing it cuts query time significantly on devices with many
        // installed apps.
        val resolved = pm.queryIntentActivities(intent, 0)

        val apps = resolved.mapNotNull { ri ->
            runCatching { buildAppInfo(pm, ri.activityInfo.applicationInfo) }.getOrNull()
        }

        dao.upsertAll(apps)
        dao.pruneUninstalled(apps.map { it.packageName })
    }

    // ── Single-package install / update ───────────────────────────────────────

    fun indexPackage(context: Context, packageName: String) {
        scope.launch {
            runCatching {
                val pm = context.packageManager
                pm.getLaunchIntentForPackage(packageName) ?: return@launch
                val ai  = pm.getApplicationInfo(packageName, 0)
                val app = buildAppInfo(pm, ai)
                AppDatabase.get(context).appDao().upsertAll(listOf(app))
                IconCache.evict(packageName)
            }
        }
    }

    // ── Single-package uninstall ──────────────────────────────────────────────

    fun removePackage(context: Context, packageName: String) {
        scope.launch {
            runCatching {
                AppDatabase.get(context).appDao().delete(packageName)
                IconCache.evict(packageName)
            }
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildAppInfo(pm: PackageManager, ai: ApplicationInfo): AppInfo {
        val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ai.category
        else
            ApplicationInfo.CATEGORY_UNDEFINED

        return AppInfo(
            packageName = ai.packageName,
            label       = pm.getApplicationLabel(ai).toString(),
            category    = category
        )
    }
}
