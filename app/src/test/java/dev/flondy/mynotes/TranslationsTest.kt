package dev.flondy.mynotes

import dev.flondy.mynotes.settings.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Prüft alle Übersetzungen gegen die englische Standarddatei:
 * gleiche Texte, gleiche Platzhalter (%1$s …), nötige Pluralformen und passende Sprachliste.
 */
class TranslationsTest {
    private val res = File(System.getProperty("user.dir"), "src/main/res").let { if (it.exists()) it else File("app/src/main/res") }
    private val placeholder = Regex("%(\\d+\\$)?[sd]")

    private data class Entry(val texts: List<String>, val quantities: Set<String>)

    private fun load(dir: String): Map<String, Entry> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$dir/strings.xml"))
        val out = LinkedHashMap<String, Entry>()
        val root = doc.documentElement
        val strings = root.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            if (e.getAttribute("translatable") == "false") continue
            out[e.getAttribute("name")] = Entry(listOf(e.textContent), emptySet())
        }
        val plurals = root.getElementsByTagName("plurals")
        for (i in 0 until plurals.length) {
            val e = plurals.item(i) as Element
            val items = e.getElementsByTagName("item")
            val texts = (0 until items.length).map { (items.item(it) as Element).textContent }
            val q = (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }.toSet()
            out["plurals:" + e.getAttribute("name")] = Entry(texts, q)
        }
        return out
    }

    private fun folder(tag: String) = if (tag == "en") "values" else "values-$tag"

    @Test fun everyLanguageHasEveryTextWithSamePlaceholders() {
        val base = load("values")
        assertTrue(base.size > 200)
        for (tag in AppLanguage.supported.filter { it != "en" }) {
            val tr = load(folder(tag))
            assertEquals("Fehlende/zusätzliche Texte in $tag", base.keys, tr.keys)
            for ((name, entry) in base) {
                val expected = entry.texts.flatMap { t -> placeholder.findAll(t).map { it.value } }.toSet()
                tr.getValue(name).texts.forEach { t ->
                    val got = placeholder.findAll(t).map { it.value }.toSet()
                    assertEquals("Platzhalter in $tag/$name", expected, got)
                }
            }
        }
    }

    @Test fun pluralsHaveRequiredForms() {
        val required = mapOf("ru" to setOf("one", "few", "many", "other"), "pl" to setOf("one", "few", "many", "other"))
        for (tag in AppLanguage.supported) {
            load(folder(tag)).filterKeys { it.startsWith("plurals:") }.forEach { (name, e) ->
                assertTrue("$tag/$name braucht one+other", e.quantities.containsAll(setOf("one", "other")))
                required[tag]?.let { assertTrue("$tag/$name braucht $it", e.quantities.containsAll(it)) }
            }
        }
    }

    @Test fun localeConfigMatchesSupportedLanguages() {
        val xml = File(res, "xml/locales_config.xml").readText()
        val tags = Regex("android:name=\"([a-z-]+)\"").findAll(xml).map { it.groupValues[1] }.toList()
        assertEquals(AppLanguage.supported.sorted(), tags.sorted())
        AppLanguage.supported.forEach { assertTrue("Ordner fehlt für $it", File(res, folder(it) + "/strings.xml").exists()) }
    }
}
