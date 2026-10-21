package dev.flondy.mynotes.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** YELLOW = Gold der Leder-Optik (Name bleibt für gespeicherte Einstellungen erhalten). */
enum class AccentColor(val argb: Long) {
    YELLOW(0xFFF6C445),
    MINT(0xFF5EEAD4),
    LAVENDER(0xFFC4B5FD),
    CORAL(0xFFFDA4AF),
}

enum class CardDensity { COMFORTABLE, COMPACT }

/** Darstellung der Startseite: Raster (2 Spalten), Liste oder Übersicht mit Bereichen. */
enum class HomeLayout { GRID, LIST, OVERVIEW }

enum class SortMode { MANUAL, UPDATED, NEWEST, OLDEST, TITLE_AZ, TITLE_ZA }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.YELLOW,
    val sortMode: SortMode = SortMode.MANUAL,
    val favoritesFirst: Boolean = false,
    val cardDensity: CardDensity = CardDensity.COMFORTABLE,
    val homeLayout: HomeLayout = HomeLayout.GRID,
    val animationsEnabled: Boolean = true,
    val immersiveMode: Boolean = false,
    val hideCheckedItems: Boolean = false,
    val showAdFreeBadge: Boolean = true,
    val appIcon: String = "DEFAULT",
    val trashRetentionDays: Int = 30,
    val debugForceAdFree: Boolean = false,
)
