package dev.flondy.mynotes.ui.theme

import androidx.compose.ui.graphics.Color

// --- Markenfarben ---
val BrandYellow = Color(0xFFFDEE51)
val Ink = Color(0xFF0C0F1C)
val InkSurface = Color(0xFF111827)
val InkSurfaceHigh = Color(0xFF1A2233)
val InkSurfaceHighest = Color(0xFF232C3F)
val InkOutline = Color(0xFF2B3548)
val PremiumWhite = Color(0xFFF8FAFC)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate200 = Color(0xFFE2E8F0)
val Slate100 = Color(0xFFF1F5F9)
val LightBackground = Color(0xFFF6F7FB)
val OnAccent = Color(0xFF1A1806)
val ErrorLight = Color(0xFFDC2626)
val ErrorDark = Color(0xFFF87171)

/**
 * Notizfarben (Index wird in der DB gespeichert – Reihenfolge nicht ändern, nur ergänzen).
 * Jeweils dezente Kartenfarbe für hell und dunkel plus kräftiger Punkt für die Auswahl.
 */
data class NoteColor(val light: Color, val dark: Color, val swatch: Color)

val NoteColors = listOf(
    NoteColor(Color.Unspecified, Color.Unspecified, Color(0xFF9CA3AF)), // 0 Standard
    NoteColor(Color(0xFFFFF9C4), Color(0xFF2E2A12), Color(0xFFFDEE51)), // 1 Gelb
    NoteColor(Color(0xFFFFE8CC), Color(0xFF33240F), Color(0xFFFDBA74)), // 2 Orange
    NoteColor(Color(0xFFFFE1E1), Color(0xFF341A1D), Color(0xFFFCA5A5)), // 3 Koralle
    NoteColor(Color(0xFFFCE7F3), Color(0xFF31182A), Color(0xFFF9A8D4)), // 4 Rosa
    NoteColor(Color(0xFFEDE9FE), Color(0xFF231D3A), Color(0xFFC4B5FD)), // 5 Lavendel
    NoteColor(Color(0xFFDBEAFE), Color(0xFF15233A), Color(0xFF93C5FD)), // 6 Blau
    NoteColor(Color(0xFFD1FAE5), Color(0xFF12291F), Color(0xFF6EE7B7)), // 7 Mint
    NoteColor(Color(0xFFE5E7EB), Color(0xFF242A35), Color(0xFFCBD5E1)), // 8 Grau
)

fun noteColor(index: Int): NoteColor = NoteColors.getOrElse(index) { NoteColors[0] }
