package dev.flondy.mynotes.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {
    @Test fun render_removesMarkersAndKeepsStyles() {
        val r = RichText.render("Ein **fetter** und *kursiver* und __unterstrichener__ Text")
        assertEquals("Ein fetter und kursiver und unterstrichener Text", r.text)
        val bold = r.spans.single { it.style == RichText.Style.BOLD }
        assertEquals("fetter", r.text.substring(bold.start, bold.end))
        val italic = r.spans.single { it.style == RichText.Style.ITALIC }
        assertEquals("kursiver", r.text.substring(italic.start, italic.end))
        val under = r.spans.single { it.style == RichText.Style.UNDERLINE }
        assertEquals("unterstrichener", r.text.substring(under.start, under.end))
    }

    @Test fun links() {
        val r = RichText.render("Siehe [Website](https://example.com) bitte")
        assertEquals("Siehe Website bitte", r.text)
        assertEquals("https://example.com", r.spans.single().url)
        assertEquals("Siehe Website (https://example.com) bitte", RichText.toPlainText("Siehe [Website](https://example.com) bitte"))
    }

    @Test fun plainTextWithoutMarkersIsUnchanged() {
        val t = "Preis: 3 * 4 = 12, Datei_name_v2.txt, E-Mail a@b.de"
        assertEquals(t, RichText.render(t).text)
        assertTrue(RichText.parse(t).isEmpty())
    }

    @Test fun toggleWrap_addsAndRemoves() {
        val e = RichText.toggleWrap("Hallo Welt", 6, 10, "**")
        assertEquals("Hallo **Welt**", e.text)
        assertEquals(8, e.selectionStart); assertEquals(12, e.selectionEnd)
        val back = RichText.toggleWrap(e.text, e.selectionStart, e.selectionEnd, "**")
        assertEquals("Hallo Welt", back.text)
        val empty = RichText.toggleWrap("ab", 1, 1, "*")
        assertEquals("a**b", empty.text); assertEquals(2, empty.selectionStart)
    }

    @Test fun toggleList_bulletsAndNumbers() {
        val t = "Eins\nZwei\nDrei"
        val b = RichText.toggleList(t, 0, t.length, numbered = false)
        assertEquals("• Eins\n• Zwei\n• Drei", b.text)
        assertEquals(t, RichText.toggleList(b.text, 0, b.text.length, numbered = false).text)
        val n = RichText.toggleList(t, 0, t.length, numbered = true)
        assertEquals("1. Eins\n2. Zwei\n3. Drei", n.text)
        // Nur die aktuelle Zeile
        assertEquals("Eins\n• Zwei\nDrei", RichText.toggleList(t, 6, 6, numbered = false).text)
    }

    @Test fun continueList_onEnter() {
        val e = RichText.continueList("• Milch", "• Milch\n")!!
        assertEquals("• Milch\n• ", e.text)
        val n = RichText.continueList("1. A", "1. A\n")!!
        assertEquals("1. A\n2. ", n.text)
        // Leerer Punkt + Enter beendet die Liste
        assertEquals("• Milch\n", RichText.continueList("• Milch\n• ", "• Milch\n• \n")!!.text)
        assertNull(RichText.continueList("Text", "Text\n"))
    }

    @Test fun insertLink_normalizesUrl() {
        val e = RichText.insertLink("Mehr hier", 5, 9, "example.com")
        assertEquals("Mehr [hier](https://example.com)", e.text)
        assertEquals("mailto:a@b.de", RichText.normalizeUrl("a@b.de"))
    }
}
