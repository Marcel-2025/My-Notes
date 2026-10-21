package dev.flondy.mynotes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.flondy.mynotes.settings.AccentColor
import dev.flondy.mynotes.settings.AppSettings
import dev.flondy.mynotes.settings.CardDensity
import dev.flondy.mynotes.settings.HomeLayout
import dev.flondy.mynotes.settings.SortMode
import dev.flondy.mynotes.settings.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Name "settings" unverändert lassen – dort liegt die Dark-Mode-Einstellung bestehender Nutzer.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Alle App-Einstellungen, dauerhaft in DataStore gespeichert. */
class SettingsDataStore(private val context: Context) {

    companion object {
        /** Alt (bis v1.2): true = dunkel, false = hell. Wird beim Lesen in [THEME_MODE] übersetzt. */
        val IS_DARK_MODE_ENABLED_KEY = booleanPreferencesKey("is_dark_mode_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val SORT_MODE = stringPreferencesKey("sort_mode")
        val FAVORITES_FIRST = booleanPreferencesKey("favorites_first")
        val CARD_DENSITY = stringPreferencesKey("card_density")
        val HOME_LAYOUT = stringPreferencesKey("home_layout")
        val ANIMATIONS = booleanPreferencesKey("animations_enabled")
        val IMMERSIVE = booleanPreferencesKey("immersive_mode")
        val HIDE_CHECKED = booleanPreferencesKey("hide_checked_items")
        val SHOW_AD_FREE_BADGE = booleanPreferencesKey("show_ad_free_badge")
        val APP_ICON = stringPreferencesKey("app_icon")
        val TRASH_RETENTION = intPreferencesKey("trash_retention_days")
        val DEBUG_FORCE_AD_FREE = booleanPreferencesKey("debug_force_ad_free")
        val CATEGORIES_SEEDED = booleanPreferencesKey("categories_seeded")
        val AD_FREE_CACHE = booleanPreferencesKey("ad_free_entitlement_cache")

        private inline fun <reified T : Enum<T>> parse(value: String?, default: T): T =
            value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { p ->
        val themeMode = p[THEME_MODE]?.let { parse(it, ThemeMode.SYSTEM) }
            ?: when (p[IS_DARK_MODE_ENABLED_KEY]) {
                true -> ThemeMode.DARK
                false -> ThemeMode.LIGHT
                null -> ThemeMode.SYSTEM
            }
        AppSettings(
            themeMode = themeMode,
            accent = parse(p[ACCENT], AccentColor.YELLOW),
            sortMode = parse(p[SORT_MODE], SortMode.MANUAL),
            favoritesFirst = p[FAVORITES_FIRST] ?: false,
            cardDensity = parse(p[CARD_DENSITY], CardDensity.COMFORTABLE),
            homeLayout = parse(p[HOME_LAYOUT], HomeLayout.GRID),
            animationsEnabled = p[ANIMATIONS] ?: true,
            immersiveMode = p[IMMERSIVE] ?: false,
            hideCheckedItems = p[HIDE_CHECKED] ?: false,
            showAdFreeBadge = p[SHOW_AD_FREE_BADGE] ?: true,
            appIcon = p[APP_ICON] ?: "DEFAULT",
            trashRetentionDays = p[TRASH_RETENTION] ?: 30,
            debugForceAdFree = BuildConfig.DEBUG && (p[DEBUG_FORCE_AD_FREE] ?: false),
        )
    }

    /** Kompatibilität zur alten API. */
    val isDarkThemeEnabledFlow: Flow<Boolean> = settingsFlow.map { it.themeMode == ThemeMode.DARK }

    suspend fun setTheme(isDarkTheme: Boolean) = setThemeMode(if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT)

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[THEME_MODE] = mode.name }
    suspend fun setAccent(accent: AccentColor) = context.dataStore.edit { it[ACCENT] = accent.name }
    suspend fun setSortMode(mode: SortMode) = context.dataStore.edit { it[SORT_MODE] = mode.name }
    suspend fun setFavoritesFirst(value: Boolean) = context.dataStore.edit { it[FAVORITES_FIRST] = value }
    suspend fun setCardDensity(value: CardDensity) = context.dataStore.edit { it[CARD_DENSITY] = value.name }
    suspend fun setHomeLayout(value: HomeLayout) = context.dataStore.edit { it[HOME_LAYOUT] = value.name }
    suspend fun setAnimationsEnabled(value: Boolean) = context.dataStore.edit { it[ANIMATIONS] = value }
    suspend fun setImmersiveMode(value: Boolean) = context.dataStore.edit { it[IMMERSIVE] = value }
    suspend fun setHideCheckedItems(value: Boolean) = context.dataStore.edit { it[HIDE_CHECKED] = value }
    suspend fun setShowAdFreeBadge(value: Boolean) = context.dataStore.edit { it[SHOW_AD_FREE_BADGE] = value }
    suspend fun setAppIcon(value: String) = context.dataStore.edit { it[APP_ICON] = value }
    suspend fun setDebugForceAdFree(value: Boolean) = context.dataStore.edit { it[DEBUG_FORCE_AD_FREE] = value }

    suspend fun isCategoriesSeeded(): Boolean = context.dataStore.data.first()[CATEGORIES_SEEDED] ?: false
    suspend fun markCategoriesSeeded() = context.dataStore.edit { it[CATEGORIES_SEEDED] = true }

    /**
     * Zwischenspeicher der Werbefrei-Berechtigung, damit beim App-Start keine Werbung aufblitzt.
     * Wird bei jedem Start durch die Abfrage bei Google Play bestätigt oder widerrufen.
     */
    val adFreeCacheFlow: Flow<Boolean> = context.dataStore.data.map { it[AD_FREE_CACHE] ?: false }
    suspend fun setAdFreeCache(value: Boolean) = context.dataStore.edit { it[AD_FREE_CACHE] = value }
}
