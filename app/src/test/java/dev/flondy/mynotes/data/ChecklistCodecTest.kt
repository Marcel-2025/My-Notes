package dev.flondy.mynotes.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ChecklistCodecTest {
    @Test fun roundTrip() {
        val content = "[ ] Milch\n[x] Brot\n[ ] Eier"
        val items = ChecklistCodec.decode(content)
        assertEquals(listOf("Milch", "Brot", "Eier"), items.map { it.text })
        assertEquals(listOf(false, true, false), items.map { it.checked })
        assertEquals(content, ChecklistCodec.encode(items))
    }

    @Test fun plainTextLinesBecomeItems_andBlankItemsAreDropped() {
        val items = ChecklistCodec.fromPlainText("- Milch\n\n• Brot\nEier")
        assertEquals(listOf("Milch", "Brot", "Eier"), items.map { it.text })
        assertEquals("[ ] A", ChecklistCodec.encode(listOf(ChecklistItem(1, "A", false), ChecklistItem(2, "  ", true))))
    }

    @Test fun progressAndShareText() {
        assertEquals(1 to 3, ChecklistCodec.progress("[ ] a\n[x] b\n[ ] c"))
        assertEquals("☐ a\n☑ b", ChecklistCodec.toShareText("[ ] a\n[x] b"))
    }

    @Test fun newlinesInsideItemAreFlattened() {
        assertEquals("[ ] a b", ChecklistCodec.encode(listOf(ChecklistItem(1, "a\nb", false))))
    }
}
