package dev.aura.launcher.data.repository

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import dev.aura.launcher.data.cache.AppDatabase
import dev.aura.launcher.data.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {

    private val dao = AppDatabase.get(context).appDao()
    private val pm  = context.packageManager

    val allApps: Flow<List<AppInfo>> = dao.observeAll()

    suspend fun search(query: String): List<AppInfo> =
        withContext(Dispatchers.IO) { dao.search(query) }

    fun byCategory(cat: Int): Flow<List<AppInfo>> = dao.byCategory(cat)

    suspend fun iconFor(packageName: String): Drawable? =
        withContext(Dispatchers.IO) {
            runCatching { pm.getApplicationIcon(packageName) }.getOrNull()
        }

    suspend fun launch(context: Context, packageName: String) {
        val intent = pm.getLaunchIntentForPackage(packageName)
            ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } ?: return
        context.startActivity(intent)
        withContext(Dispatchers.IO) { dao.recordLaunch(packageName) }
    }

    suspend fun uninstall(context: Context, packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun openAppInfo(context: Context, packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    suspend fun pickWallpaper(context: Context) {
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Fall back to generic wallpaper picker if live wallpaper picker unavailable
        val target = if (pm.resolveActivity(intent, 0) != null) intent
                     else Intent(Intent.ACTION_SET_WALLPAPER).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(target)
    }
}
