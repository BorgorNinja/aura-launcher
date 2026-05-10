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
        val KEY_GRID_COLUMNS       = intPreferencesKey("grid_columns")
        val KEY_DARK_THEME_MODE    = stringPreferencesKey("dark_theme_mode")
        val KEY_NOTIFICATION_DOTS  = booleanPreferencesKey("notification_dots")
        val KEY_ICON_PACK          = stringPreferencesKey("icon_pack")
    }

    val settings: Flow<AuraSettings> = context.dataStore.data.map { prefs ->
        AuraSettings(
            gridColumns      = prefs[KEY_GRID_COLUMNS]      ?: 4,
            darkThemeMode    = prefs[KEY_DARK_THEME_MODE]   ?: "system",
            notificationDots = prefs[KEY_NOTIFICATION_DOTS] ?: true,
            iconPackPackage  = prefs[KEY_ICON_PACK]         ?: ""
        )
    }

    suspend fun setGridColumns(value: Int) =
        context.dataStore.edit { it[KEY_GRID_COLUMNS] = value.coerceIn(3, 6) }

    suspend fun setDarkThemeMode(value: String) =
        context.dataStore.edit { it[KEY_DARK_THEME_MODE] = value }

    suspend fun setNotificationDots(value: Boolean) =
        context.dataStore.edit { it[KEY_NOTIFICATION_DOTS] = value }

    suspend fun setIconPack(packageName: String) =
        context.dataStore.edit { it[KEY_ICON_PACK] = packageName }
}
