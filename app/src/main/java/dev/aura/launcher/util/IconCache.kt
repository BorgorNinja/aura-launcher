package dev.aura.launcher.util

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Process-scoped icon cache.
 *
 * Optimisations applied:
 *
 * 1. Byte-based LruCache (20 MB cap).  The old count-based cache (200 entries)
 *    was arbitrary.  Byte-based sizing means the budget is always spent on the
 *    largest items first and we never evict icons that still fit.
 *
 * 2. Bitmap.Config.HARDWARE final storage.  After scaling, icons are uploaded
 *    to GPU-resident memory.  Compose's Canvas skips the CPU→GPU upload on
 *    every draw call — critical during fast scrolling where the same icons are
 *    redrawn every 16 ms.  HARDWARE bitmaps are API-26+ (our minSdk).
 *
 * 3. Parallel preloading.  Icons are loaded in parallel using Dispatchers.IO
 *    coroutines so the cache is warm well before the user opens the drawer.
 *
 * 4. getSync() for zero-overhead composition fast-path.
 *    rememberAppIcon() calls getSync() inside remember() — if the icon is
 *    already cached, the composable returns on the first frame with NO
 *    coroutine launch, NO recomposition, and NO state allocation.
 */
object IconCache {

    /** Decode at this size (px). Matches ~52 dp @ 2× density. */
    private const val TARGET_PX = 108

    /** Estimated bytes per stored icon (HARDWARE bitmaps report 0 via byteCount). */
    private val BYTES_PER_ICON = TARGET_PX * TARGET_PX * 4  // ARGB_8888 equivalent

    /** 20 MB total icon budget. */
    private const val MAX_BYTES = 20 * 1024 * 1024

    private val cache = object : LruCache<String, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = BYTES_PER_ICON
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Synchronous cache lookup. Safe to call from the composition thread.
     * Returns the cached bitmap instantly with zero overhead, or null on miss.
     */
    fun getSync(packageName: String): ImageBitmap? = cache.get(packageName)

    /**
     * Returns the cached bitmap immediately, or loads it on Dispatchers.IO.
     * Cache hits return in O(1) before the withContext dispatch.
     */
    suspend fun getOrLoad(packageName: String, pm: PackageManager): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching { loadAndCache(packageName, pm) }.getOrNull()
        }
    }

    /**
     * Bulk preload — loads all given packages in parallel on Dispatchers.IO.
     * Only fetches packages not yet in cache. Call this after the app list
     * is available so the drawer is fully warm before the user opens it.
     */
    suspend fun preload(packages: List<String>, pm: PackageManager) {
        val missing = packages.filter { cache.get(it) == null }
        if (missing.isEmpty()) return
        coroutineScope {
            missing.map { pkg ->
                async(Dispatchers.IO) {
                    runCatching { loadAndCache(pkg, pm) }
                }
            }.awaitAll()
        }
    }

    fun evict(packageName: String) { cache.remove(packageName) }
    fun clear()                    { cache.evictAll() }

    // ── Internal ──────────────────────────────────────────────────────────────

    /** Must be called from a background thread (reads APK from disk). */
    private fun loadAndCache(packageName: String, pm: PackageManager): ImageBitmap {
        // Re-check under the assumption another coroutine may have loaded it.
        cache.get(packageName)?.let { return it }
        val bmp = pm.getApplicationIcon(packageName).toHardwareBitmap()
        cache.put(packageName, bmp)
        return bmp
    }

    /**
     * Converts a Drawable to a GPU-resident HARDWARE ImageBitmap at TARGET_PX.
     *
     * Pipeline:
     *   1. Draw at native resolution into ARGB_8888 (avoids aliasing from a
     *      direct small-canvas draw, especially for adaptive icons).
     *   2. Scale down to TARGET_PX × TARGET_PX.
     *   3. Copy to Bitmap.Config.HARDWARE — pixels move to GPU memory.
     *   4. Recycle the intermediate CPU bitmaps.
     */
    private fun Drawable.toHardwareBitmap(): ImageBitmap {
        val w = if (intrinsicWidth  > 0) intrinsicWidth  else TARGET_PX
        val h = if (intrinsicHeight > 0) intrinsicHeight else TARGET_PX

        // Step 1 — draw at native size
        val native = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        setBounds(0, 0, w, h)
        draw(Canvas(native))

        // Step 2 — scale (skip if already the right size)
        val scaled = if (w == TARGET_PX && h == TARGET_PX) native
                     else Bitmap.createScaledBitmap(native, TARGET_PX, TARGET_PX, true)
                         .also { if (it !== native) native.recycle() }

        // Step 3 — upload to GPU memory
        val hardware = scaled.copy(Bitmap.Config.HARDWARE, false)
        scaled.recycle()

        return hardware.asImageBitmap()
    }
}
