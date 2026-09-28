package dev.aura.launcher.ui.util

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Small badge drawn at the top-end corner of an app icon when that app has
 * an active notification. Must be called inside a Box that wraps the icon
 * so the dot is positioned relative to the icon bounds, not the label.
 */
@Composable
fun BoxScope.NotificationDot() {
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(12.dp)
            .background(MaterialTheme.colorScheme.error, CircleShape)
            .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
    )
}
