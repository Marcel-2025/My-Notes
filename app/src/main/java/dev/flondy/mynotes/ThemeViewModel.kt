package dev.flondy.mynotes // Dein Paketname

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// AndroidViewModel verwenden, um Zugriff auf den Application Context zu haben
class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsDataStore = SettingsDataStore(application)

    // isDarkThemeEnabled wird jetzt aus dem DataStore gelesen und als StateFlow bereitgestellt
    val isDarkThemeEnabled = settingsDataStore.isDarkThemeEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000), // Empfohlene Konfiguration
            initialValue = false // Anfänglicher Standardwert, bevor DataStore geladen hat
        )

    fun toggleTheme() {
        viewModelScope.launch {
            val currentTheme = isDarkThemeEnabled.value // Den aktuellen Wert aus dem StateFlow holen
            settingsDataStore.setTheme(!currentTheme)
        }
    }
}
