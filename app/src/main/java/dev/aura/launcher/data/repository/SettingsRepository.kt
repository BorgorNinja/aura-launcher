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
        val KEY_WIDGET_IDS        = stringPreferencesKey("widget_ids")   // comma-separated ints
    }

    val settings: Flow<AuraSettings> = context.dataStore.data.map { prefs ->
        AuraSettings(
            gridColumns      = prefs[KEY_GRID_COLUMNS]      ?: 4,
            darkThemeMode    = prefs[KEY_DARK_THEME_MODE]   ?: "system",
            notificationDots = prefs[KEY_NOTIFICATION_DOTS] ?: true,
            iconPackPackage  = prefs[KEY_ICON_PACK]         ?: ""
        )
    }

    val widgetIds: Flow<List<Int>> = context.dataStore.data.map { prefs ->
        (prefs[KEY_WIDGET_IDS] ?: "")
            .split(",")
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
    }

    suspend fun setGridColumns(value: Int) =
        context.dataStore.edit { it[KEY_GRID_COLUMNS] = value.coerceIn(3, 6) }

    suspend fun setDarkThemeMode(value: String) =
        context.dataStore.edit { it[KEY_DARK_THEME_MODE] = value }

    suspend fun setNotificationDots(value: Boolean) =
        context.dataStore.edit { it[KEY_NOTIFICATION_DOTS] = value }

    suspend fun setIconPack(pkg: String) =
        context.dataStore.edit { it[KEY_ICON_PACK] = pkg }

    suspend fun addWidgetId(id: Int) = context.dataStore.edit { prefs ->
        val current = prefs[KEY_WIDGET_IDS] ?: ""
        val ids = current.split(",").filter { it.isNotBlank() }.toMutableList()
        if (!ids.contains(id.toString())) ids.add(id.toString())
        prefs[KEY_WIDGET_IDS] = ids.joinToString(",")
    }

    suspend fun removeWidgetId(id: Int) = context.dataStore.edit { prefs ->
        val ids = (prefs[KEY_WIDGET_IDS] ?: "")
            .split(",")
            .filter { it.isNotBlank() && it != id.toString() }
        prefs[KEY_WIDGET_IDS] = ids.joinToString(",")
    }
}
