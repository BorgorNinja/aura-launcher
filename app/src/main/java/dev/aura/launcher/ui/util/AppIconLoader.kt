package dev.aura.launcher.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import dev.aura.launcher.util.IconCache

@Composable
fun rememberAppIcon(packageName: String): ImageBitmap? {
    val pm = LocalContext.current.packageManager
    return produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = IconCache.get(packageName, pm)
    }.value
}
