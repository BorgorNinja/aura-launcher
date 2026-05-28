package dev.aura.launcher.data.model

import androidx.compose.runtime.Stable
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AppInfo — lightweight representation of an installed application.
 *
 * @Stable tells the Compose compiler that:
 *   1. equals() is consistent across calls (data class guarantees this).
 *   2. If two instances compare equal, their public properties are equal.
 *   3. Recomposition will be notified whenever a property changes.
 *
 * Without @Stable, the Compose runtime treats AppInfo as UNSTABLE, meaning
 * it must recompose every AppGridItem on every parent recomposition — even
 * when the item's data hasn't changed. With @Stable, Compose uses == to
 * check for changes and SKIPS recomposition for unchanged items.
 *
 * This is the single largest performance improvement for the app drawer:
 * scrolling no longer recomposes all visible items on unrelated state updates.
 */
@Stable
@Entity(tableName = "app_index")
data class AppInfo(
    @PrimaryKey
    val packageName: String,
    val label: String,
    val category: Int,
    val launchCount: Int = 0,
    val lastLaunchMs: Long = 0L
)
