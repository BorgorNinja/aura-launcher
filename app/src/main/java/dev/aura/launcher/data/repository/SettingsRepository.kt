package dev.aura.launcher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.aura.launcher.data.model.AuraSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("aura_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val KEY_GRID_COLUMNS      = intPreferencesKey("grid_columns")
        val KEY_DARK_THEME_MODE   = stringPreferencesKey("dark_theme_mode")
        val KEY_NOTIFICATION_DOTS = booleanPreferencesKey("notification_dots")
        val KEY_ICON_PACK         = stringPreferencesKey("icon_pack")
        val KEY_WIDGET_IDS        = stringPreferencesKey("widget_ids")
        val KEY_SWIPE_DOWN_ACTION = stringPreferencesKey("swipe_down_action")
        val KEY_DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
        val KEY_COLOR_THEME       = stringPreferencesKey("color_theme")
        val KEY_DOCK_GROUP        = intPreferencesKey("dock_group")
        // 12 slots: 3 groups × 4 slots, pipe-separated; empty string = vacant
        val KEY_DOCK_SLOTS        = stringPreferencesKey("dock_slots")
    }

    val settings: Flow<AuraSettings> = context.dataStore.data.map { prefs ->
        AuraSettings(
            gridColumns      = prefs[KEY_GRID_COLUMNS]      ?: 4,
            darkThemeMode    = prefs[KEY_DARK_THEME_MODE]   ?: "system",
            notificationDots = prefs[KEY_NOTIFICATION_DOTS] ?: true,
            iconPackPackage  = prefs[KEY_ICON_PACK]         ?: "",
            swipeDownAction  = prefs[KEY_SWIPE_DOWN_ACTION] ?: "notifications",
            doubleTapAction  = prefs[KEY_DOUBLE_TAP_ACTION] ?: "none",
            colorTheme       = prefs[KEY_COLOR_THEME]       ?: "dynamic",
            activeDockGroup  = prefs[KEY_DOCK_GROUP]        ?: 0
        )
    }

    val widgetIds: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        (prefs[KEY_WIDGET_IDS] ?: "")
            .split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
    }

    /**
     * Emits exactly 12 items (3 groups × 4 slots). null = vacant.
     * Migrates legacy 4-item format by placing those apps in group 0.
     */
    val dockSlots: Flow<List<String?>> = context.dataStore.data.map { prefs ->
        val raw   = prefs[KEY_DOCK_SLOTS] ?: ""
        val parts = raw.split("|").map { it.ifEmpty { null } }
        when {
            parts.size >= 12 -> parts.take(12)
            // Legacy: 4-slot string → promote to group 0, groups 1+2 empty
            parts.isNotEmpty() -> (parts.take(4) + List(8) { null })
            else               -> List(12) { null }
        }
    }

    suspend fun setGridColumns(value: Int)          = context.dataStore.edit { it[KEY_GRID_COLUMNS]      = value.coerceIn(3, 6) }
    suspend fun setDarkThemeMode(value: String)     = context.dataStore.edit { it[KEY_DARK_THEME_MODE]   = value }
    suspend fun setNotificationDots(value: Boolean) = context.dataStore.edit { it[KEY_NOTIFICATION_DOTS] = value }
    suspend fun setIconPack(pkg: String)            = context.dataStore.edit { it[KEY_ICON_PACK]         = pkg }
    suspend fun setSwipeDownAction(action: String)  = context.dataStore.edit { it[KEY_SWIPE_DOWN_ACTION] = action }
    suspend fun setDoubleTapAction(action: String)  = context.dataStore.edit { it[KEY_DOUBLE_TAP_ACTION] = action }
    suspend fun setColorTheme(theme: String)        = context.dataStore.edit { it[KEY_COLOR_THEME]       = theme }
    suspend fun setDockGroup(group: Int)            = context.dataStore.edit { it[KEY_DOCK_GROUP]        = group.coerceIn(0, 2) }

    suspend fun setDockSlot(absIndex: Int, packageName: String?) {
        context.dataStore.edit { prefs ->
            val current = (prefs[KEY_DOCK_SLOTS] ?: "")
            val parts   = current.split("|").toMutableList()
            // Ensure list is 12 items
            while (parts.size < 12) parts.add("")
            parts[absIndex.coerceIn(0, 11)] = packageName ?: ""
            prefs[KEY_DOCK_SLOTS] = parts.joinToString("|")
        }
    }

    suspend fun addWidgetId(id: Int) = context.dataStore.edit { prefs ->
        val ids = (prefs[KEY_WIDGET_IDS] ?: "").split(",").filter { it.isNotBlank() }.toMutableList()
        if (!ids.contains(id.toString())) ids.add(id.toString())
        prefs[KEY_WIDGET_IDS] = ids.joinToString(",")
    }

    suspend fun removeWidgetId(id: Int) = context.dataStore.edit { prefs ->
        val ids = (prefs[KEY_WIDGET_IDS] ?: "")
            .split(",").filter { it.isNotBlank() && it != id.toString() }
        prefs[KEY_WIDGET_IDS] = ids.joinToString(",")
    }
}
