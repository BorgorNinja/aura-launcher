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

/**
 * AppIndexCache — builds and maintains the Room app index.
 *
 * warmAsync()       : full scan on startup (runs once on Application.onCreate)
 * indexPackage()    : upsert a single newly-installed / updated package
 * removePackage()   : delete a single uninstalled package
 *
 * All work is dispatched to Dispatchers.IO and never blocks the main thread.
 */
object AppIndexCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Full scan ─────────────────────────────────────────────────────────────

    fun warmAsync(context: Context) {
        scope.launch { fullScan(context) }
    }

    private suspend fun fullScan(context: Context) {
        val pm  = context.packageManager
        val dao = AppDatabase.get(context).appDao()

        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

        val apps = resolved.mapNotNull { ri ->
            runCatching { buildAppInfo(pm, ri.activityInfo.applicationInfo) }.getOrNull()
        }

        dao.upsertAll(apps)
        dao.pruneUninstalled(apps.map { it.packageName })
    }

    // ── Single-package install / update ───────────────────────────────────────

    /**
     * Called by PackageChangeReceiver when ACTION_PACKAGE_ADDED or
     * ACTION_PACKAGE_REPLACED is received. Upserts the app and evicts its
     * cached icon so the fresh icon is loaded on next render.
     */
    fun indexPackage(context: Context, packageName: String) {
        scope.launch {
            runCatching {
                val pm = context.packageManager

                // Only index apps that have a launcher entry (skips libraries, services, etc.)
                val launchIntent = pm.getLaunchIntentForPackage(packageName) ?: return@launch

                val ai  = pm.getApplicationInfo(packageName, 0)
                val app = buildAppInfo(pm, ai)

                AppDatabase.get(context).appDao().upsertAll(listOf(app))
                IconCache.evict(packageName)
            }
        }
    }

    // ── Single-package uninstall ──────────────────────────────────────────────

    /**
     * Called by PackageChangeReceiver when ACTION_PACKAGE_REMOVED is received
     * (and the removal is not part of a package replacement/update).
     */
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
