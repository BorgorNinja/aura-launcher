package dev.aura.launcher.ui.util

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import dev.aura.launcher.util.IconCache

/**
 * Returns the icon for [packageName], optimised for smooth list scrolling.
 *
 * Two-path design:
 *
 * FAST PATH (cache hit, the common case after preload):
 *   `remember(packageName) { IconCache.getSync(packageName) }` runs once per
 *   composition slot and returns the bitmap instantly.  No coroutine is
 *   scheduled, no State is allocated, no recomposition is triggered.
 *   Cost: one LruCache.get() call (~1 µs).
 *
 * SLOW PATH (cache miss, typically only on first launch or new installs):
 *   Falls through to `produceState` which loads the icon off the main thread
 *   and recomposes the item exactly once when the bitmap is ready.
 *
 * [pm] is accepted as a parameter so callers can hoist the PackageManager
 * lookup to the screen level (one LocalContext.current read for the whole
 * list instead of one per item).
 */
@Composable
fun rememberAppIcon(packageName: String, pm: PackageManager): ImageBitmap? {
    // Fast path — bitmap is in cache from the preload that ran when the app
    // list loaded.  remember() stores the result in the composition slot so
    // this block only executes once per item (not on every recomposition).
    val cached = remember(packageName) { IconCache.getSync(packageName) }
    if (cached != null) return cached

    // Slow path — icon not yet cached; load async and return null→bitmap.
    return produceState<ImageBitmap?>(initialValue = null, key1 = packageName) {
        value = IconCache.getOrLoad(packageName, pm)
    }.value
}

/** Convenience overload that reads PackageManager from LocalContext. */
@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val pm = LocalContext.current.packageManager
    return rememberAppIcon(packageName, pm)
}
