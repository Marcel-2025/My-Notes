package dev.flondy.mynotes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.flondy.mynotes.Note
import dev.flondy.mynotes.ui.components.ReorderableNoteList
import dev.flondy.mynotes.ui.theme.MyNotesTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Gesten-Tests für Drag & Drop (laufen ohne Gerät über Robolectric).
 * Kartenhöhe 80 dp, Abstand 10 dp, oben 0 dp Innenabstand → Kartenmitte i = i*90 + 40 dp.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = android.app.Application::class, qualifiers = "w400dp-h800dp-mdpi")
class ReorderableNoteListTest {
    @get:Rule val rule = createComposeRule()

    private var reordered: List<Int>? = null
    private var clicked: Int? = null
    private var menu: Int? = null

    private fun setUp(count: Int = 6, enabled: Boolean = true, pinnedIds: Set<Int> = emptySet()) {
        val initial = (1..count).map { Note(id = it, title = "Notiz $it", content = "", displayOrder = it, isPinned = it in pinnedIds) }
        rule.setContent {
            MyNotesTheme {
                var notes by remember { mutableStateOf(initial) }
                ReorderableNoteList(
                    notes = notes,
                    reorderEnabled = enabled,
                    listState = rememberLazyListState(),
                    contentPadding = PaddingValues(0.dp),
                    onReorder = { ids -> reordered = ids; notes = ids.map { id -> notes.first { it.id == id } } },
                    onLongPressWithoutMove = { menu = it.id },
                    modifier = Modifier.height(600.dp),
                ) { note, _, isOverlay ->
                    Box(
                        Modifier.fillMaxWidth().height(80.dp)
                            .testTag(if (isOverlay) "overlay" else "note_${note.id}")
                            .clickable { clicked = note.id }
                    ) { Text(note.title) }
                }
            }
        }
    }

    private fun drag(fromIndex: Int, toY: Float) {
        rule.onNodeWithTag("note_list").performTouchInput {
            val startY = (fromIndex * 90 + 40).dp.toPx()
            down(Offset(centerX, startY))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            val target = toY.dp.toPx()
            val steps = 30
            for (i in 1..steps) moveTo(Offset(centerX, startY + (target - startY) * i / steps))
            up()
        }
        rule.waitForIdle()
    }

    @Test fun tapOpensNote() {
        setUp()
        rule.onNodeWithTag("note_2").performClick()
        assertEquals(2, clicked)
        assertNull(reordered)
    }

    @Test fun dragDown_movesNoteBelowTarget() {
        setUp()
        drag(fromIndex = 0, toY = 2 * 90 + 60f) // über die Mitte von Karte 3 hinaus
        assertEquals(listOf(2, 3, 1, 4, 5, 6), reordered)
        assertNull("Loslassen nach Drag darf keinen Klick auslösen", clicked)
    }

    @Test fun dragUp_toFirstPosition() {
        setUp()
        drag(fromIndex = 3, toY = 10f)
        assertEquals(listOf(4, 1, 2, 3, 5, 6), reordered)
    }

    @Test fun dragToLastPosition() {
        setUp()
        drag(fromIndex = 1, toY = 5 * 90 + 70f)
        assertEquals(listOf(1, 3, 4, 5, 6, 2), reordered)
    }

    @Test fun longPressWithoutMove_opensMenu_notEditor() {
        setUp()
        rule.onNodeWithTag("note_list").performTouchInput {
            down(Offset(centerX, (90 + 40).dp.toPx()))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            up()
        }
        rule.waitForIdle()
        assertEquals(2, menu)
        assertNull(reordered)
        assertNull(clicked)
    }

    @Test fun autoScroll_whenDraggingToBottomEdge() {
        setUp(count = 20)
        rule.onNodeWithTag("note_list").performTouchInput {
            val startY = 40.dp.toPx()
            down(Offset(centerX, startY))
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
            val bottom = 590.dp.toPx()
            for (i in 1..20) moveTo(Offset(centerX, startY + (bottom - startY) * i / 20))
            // Finger am unteren Rand halten → Liste scrollt automatisch
            repeat(120) { advanceEventTime(16); moveBy(Offset(0f, 0.01f)) }
            up()
        }
        rule.waitForIdle()
        val order = reordered!!
        val newIndex = order.indexOf(1)
        assert(newIndex > 7) { "Notiz 1 sollte durch Auto-Scroll weit nach unten gewandert sein, Index war $newIndex" }
    }

    @Test fun pinnedAndUnpinnedStayInTheirGroups() {
        setUp(pinnedIds = setOf(1, 2))
        drag(fromIndex = 3, toY = 10f) // unangeheftete Notiz 4 nach ganz oben ziehen
        assertEquals(listOf(1, 2, 4, 3, 5, 6), reordered)
    }

    @Test fun dragDisabled_longPressDoesNothing() {
        setUp(enabled = false)
        drag(fromIndex = 0, toY = 300f)
        assertNull(reordered)
    }
}
