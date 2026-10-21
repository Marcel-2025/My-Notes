package dev.flondy.mynotes.data

/**
 * Leichtgewichtige Formatierung im Notiztext (Markdown-ähnlich, bleibt als reiner Text speicherbar):
 *
 *   **fett**   *kursiv*   __unterstrichen__   [Linktext](https://…)
 *   Zeilen mit „• “ = Aufzählung, „1. “ = Nummerierung
 *
 * Vorteil: Datenbank, Suche, Backup und ältere Notizen bleiben unverändert kompatibel.
 */
object RichText {
    enum class Style { BOLD, ITALIC, UNDERLINE, LINK }

    /** Formatierter Bereich im Originaltext: [contentStart, contentEnd) ist der sichtbare Inhalt. */
    data class Span(
        val style: Style,
        val start: Int,
        val end: Int,
        val contentStart: Int,
        val contentEnd: Int,
        val url: String? = null,
    )

    /** Text ohne Steuerzeichen plus Formatierung in dessen Koordinaten. */
    data class Rendered(val text: String, val spans: List<Span>)

    private val LINK = Regex("""\[([^\]\n]+)]\((https?://[^)\s]+|www\.[^)\s]+|mailto:[^)\s]+)\)""")
    private val BOLD = Regex("""\*\*([^*\n]+?)\*\*""")
    private val UNDERLINE = Regex("""__([^_\n]+?)__""")
    private val ITALIC = Regex("""(?<![*\w])\*([^*\n]+?)\*(?![*\w])""")

    const val BULLET = "• "

    fun parse(text: String): List<Span> {
        val spans = mutableListOf<Span>()
        val taken = BooleanArray(text.length)
        fun free(r: IntRange) = r.none { taken[it] }
        fun take(r: IntRange) = r.forEach { taken[it] = true }

        LINK.findAll(text).forEach { m ->
            val label = m.groups[1]!!.range
            spans += Span(Style.LINK, m.range.first, m.range.last + 1, label.first, label.last + 1, m.groupValues[2])
            take(m.range)
        }
        for ((regex, style, len) in listOf(Triple(BOLD, Style.BOLD, 2), Triple(UNDERLINE, Style.UNDERLINE, 2), Triple(ITALIC, Style.ITALIC, 1))) {
            regex.findAll(text).forEach { m ->
                // Marker dürfen nicht in einem Link oder anderen Marker liegen
                val markers = (m.range.first until m.range.first + len) + (m.range.last - len + 1..m.range.last)
                if (markers.all { !taken[it] } && free(m.range.first until m.range.first + len)) {
                    spans += Span(style, m.range.first, m.range.last + 1, m.range.first + len, m.range.last + 1 - len)
                    markers.forEach { taken[it] = true }
                }
            }
        }
        return spans.sortedBy { it.start }
    }

    /** Entfernt alle Steuerzeichen (für Kartenvorschau, Teilen, Export). */
    fun render(text: String): Rendered {
        val spans = parse(text)
        if (spans.isEmpty()) return Rendered(text, emptyList())
        // Zu entfernende Bereiche (Marker bzw. bei Links „[“ und „](url)“)
        val remove = BooleanArray(text.length)
        spans.forEach { s ->
            (s.start until s.contentStart).forEach { remove[it] = true }
            (s.contentEnd until s.end).forEach { remove[it] = true }
        }
        val map = IntArray(text.length + 1)
        val sb = StringBuilder()
        for (i in text.indices) {
            map[i] = sb.length
            if (!remove[i]) sb.append(text[i])
        }
        map[text.length] = sb.length
        val out = spans.map { s -> s.copy(start = map[s.contentStart], end = map[s.contentEnd], contentStart = map[s.contentStart], contentEnd = map[s.contentEnd]) }
        return Rendered(sb.toString(), out)
    }

    /** Lesbarer Klartext; Links werden als „Text (URL)“ ausgegeben. */
    fun toPlainText(text: String): String =
        LINK.replace(text) { m -> "${m.groupValues[1]} (${m.groupValues[2]})" }
            .let { render(it).text }

    data class Edit(val text: String, val selectionStart: Int, val selectionEnd: Int)

