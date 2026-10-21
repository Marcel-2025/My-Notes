package dev.flondy.mynotes.data

import dev.flondy.mynotes.Note
import dev.flondy.mynotes.settings.SortMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NoteFiltersTest {
    private val day = 86_400_000L
    private val now = 1_760_000_000_000L

    private fun note(id: Int, title: String, content: String = "", order: Int = id, created: Long = now - id * day,
                     updated: Long = created, pinned: Boolean = false, fav: Boolean = false, cat: Int? = null, type: Int = Note.TYPE_TEXT) =
        Note(id = id, title = title, content = content, displayOrder = order, createdAt = created, updatedAt = updated,
            isPinned = pinned, isFavorite = fav, categoryId = cat, noteType = type)

    private val notes = listOf(
        note(1, "Einkaufsliste", "Milch, Brot", order = 2),
        note(2, "arbeit", "Meeting um 10", order = 0, fav = true, cat = 7),
        note(3, "Ärger", "", order = 1, pinned = true),
        note(4, "Zebra", "Zoo besuchen", order = 3, type = Note.TYPE_CHECKLIST),
    )

    private fun ids(list: List<Note>) = list.map { it.id }

    @Test fun search_isCaseInsensitive_inTitleAndContent() {
        assertEquals(listOf(1), ids(NoteFilters.apply(notes, NoteQuery(search = "MILCH"), SortMode.MANUAL, false, now)))
        assertEquals(listOf(2), ids(NoteFilters.apply(notes, NoteQuery(search = "ARBEIT"), SortMode.MANUAL, false, now)))
        assertEquals(listOf(4), ids(NoteFilters.apply(notes, NoteQuery(search = "  zoo "), SortMode.MANUAL, false, now)))
    }

    @Test fun search_findsChecklistItems() {
        val list = listOf(Note(id = 9, title = "Einkauf", content = "", checklist = "[ ] Bananen\n[x] Brot"))
        assertEquals(listOf(9), ids(NoteFilters.apply(list, NoteQuery(search = "banane"), SortMode.MANUAL, false, now)))
    }

    @Test fun search_noResults_andEmptyQueryReturnsAll() {
        assertTrue(NoteFilters.apply(notes, NoteQuery(search = "xyz"), SortMode.MANUAL, false, now).isEmpty())
        assertEquals(4, NoteFilters.apply(notes, NoteQuery(search = "   "), SortMode.MANUAL, false, now).size)
    }

    @Test fun manualSort_pinnedFirst_thenDisplayOrder() {
        assertEquals(listOf(3, 2, 1, 4), ids(NoteFilters.apply(notes, NoteQuery(), SortMode.MANUAL, false, now)))
    }

    @Test fun sortModes() {
        assertEquals(listOf(3, 1, 2, 4), ids(NoteFilters.apply(notes, NoteQuery(), SortMode.NEWEST, false, now)))
        assertEquals(listOf(3, 4, 2, 1), ids(NoteFilters.apply(notes, NoteQuery(), SortMode.OLDEST, false, now)))
        // Locale-bewusst: „arbeit“ (klein) vor „Einkauf“, „Ä“ wie „A“
        assertEquals(listOf(3, 2, 1, 4), ids(NoteFilters.apply(notes, NoteQuery(), SortMode.TITLE_AZ, false, now, Locale.GERMAN)))
        assertEquals(listOf(3, 4, 1, 2), ids(NoteFilters.apply(notes, NoteQuery(), SortMode.TITLE_ZA, false, now, Locale.GERMAN)))
    }

    @Test fun sortCombinedWithSearch() {
        val r = NoteFilters.apply(notes, NoteQuery(search = "e"), SortMode.TITLE_AZ, false, now, Locale.GERMAN)
        assertEquals(listOf(3, 2, 1, 4), ids(r))
    }

    @Test fun favoritesFirst() {
        assertEquals(listOf(3, 2, 4, 1), ids(NoteFilters.apply(notes, NoteQuery(), SortMode.OLDEST, true, now)))
    }

    @Test fun quickFiltersAndCategory() {
        assertEquals(listOf(2), ids(NoteFilters.apply(notes, NoteQuery(quickFilter = QuickFilter.FAVORITES), SortMode.MANUAL, false, now)))
        assertEquals(listOf(3), ids(NoteFilters.apply(notes, NoteQuery(quickFilter = QuickFilter.PINNED), SortMode.MANUAL, false, now)))
        assertEquals(listOf(4), ids(NoteFilters.apply(notes, NoteQuery(quickFilter = QuickFilter.CHECKLISTS), SortMode.MANUAL, false, now)))
        assertEquals(listOf(3, 2, 1), ids(NoteFilters.apply(notes, NoteQuery(quickFilter = QuickFilter.TEXT), SortMode.MANUAL, false, now)))
        assertEquals(listOf(2), ids(NoteFilters.apply(notes, NoteQuery(categoryId = 7), SortMode.MANUAL, false, now)))
        assertEquals(listOf(2, 1, 4), ids(NoteFilters.apply(notes, NoteQuery(onlyWithContent = true), SortMode.MANUAL, false, now)))
    }

    @Test fun dateFilter_usesLastEdited() {
        val list = listOf(note(1, "a", updated = now), note(2, "b", updated = now - 3 * day), note(3, "c", updated = now - 20 * day), note(4, "d", updated = now - 60 * day))
        assertEquals(listOf(1), ids(NoteFilters.apply(list, NoteQuery(dateFilter = DateFilter.TODAY), SortMode.MANUAL, false, now)))
        assertEquals(listOf(1, 2), ids(NoteFilters.apply(list, NoteQuery(dateFilter = DateFilter.LAST_7_DAYS), SortMode.MANUAL, false, now)))
        assertEquals(listOf(1, 2, 3), ids(NoteFilters.apply(list, NoteQuery(dateFilter = DateFilter.LAST_30_DAYS), SortMode.MANUAL, false, now)))
    }

    @Test fun reorderOnlyAllowedForManualWithoutSearch() {
        assertTrue(NoteFilters.isReorderAllowed(NoteQuery(), SortMode.MANUAL, false))
        assertTrue(NoteFilters.isReorderAllowed(NoteQuery(categoryId = 3), SortMode.MANUAL, false))
        assertFalse(NoteFilters.isReorderAllowed(NoteQuery(search = "a"), SortMode.MANUAL, false))
        assertFalse(NoteFilters.isReorderAllowed(NoteQuery(), SortMode.NEWEST, false))
        assertFalse(NoteFilters.isReorderAllowed(NoteQuery(), SortMode.MANUAL, true))
    }

    // ---------- computeReorder (Drag & Drop-Persistenz)
    private val global = (1..5).map { note(it, "n$it", order = it - 1) }

    private fun applyChanges(changes: Map<Int, Int>) =
        global.map { it.copy(displayOrder = changes[it.id] ?: it.displayOrder) }.sortedBy { it.displayOrder }.map { it.id }

    @Test fun reorder_moveDown() = assertEquals(listOf(2, 3, 1, 4, 5), applyChanges(NoteFilters.computeReorder(global, listOf(2, 3, 1, 4, 5))))
    @Test fun reorder_moveUp() = assertEquals(listOf(1, 4, 2, 3, 5), applyChanges(NoteFilters.computeReorder(global, listOf(1, 4, 2, 3, 5))))
    @Test fun reorder_toFirst() = assertEquals(listOf(5, 1, 2, 3, 4), applyChanges(NoteFilters.computeReorder(global, listOf(5, 1, 2, 3, 4))))
    @Test fun reorder_toLast() = assertEquals(listOf(2, 3, 4, 5, 1), applyChanges(NoteFilters.computeReorder(global, listOf(2, 3, 4, 5, 1))))
    @Test fun reorder_unchanged_producesNoWrites() = assertTrue(NoteFilters.computeReorder(global, listOf(1, 2, 3, 4, 5)).isEmpty())

    @Test fun reorder_withinFilteredSubset_keepsOtherNotesInPlace() {
        // Angezeigt (gefiltert): 2, 4, 5 → Nutzer zieht 5 nach oben
        val changes = NoteFilters.computeReorder(global, listOf(5, 2, 4))
        assertEquals(listOf(1, 5, 3, 2, 4), applyChanges(changes))
    }

    @Test fun reorder_withUnknownIds_isIgnored() = assertTrue(NoteFilters.computeReorder(global, listOf(1, 99)).isEmpty())
}
