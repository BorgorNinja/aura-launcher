package dev.aura.launcher.data.cache

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dev.aura.launcher.data.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AppIndexCache
 *
 * Reads installed packages via PackageManager, derives category metadata
 * from ApplicationInfo.category (API 26), and writes the result to Room.
 *
 * All work runs on Dispatchers.IO — never blocks the main thread.
 * No network calls. No external metadata service.
 */
object AppIndexCache {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun warmAsync(context: Context) {
        scope.launch {
            val pm  = context.packageManager
            val db  = AppDatabase.get(context)
            val dao = db.appDao()

            val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }

            val resolved = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)

            val apps = resolved.mapNotNull { ri ->
                runCatching {
                    val ai  = ri.activityInfo.applicationInfo
                    val pkg = ai.packageName
                    AppInfo(
                        packageName = pkg,
                        label       = pm.getApplicationLabel(ai).toString(),
                        category    = if (android.os.Build.VERSION.SDK_INT >= 26)
                                          ai.category
                                      else
                                          ApplicationInfo.CATEGORY_UNDEFINED
                    )
                }.getOrNull()
            }

            dao.upsertAll(apps)
            dao.pruneUninstalled(apps.map { it.packageName })
        }
    }
}
