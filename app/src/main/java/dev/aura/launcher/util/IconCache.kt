package dev.aura.launcher.util

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Process-scoped LRU icon cache.
 * Holds up to 150 decoded icons in RAM (~30–40 MB peak for 52dp icons).
 * Cleared automatically when the process dies (reboot / force-stop).
 * No disk I/O — all reads/writes are in-memory.
 */
object IconCache {

    private val cache = LruCache<String, ImageBitmap>(150)

    suspend fun get(packageName: String, pm: PackageManager): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val bmp = pm.getApplicationIcon(packageName).toImageBitmap()
                cache.put(packageName, bmp)
                bmp
            }.getOrNull()
        }
    }

    fun evict(packageName: String) = cache.remove(packageName)

    fun clear() = cache.evictAll()

    private fun Drawable.toImageBitmap(): ImageBitmap {
        val w   = if (intrinsicWidth  > 0) intrinsicWidth  else 108
        val h   = if (intrinsicHeight > 0) intrinsicHeight else 108
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, w, h)
        draw(Canvas(bmp))
        return bmp.asImageBitmap()
    }
}