    /** Fett/Kursiv/Unterstrichen um die Auswahl setzen oder entfernen. */
    fun toggleWrap(text: String, selStart: Int, selEnd: Int, marker: String): Edit {
        val start = minOf(selStart, selEnd).coerceIn(0, text.length)
        val end = maxOf(selStart, selEnd).coerceIn(0, text.length)
        val m = marker.length
        // Bereits umschlossen → Marker entfernen
        if (start >= m && end + m <= text.length &&
            text.substring(start - m, start) == marker && text.substring(end, end + m) == marker
        ) {
            val t = text.substring(0, start - m) + text.substring(start, end) + text.substring(end + m)
            return Edit(t, start - m, end - m)
        }
        val t = text.substring(0, start) + marker + text.substring(start, end) + marker + text.substring(end)
        return Edit(t, start + m, end + m)
    }

    /** Aufzählung bzw. Nummerierung für alle Zeilen der Auswahl umschalten. */
    fun toggleList(text: String, selStart: Int, selEnd: Int, numbered: Boolean): Edit {
        val start = minOf(selStart, selEnd).coerceIn(0, text.length)
        val end = maxOf(selStart, selEnd).coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', start - 1) + 1
        val lineEndIdx = text.indexOf('\n', end).let { if (it < 0) text.length else it }
        val block = text.substring(lineStart, lineEndIdx)
        val lines = block.split('\n')
        val numberRegex = Regex("""^\d+\.\s""")
        val allHave = lines.all { if (numbered) numberRegex.containsMatchIn(it) else it.startsWith(BULLET) || it.startsWith("- ") }
        val newLines = lines.mapIndexed { i, line ->
            val stripped = line.replaceFirst(numberRegex, "").removePrefix(BULLET).removePrefix("- ")
            when {
                allHave -> stripped
                numbered -> "${i + 1}. $stripped"
                else -> BULLET + stripped
            }
        }
        val newBlock = newLines.joinToString("\n")
        val t = text.substring(0, lineStart) + newBlock + text.substring(lineEndIdx)
        val cursor = lineStart + newBlock.length
        return Edit(t, cursor, cursor)
    }

    /** Link einfügen: Auswahl wird zum Linktext (sonst die URL selbst). */
    fun insertLink(text: String, selStart: Int, selEnd: Int, rawUrl: String): Edit {
        val start = minOf(selStart, selEnd).coerceIn(0, text.length)
        val end = maxOf(selStart, selEnd).coerceIn(0, text.length)
        val url = normalizeUrl(rawUrl)
        val label = text.substring(start, end).ifBlank { rawUrl.trim() }.replace("]", ")")
        val link = "[$label]($url)"
        val t = text.substring(0, start) + link + text.substring(end)
        return Edit(t, start + link.length, start + link.length)
    }

    fun normalizeUrl(raw: String): String {
        val u = raw.trim().replace(" ", "")
        return when {
            u.startsWith("http://") || u.startsWith("https://") || u.startsWith("mailto:") -> u
            u.contains("@") && !u.contains("/") -> "mailto:$u"
            else -> "https://$u"
        }
    }

    /** Fortsetzung von Aufzählungen beim Zeilenumbruch: „• “ bzw. „n+1. “ automatisch ergänzen. */
    fun continueList(old: String, new: String): Edit? {
        if (new.length != old.length + 1) return null
        val i = (0 until new.length).firstOrNull { it >= old.length || new[it] != old[it] } ?: return null
        if (new[i] != '\n') return null
        val lineStart = new.lastIndexOf('\n', i - 1) + 1
        val line = new.substring(lineStart, i)
        val numbered = Regex("""^(\d+)\.\s""").find(line)
        val prefix = when {
            line.startsWith(BULLET) -> BULLET
            numbered != null -> "${numbered.groupValues[1].toInt() + 1}. "
            else -> return null
        }
        // Leerer Listenpunkt + Enter → Liste beenden
        val lineContent = if (numbered != null) line.substring(numbered.value.length) else line.removePrefix(BULLET)
        if (lineContent.isBlank()) {
            val t = new.substring(0, lineStart) + new.substring(i + 1)
            return Edit(t, lineStart, lineStart)
        }
        val t = new.substring(0, i + 1) + prefix + new.substring(i + 1)
        return Edit(t, i + 1 + prefix.length, i + 1 + prefix.length)
    }
}
