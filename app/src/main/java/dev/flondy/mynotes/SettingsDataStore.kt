package dev.flondy.mynotes // Dein Paketname

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Erstellt eine Instanz von DataStore (nur einmal pro App)
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        val IS_DARK_MODE_ENABLED_KEY = booleanPreferencesKey("is_dark_mode_enabled")
    }

    val isDarkThemeEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_MODE_ENABLED_KEY] ?: false // Standardwert false (hell)
        }

    suspend fun setTheme(isDarkTheme: Boolean) {
        context.dataStore.edit { settings ->
            settings[IS_DARK_MODE_ENABLED_KEY] = isDarkTheme
        }
    }
}
