package dev.flondy.mynotes.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentColor(val argb: Long) {
    YELLOW(0xFFFDEE51),
    MINT(0xFF5EEAD4),
    LAVENDER(0xFFC4B5FD),
    CORAL(0xFFFDA4AF),
}

enum class CardDensity { COMFORTABLE, COMPACT }

enum class SortMode { MANUAL, UPDATED, NEWEST, OLDEST, TITLE_AZ, TITLE_ZA }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentColor = AccentColor.YELLOW,
    val sortMode: SortMode = SortMode.MANUAL,
    val favoritesFirst: Boolean = false,
    val cardDensity: CardDensity = CardDensity.COMFORTABLE,
    val animationsEnabled: Boolean = true,
    val immersiveMode: Boolean = false,
    val hideCheckedItems: Boolean = false,
    val showAdFreeBadge: Boolean = true,
    val appIcon: String = "DEFAULT",
    val trashRetentionDays: Int = 30,
    val debugForceAdFree: Boolean = false,
)
