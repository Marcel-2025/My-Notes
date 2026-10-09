package dev.flondy.mynotes.data

import dev.flondy.mynotes.Note
import dev.flondy.mynotes.settings.SortMode
import java.text.Collator
import java.util.Calendar
import java.util.Locale

enum class QuickFilter { ALL, FAVORITES, PINNED, CHECKLISTS }

enum class DateFilter { ANY, TODAY, LAST_7_DAYS, LAST_30_DAYS }

data class NoteQuery(
    val search: String = "",
    val quickFilter: QuickFilter = QuickFilter.ALL,
    val categoryId: Int? = null,
    val dateFilter: DateFilter = DateFilter.ANY,
    val onlyWithContent: Boolean = false,
) {
    val isFiltered: Boolean
        get() = search.isNotBlank() || quickFilter != QuickFilter.ALL || categoryId != null ||
            dateFilter != DateFilter.ANY || onlyWithContent
}

/** Reine Funktionen für Suche, Filter und Sortierung – unabhängig von UI und Datenbank testbar. */
object NoteFilters {

    fun apply(
        notes: List<Note>,
        query: NoteQuery,
        sortMode: SortMode,
        favoritesFirst: Boolean,
        now: Long = System.currentTimeMillis(),
        locale: Locale = Locale.getDefault(),
    ): List<Note> {
        val needle = query.search.trim().lowercase(locale)
        val minTime = dateCutoff(query.dateFilter, now)
        val filtered = notes.filter { n ->
            (needle.isEmpty() || n.title.lowercase(locale).contains(needle) || n.content.lowercase(locale).contains(needle)) &&
                when (query.quickFilter) {
                    QuickFilter.ALL -> true
                    QuickFilter.FAVORITES -> n.isFavorite
                    QuickFilter.PINNED -> n.isPinned
                    QuickFilter.CHECKLISTS -> n.isChecklist
                } &&
                (query.categoryId == null || n.categoryId == query.categoryId) &&
                (minTime == null || n.updatedAt >= minTime) &&
                (!query.onlyWithContent || n.hasContent)
        }
        return sort(filtered, sortMode, favoritesFirst, locale)
    }

    fun sort(notes: List<Note>, sortMode: SortMode, favoritesFirst: Boolean, locale: Locale = Locale.getDefault()): List<Note> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        val byMode: Comparator<Note> = when (sortMode) {
            SortMode.MANUAL -> compareBy<Note> { it.displayOrder }.thenByDescending { it.updatedAt }
            SortMode.UPDATED -> compareByDescending { it.updatedAt }
            SortMode.NEWEST -> compareByDescending<Note> { it.createdAt }.thenByDescending { it.id }
            SortMode.OLDEST -> compareBy<Note> { it.createdAt }.thenBy { it.id }
            SortMode.TITLE_AZ -> Comparator { a, b -> collator.compare(a.title, b.title) }
            SortMode.TITLE_ZA -> Comparator { a, b -> collator.compare(b.title, a.title) }
        }
        var comparator: Comparator<Note> = compareByDescending { it.isPinned }
        if (favoritesFirst) comparator = comparator.thenByDescending { it.isFavorite }
        return notes.sortedWith(comparator.then(byMode))
    }

    /** Manuelles Verschieben ist nur sinnvoll, wenn die Anzeige der gespeicherten Reihenfolge entspricht. */
    fun isReorderAllowed(query: NoteQuery, sortMode: SortMode, favoritesFirst: Boolean): Boolean =
        sortMode == SortMode.MANUAL && query.search.isBlank() && !favoritesFirst

    fun dateCutoff(filter: DateFilter, now: Long): Long? {
        if (filter == DateFilter.ANY) return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        when (filter) {
            DateFilter.TODAY -> Unit
            DateFilter.LAST_7_DAYS -> cal.add(Calendar.DAY_OF_YEAR, -6)
            DateFilter.LAST_30_DAYS -> cal.add(Calendar.DAY_OF_YEAR, -29)
            DateFilter.ANY -> Unit
        }
        return cal.timeInMillis
    }

    /**
     * Berechnet die neue globale Reihenfolge, wenn innerhalb einer (ggf. gefilterten) Teilmenge
     * umsortiert wurde. Die Teilmenge belegt dieselben „Plätze“ in der Gesamtliste wie zuvor –
     * nicht angezeigte Notizen behalten ihre Position.
     *
     * @param globalOrder alle aktiven Notizen in aktueller manueller Reihenfolge
     * @param newSubsetOrder IDs der angezeigten Notizen in der neuen Reihenfolge
     * @return Map id → neue displayOrder (nur geänderte Einträge)
     */
    fun computeReorder(globalOrder: List<Note>, newSubsetOrder: List<Int>): Map<Int, Int> {
        val subset = newSubsetOrder.toSet()
        val byId = globalOrder.associateBy { it.id }
        if (newSubsetOrder.any { it !in byId }) return emptyMap()
        val slots = globalOrder.indices.filter { globalOrder[it].id in subset }
        if (slots.size != newSubsetOrder.size) return emptyMap()
        val result = globalOrder.toMutableList()
        slots.forEachIndexed { i, slot -> result[slot] = byId.getValue(newSubsetOrder[i]) }
        val changes = mutableMapOf<Int, Int>()
        result.forEachIndexed { index, note -> if (note.displayOrder != index) changes[note.id] = index }
        return changes
    }
}
