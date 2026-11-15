package dev.flondy.mynotes.ui

import dev.flondy.mynotes.Category
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.data.NoteTemplates
import dev.flondy.mynotes.ui.stats.NoteStats
import dev.flondy.mynotes.ui.theme.Entitlements
import dev.flondy.mynotes.ui.theme.PaperPattern
import dev.flondy.mynotes.ui.theme.ShopPreview
import dev.flondy.mynotes.ui.theme.Skin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 2.4: Freischaltung, Vorschau, Vorlagen und Statistik. */
class ShopLogicTest {
    @After fun tearDown() = ShopPreview.stop()

    @Test fun freeContentAlwaysUnlocked_premiumLocked() {
        assertTrue(Entitlements.skinUnlocked(Skin.LEATHER, emptySet()))
        assertTrue(Entitlements.skinUnlocked(Skin.GRAPHITE, emptySet()))
        assertFalse(Entitlements.skinUnlocked(Skin.MIDNIGHT, emptySet()))
        assertTrue(Entitlements.skinUnlocked(Skin.MIDNIGHT, setOf("skin:MIDNIGHT")))
        assertTrue(Entitlements.patternUnlocked(PaperPattern.GRID, setOf(Entitlements.ALL)))
        assertFalse(Entitlements.patternUnlocked(PaperPattern.DOTS, emptySet()))
    }

    @Test fun savedPremiumSkinFallsBackWhenLocked() {
        assertEquals(Skin.LEATHER, Entitlements.effectiveSkin("OCEAN", emptySet()))
        assertEquals(Skin.OCEAN, Entitlements.effectiveSkin("OCEAN", setOf(Entitlements.ALL)))
        assertEquals(Skin.LEATHER, Entitlements.effectiveSkin("UNKNOWN", emptySet()))
        assertEquals(PaperPattern.PLAIN, Entitlements.effectivePattern("GRID", emptySet()))
    }

    @Test fun previewIsTemporary() {
        ShopPreview.start(skin = Skin.FOREST, now = 1_000L)
        assertEquals(Skin.FOREST, Entitlements.effectiveSkin("LEATHER", emptySet()))
        assertEquals(30, ShopPreview.secondsLeft(1_000L))
        assertEquals(0, ShopPreview.secondsLeft(31_000L))
        ShopPreview.stop()
        assertEquals(Skin.LEATHER, Entitlements.effectiveSkin("LEATHER", emptySet()))
    }

    @Test fun templatesInAllLanguages() {
        for (l in listOf("de", "en", "es", "fr", "it", "pl", "pt", "ru", "tr")) {
            val t = NoteTemplates.forLanguage(l)
            assertEquals(6, t.size)
            assertTrue(t.all { it.title.isNotBlank() && (it.text.isNotBlank() || it.items.isNotEmpty()) })
        }
        assertEquals(NoteTemplates.forLanguage("en"), NoteTemplates.forLanguage("xx"))
    }

    @Test fun statsCountRealNotes() {
        val now = 10L * 24 * 60 * 60 * 1000
        val notes = listOf(
            Note(id = 1, title = "Einkauf", content = "", checklist = "[x] Brot\n[ ] Milch", noteType = Note.TYPE_CHECKLIST, categoryId = 1, updatedAt = now),
            Note(id = 2, title = "Idee", content = "zwei Wörter", updatedAt = now - 3L * 24 * 60 * 60 * 1000),
            Note(id = 3, title = "weg", content = "", deletedAt = now),
        )
        val s = NoteStats.of(notes, listOf(Category(id = 1, name = "Einkaufen")), now)
        assertEquals(2, s.notes); assertEquals(1, s.lists); assertEquals(1, s.done); assertEquals(1, s.open)
        assertEquals(0.5f, s.progress)
        assertEquals(14, s.activity.size); assertEquals(2, s.activity.sum())
        assertEquals(2, s.byCategory.size)
    }
}
