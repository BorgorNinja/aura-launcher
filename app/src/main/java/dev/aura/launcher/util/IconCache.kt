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
 *
 * Performance notes:
 * - Icons are pre-scaled to TARGET_PX × TARGET_PX on load (≈52dp @ 2×).
 *   This avoids storing 192×192 full-res bitmaps and removes per-frame
 *   scaling work in the Image composable.
 * - getSync() returns the cached bitmap synchronously (no coroutine).
 *   rememberAppIcon() passes this as produceState's initialValue, so
 *   already-cached icons appear on the FIRST frame — no blank→icon flicker.
 * - Holds 200 icons ≈ 200 × (108×108×4) ≈ 9 MB peak.
 */
object IconCache {

    /** Target bitmap size in pixels — matches ~52dp at 2× density. */
    private const val TARGET_PX = 108

    private val cache = LruCache<String, ImageBitmap>(200)

    /** Synchronous cache lookup — safe to call from the composition thread. */
    fun getSync(packageName: String): ImageBitmap? = cache.get(packageName)

    /** Returns cached bitmap immediately, or loads + caches it off the main thread. */
    suspend fun getOrLoad(packageName: String, pm: PackageManager): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val bmp = pm.getApplicationIcon(packageName).toScaledBitmap()
                cache.put(packageName, bmp)
                bmp
            }.getOrNull()
        }
    }

    /** Bulk-warm the cache for a list of packages (call from a background coroutine). */
    suspend fun preload(packages: List<String>, pm: PackageManager) =
        withContext(Dispatchers.IO) {
            packages.forEach { pkg ->
                if (cache.get(pkg) == null) {
                    runCatching {
                        val bmp = pm.getApplicationIcon(pkg).toScaledBitmap()
                        cache.put(pkg, bmp)
                    }
                }
            }
        }

    fun evict(packageName: String) = cache.remove(packageName)
    fun clear() = cache.evictAll()

    private fun Drawable.toScaledBitmap(): ImageBitmap {
        // Draw at native size first, then scale down to TARGET_PX.
        // Avoids aliasing that setBounds(TARGET, TARGET) alone can cause
        // on some icon shapes.
        val native = Bitmap.createBitmap(
            if (intrinsicWidth  > 0) intrinsicWidth  else TARGET_PX,
            if (intrinsicHeight > 0) intrinsicHeight else TARGET_PX,
            Bitmap.Config.ARGB_8888
        )
        setBounds(0, 0, native.width, native.height)
        draw(Canvas(native))
        return if (native.width == TARGET_PX && native.height == TARGET_PX) {
            native.asImageBitmap()
        } else {
            Bitmap.createScaledBitmap(native, TARGET_PX, TARGET_PX, true)
                .also { native.recycle() }
                .asImageBitmap()
        }
    }
}
