package dev.flondy.mynotes.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Breitenklassen nach den Material-Richtlinien (Kompakt < 600 dp, Mittel < 840 dp, Erweitert ≥ 840 dp). */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Beschreibt die aktuelle Fenstergröße und leitet daraus die Layout-Entscheidungen ab.
 * Bewusst ohne geteilte Ansichten (kein Liste/Detail nebeneinander): jede Seite bleibt eine Seite,
 * nutzt auf großen Bildschirmen aber mehr Spalten, eine seitliche Navigation und lesefreundliche Breiten.
 */
data class WindowLayout(val widthDp: Int, val heightDp: Int) {
    val widthClass: WidthClass = when {
        widthDp < 600 -> WidthClass.COMPACT
        widthDp < 840 -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }
    val isLandscape: Boolean get() = widthDp > heightDp

    /** Niedrige Fenster (Handy quer): Kopfzeilen und Abstände werden kompakter. */
    val isShort: Boolean get() = heightDp < 480

    /** Seitliche Navigationsleiste statt Leiste unten: Tablets quer, große Tablets und Handys quer. */
    val useNavigationRail: Boolean get() = widthDp >= 600 && (isLandscape || widthDp >= 840)

    /** Einzeilige Kopfzeile auf der Startseite, damit im Querformat mehr Notizen sichtbar sind. */
    val compactHeader: Boolean get() = isShort || (isLandscape && widthDp >= 600)

    val isTabletOrLarger: Boolean get() = widthClass != WidthClass.COMPACT

    /** Maximale Breite der Notizübersicht (Raster/Liste). */
    val contentMaxWidth: Dp get() = when (widthClass) {
        WidthClass.COMPACT -> 600.dp
        WidthClass.MEDIUM -> 960.dp
        WidthClass.EXPANDED -> 1360.dp
    }

    /** Lesebreite für Editor, Einstellungen & Co. – lange Zeilen sind schwer zu lesen. */
    val readingMaxWidth: Dp get() = if (widthClass == WidthClass.EXPANDED) 880.dp else 760.dp

    /** Minimale Kartenbreite im Raster. */
    val gridMinCell: Dp get() = when (widthClass) {
        WidthClass.COMPACT -> 156.dp
        WidthClass.MEDIUM -> 190.dp
        WidthClass.EXPANDED -> 216.dp
    }

    /** Spalten für Listenkarten (Liste/Übersicht) und Kategorie-Kacheln. */
    val listColumns: Int get() = when (widthClass) {
        WidthClass.COMPACT -> 1
        WidthClass.MEDIUM -> 2
        WidthClass.EXPANDED -> if (widthDp >= 1200) 3 else 2
    }
    val categoryColumns: Int get() = when (widthClass) {
        WidthClass.COMPACT -> 2
        WidthClass.MEDIUM -> 3
        WidthClass.EXPANDED -> 4
    }
    val favoriteCardWidth: Dp get() = if (widthClass == WidthClass.COMPACT) 176.dp else 210.dp
}

@Composable
fun rememberWindowLayout(): WindowLayout {
    val c = LocalConfiguration.current
    return remember(c.screenWidthDp, c.screenHeightDp) { WindowLayout(c.screenWidthDp, c.screenHeightDp) }
}
