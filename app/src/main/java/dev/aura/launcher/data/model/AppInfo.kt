package dev.aura.launcher.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * AppInfo — lightweight representation of an installed application.
 *
 * Icon bitmaps are NOT stored here. They are lazy-loaded per-item in the UI
 * layer using PackageManager.getApplicationIcon() to keep heap usage low.
 *
 * Category is sourced from ApplicationInfo.category (API 26+), enabling
 * heuristic grouping without any external metadata API.
 */
@Entity(tableName = "app_index")
data class AppInfo(
    @PrimaryKey
    val packageName: String,

    val label: String,

    /**
     * One of: CATEGORY_GAME, CATEGORY_AUDIO, CATEGORY_VIDEO,
     * CATEGORY_IMAGE, CATEGORY_SOCIAL, CATEGORY_NEWS,
     * CATEGORY_MAPS, CATEGORY_PRODUCTIVITY, CATEGORY_UNDEFINED (-1)
     */
    val category: Int,

    /** Used for heuristic fuzzy search ranking — lower = more frequent use. */
    val launchCount: Int = 0,

    val lastLaunchMs: Long = 0L
)
