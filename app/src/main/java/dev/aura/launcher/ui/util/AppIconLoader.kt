package dev.aura.launcher.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import dev.aura.launcher.util.IconCache

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val pm = LocalContext.current.packageManager
    // KEY PERFORMANCE FIX: pass getSync() as initialValue.
    // If the icon is already in the LruCache (warmed during app-list load),
    // it is returned on the very first frame — no blank placeholder, no
    // recomposition, no visible flicker when scrolling the drawer.
    return produceState<ImageBitmap?>(
        initialValue = IconCache.getSync(packageName),
        key1         = packageName
    ) {
        value = IconCache.getOrLoad(packageName, pm)
    }.value
}
