package dev.flondy.mynotes.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.flondy.mynotes.MyNotesApp
import dev.flondy.mynotes.NotesActivity
import dev.flondy.mynotes.app
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * End-to-End-Ablauf auf der echten Activity (Robolectric, deutsche Oberfläche):
 * Start → Beispielnotizen → neue Notiz → Bearbeiten → Suche → Papierkorb → Wiederherstellen.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = MyNotesApp::class, qualifiers = "de-w411dp-h891dp")
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<NotesActivity>()

    private fun waitForText(text: String, timeout: Long = 5_000) =
        rule.waitUntil(timeout) { rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun repo() = rule.activity.application.app.repository

    @Test fun fullNoteLifecycle() {
        // 1) Erststart: Beispielnotizen und Kategorien sind da
        waitForText("Willkommen")
        rule.onNodeWithText("Einkaufsliste").assertIsDisplayed()
        rule.onNodeWithText("Meine Notizen").assertIsDisplayed()

        // 2) Neue Notiz anlegen
        rule.onNodeWithTag("fab_new_note").performClick()
        rule.onNodeWithTag("editor_title").performTextInput("Projektplan")
        rule.onNodeWithTag("editor_content").performTextInput("Meilenstein 1 fertigstellen")
        rule.onNodeWithTag("editor_back").performClick()
        waitForText("Projektplan")
        val created = runBlocking { repo().activeNotes.first().first { it.title == "Projektplan" } }
        assertEquals("Meilenstein 1 fertigstellen", created.content)

        // 3) Antippen öffnet den Editor mit bestehendem Inhalt, Bearbeiten speichert unter derselben ID
        rule.onNodeWithText("Projektplan").performClick()
        rule.onNodeWithTag("editor_content").performTextReplacement("Meilenstein 2")
        rule.onNodeWithTag("editor_back").performClick()
        rule.waitUntil(5_000) { runBlocking { repo().getNote(created.id)?.content == "Meilenstein 2" } }
        val edited = runBlocking { repo().getNote(created.id)!! }
        assertEquals("Erstellungsdatum bleibt erhalten", created.createdAt, edited.createdAt)
        assertEquals(created.displayOrder, edited.displayOrder)
        assertTrue(edited.updatedAt >= created.updatedAt)

        // 4) Suche (Inhalt, Groß-/Kleinschreibung egal)
        rule.onNodeWithTag("search_field").performTextInput("MEILENSTEIN")
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Willkommen", substring = true).fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Projektplan").assertIsDisplayed()
        rule.onNode(hasTestTag("search_field") and hasSetTextAction()).performTextReplacement("gibtesnicht")
        waitForText("Keine Treffer")
        rule.onNodeWithContentDescription("Suche leeren").performClick()
        waitForText("Willkommen")

        // 5) Über das Menü in den Papierkorb und per „Rückgängig“ zurück
        rule.onAllNodesWithText("Projektplan")[0]
        rule.onNode(hasText("Projektplan")).assertIsDisplayed()
        val before = runBlocking { repo().activeNotes.first().size }
        runBlocking { repo().moveToTrash(created.id) }
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Projektplan").fetchSemanticsNodes().isEmpty() }
        assertEquals(before - 1, runBlocking { repo().activeNotes.first().size })
        runBlocking { repo().restore(created.id) }
        waitForText("Projektplan")
    }

    @Test fun emptyNewNoteIsNotSaved() {
        waitForText("Willkommen")
        val before = runBlocking { repo().activeNotes.first().size }
        rule.onNodeWithTag("fab_new_note").performClick()
        rule.onNodeWithTag("editor_back").performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        rule.waitForIdle()
        assertEquals(before, runBlocking { repo().activeNotes.first().size })
    }

    @Test fun settingsAndPremiumScreensOpen() {
        waitForText("Willkommen")
        rule.onNodeWithContentDescription("Einstellungen").performClick()
        waitForText("Darstellung")
        rule.onNodeWithText("Werbefrei mit Premium").performClick()
        waitForText("Einmal bezahlen – dauerhaft werbefrei. Kein Abonnement.")
    }
}
