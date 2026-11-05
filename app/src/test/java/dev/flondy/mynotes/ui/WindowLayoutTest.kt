package dev.flondy.mynotes.ui

import androidx.compose.ui.unit.dp
import dev.flondy.mynotes.ui.components.WidthClass
import dev.flondy.mynotes.ui.components.WindowLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Layout-Regeln für Handy und Tablet, hoch und quer. */
class WindowLayoutTest {
    @Test fun phonePortrait_keepsBottomBarAndBigHeader() {
        val w = WindowLayout(411, 891)
        assertEquals(WidthClass.COMPACT, w.widthClass)
        assertFalse(w.useNavigationRail)
        assertFalse(w.compactHeader)
        assertEquals(1, w.listColumns)
        assertEquals(2, w.categoryColumns)
    }

    @Test fun phoneLandscape_usesRailAndCompactHeader() {
        val w = WindowLayout(891, 411)
        assertTrue(w.isShort)
        assertTrue(w.useNavigationRail)
        assertTrue(w.compactHeader)
    }

    @Test fun tabletPortrait_keepsBottomBarButMoreColumns() {
        val w = WindowLayout(800, 1280)
        assertEquals(WidthClass.MEDIUM, w.widthClass)
        assertFalse(w.useNavigationRail)
        assertFalse(w.compactHeader)
        assertEquals(2, w.listColumns)
        assertEquals(3, w.categoryColumns)
    }

    @Test fun tabletLandscape_usesRailWideContentAndReadingWidth() {
        val w = WindowLayout(1280, 800)
        assertEquals(WidthClass.EXPANDED, w.widthClass)
        assertTrue(w.useNavigationRail)
        assertTrue(w.compactHeader)
        assertEquals(3, w.listColumns)
        assertEquals(4, w.categoryColumns)
        assertEquals(880.dp, w.readingMaxWidth)
        assertTrue(w.contentMaxWidth > w.readingMaxWidth)
    }

    @Test fun largeTabletPortrait_usesRail() {
        assertTrue(WindowLayout(900, 1350).useNavigationRail)
    }
}
