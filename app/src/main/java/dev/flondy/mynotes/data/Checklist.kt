package dev.flondy.mynotes.data

/**
 * Checklisten werden lesbar im Feld `content` gespeichert – eine Zeile pro Eintrag:
 *
 *     [ ] Milch
 *     [x] Brot
 *
 * Vorteil: Suche, Teilen, TXT-Export und Backup funktionieren ohne Sonderlogik,
 * und Textnotizen bleiben unverändert.
 */
data class ChecklistItem(
    val id: Long,
    val text: String,
    val checked: Boolean,
)

object ChecklistCodec {
    private const val CHECKED = "[x] "
    private const val UNCHECKED = "[ ] "

    private var nextId = 1L
    fun newId(): Long = synchronized(this) { nextId++ }

    fun decode(content: String): List<ChecklistItem> {
        if (content.isBlank()) return emptyList()
        return content.lines()
            .filter { it.isNotBlank() }
            .map { line ->
                when {
                    line.startsWith(CHECKED, ignoreCase = true) ->
                        ChecklistItem(newId(), line.substring(CHECKED.length), true)
                    line.startsWith(UNCHECKED) ->
                        ChecklistItem(newId(), line.substring(UNCHECKED.length), false)
                    else -> ChecklistItem(newId(), line.trim().removePrefix("- ").removePrefix("• "), false)
                }
            }
    }

    fun encode(items: List<ChecklistItem>): String =
        items.filter { it.text.isNotBlank() }
            .joinToString("\n") { (if (it.checked) CHECKED else UNCHECKED) + it.text.replace('\n', ' ').trim() }

    /** Text → Checkliste: jede nicht-leere Zeile wird ein Eintrag. */
    fun fromPlainText(text: String): List<ChecklistItem> = decode(text)

    /** Checkliste → Text: Aufzählung mit Spiegelstrichen. */
    fun toPlainText(items: List<ChecklistItem>): String =
        items.filter { it.text.isNotBlank() }.joinToString("\n") { "- " + it.text }

    /** Lesbare Darstellung zum Teilen (☐ / ☑). */
    fun toShareText(content: String): String =
        decode(content).joinToString("\n") { (if (it.checked) "☑ " else "☐ ") + it.text }

    fun progress(content: String): Pair<Int, Int> {
        val items = decode(content)
        return items.count { it.checked } to items.size
    }
}
