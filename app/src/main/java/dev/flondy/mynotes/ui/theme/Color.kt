package dev.flondy.mynotes.ui.theme

import androidx.compose.ui.graphics.Color

// ---------------------------------------------------------------- Leder & Papier (Design 2.1)
// Leder (Kopfzeilen, Akzentflächen)
val Leather = Color(0xFF5A3426)
val LeatherLight = Color(0xFF74452F)
val LeatherDark = Color(0xFF3E2219)
val LeatherDeep = Color(0xFF2A1710)
val OnLeather = Color(0xFFFFF3DC)

// Papier (Hintergrund, Karten)
val PaperBackground = Color(0xFFF8EBD7)
val PaperCard = Color(0xFFFFF8EA)
val PaperCardHigh = Color(0xFFFFFBF2)
val PaperLine = Color(0xFFEAD8BC)
val PaperSunken = Color(0xFFF1E2C9)

// Tinte (Text auf Papier)
val InkBrown = Color(0xFF3B2418)
val InkBrownSoft = Color(0xFF7A6352)
val InkBrownFaint = Color(0xFFA8927F)

// Akzente
val Gold = Color(0xFFF6C445)
val GoldDeep = Color(0xFFE0A526)
val GoldLight = Color(0xFFFFE7A3)
val OnGold = Color(0xFF3E2219)
val Leaf = Color(0xFF6DAA2C)
val Berry = Color(0xFFD9443A)

// Abend-Variante (dunkel)
val NightBackground = Color(0xFF1C130E)
val NightSurface = Color(0xFF261A13)
val NightCard = Color(0xFF2F2118)
val NightCardHigh = Color(0xFF3A291E)
val NightLine = Color(0xFF4A3527)
val NightText = Color(0xFFF6E9D5)
val NightTextSoft = Color(0xFFC9B39B)

// Kompatibilität (alte Namen, z. B. Premium-Seite)
val BrandYellow = Gold
val OnAccent = OnGold
val ErrorLight = Color(0xFFC0392B)
val ErrorDark = Color(0xFFFF8A7A)

/**
 * Notizfarben (Index wird in der DB gespeichert – Reihenfolge nicht ändern, nur ergänzen).
 * Warme Papiertöne für hell, gedeckte Töne für die Abend-Variante, kräftiger Punkt für die Auswahl.
 */
data class NoteColor(val light: Color, val dark: Color, val swatch: Color)

val NoteColors = listOf(
    NoteColor(Color.Unspecified, Color.Unspecified, Color(0xFFE9DCC7)), // 0 Papier (Standard)
    NoteColor(Color(0xFFFFF2C2), Color(0xFF3A3018), Color(0xFFF6C445)), // 1 Gelb
    NoteColor(Color(0xFFFFE3C4), Color(0xFF3B2716), Color(0xFFF4A259)), // 2 Orange
    NoteColor(Color(0xFFFCDCD5), Color(0xFF3C1F1B), Color(0xFFE9806E)), // 3 Koralle
    NoteColor(Color(0xFFF9DDE7), Color(0xFF3A1E29), Color(0xFFE79BB5)), // 4 Rosa
    NoteColor(Color(0xFFE9E1F6), Color(0xFF2A2338), Color(0xFFB39DDB)), // 5 Lavendel
    NoteColor(Color(0xFFDCEAF6), Color(0xFF1C2A38), Color(0xFF7FB3DD)), // 6 Blau
    NoteColor(Color(0xFFE2F0D2), Color(0xFF213020), Color(0xFF8CC36A)), // 7 Grün
    NoteColor(Color(0xFFEDE6DC), Color(0xFF2E2924), Color(0xFFB8AA99)), // 8 Grau
)

fun noteColor(index: Int): NoteColor = NoteColors.getOrElse(index) { NoteColors[0] }
