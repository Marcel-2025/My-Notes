package dev.flondy.mynotes.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToKey
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
 * End-to-End-Ablauf auf der echten Activity (Robolectric, deutsche Oberfläche, Design „Leder & Papier“).
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = MyNotesApp::class, qualifiers = "de-w411dp-h891dp")
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<NotesActivity>()

    private fun waitForText(text: String, timeout: Long = 8_000) =
        rule.waitUntil(timeout) { rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }

    private fun waitGone(text: String, timeout: Long = 8_000) =
        rule.waitUntil(timeout) { rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isEmpty() }

    private fun repo() = rule.activity.application.app.repository

    /** DataStore lebt im Testprozess weiter – Ansicht für jeden Test auf „Raster“ zurücksetzen. */
    @org.junit.Before fun resetLayout() {
        runBlocking { rule.activity.application.app.settings.setHomeLayout(dev.flondy.mynotes.settings.HomeLayout.GRID) }
        rule.waitForIdle()
    }

    /** Wartet, bis alle 4 Beispielnotizen angelegt sind (nicht jede ist im Raster sichtbar). */
    private fun waitSeeded() {
        rule.waitUntil(8_000) { runBlocking { repo().activeNotes.first().size >= 4 } }
        rule.waitForIdle()
    }

    @Test fun fullNoteLifecycle() {
        // 1) Erststart: Beispielnotizen, Kategorien als Chips, Kopfzeile
        waitForText("Willkommen")
        rule.onNodeWithText("Meine Notizen").assertIsDisplayed()
        rule.onNodeWithText("Einkaufsliste").assertIsDisplayed()
        rule.onNodeWithText("Reiseplanung").assertIsDisplayed()
        rule.onNodeWithText("Listen").assertIsDisplayed()

        // 2) Neue Notiz mit Titel und formatiertem Text
        rule.onNodeWithTag("fab_new_note").performClick()
        rule.onNodeWithTag("editor_title").performTextInput("Projektplan")
        rule.onNodeWithTag("editor_content").performTextInput("Meilenstein 1 fertigstellen")
        rule.onNodeWithContentDescription("Fett").performClick() // Steuerzeichen am Cursor
        rule.onNodeWithTag("editor_save").performClick()
        waitForText("Projektplan")
        val created = runBlocking { repo().activeNotes.first().first { it.title == "Projektplan" } }
        assertTrue(created.content.startsWith("Meilenstein 1 fertigstellen"))

        // 3) Antippen öffnet den Editor; Checkliste hinzufügen; Speichern unter derselben ID
        rule.onNodeWithText("Projektplan").performClick()
        rule.onNodeWithTag("editor_content").performTextReplacement("Meilenstein 2")
        rule.onNodeWithTag("add_checklist").performClick()
        rule.onNode(hasSetTextAction() and androidx.compose.ui.test.hasContentDescription("Eintrag")).performTextInput("Angebot schreiben")
        rule.onNodeWithTag("editor_back").performClick()
        rule.waitUntil(8_000) { runBlocking { repo().getNote(created.id)?.checklist == "[ ] Angebot schreiben" } }
        val edited = runBlocking { repo().getNote(created.id)!! }
        assertEquals("Meilenstein 2", edited.content)
        assertEquals("Erstellungsdatum bleibt erhalten", created.createdAt, edited.createdAt)

        // 4) Suche über die Lupe: Ergebniszeile + Treffer auch in Checklisten
        rule.onNodeWithContentDescription("Suchen").performClick()
        rule.onNodeWithTag("search_field").performTextInput("ANGEBOT")
        waitForText("1 Ergebnis für")
        waitGone("Willkommen")
        rule.onNodeWithText("Projektplan").assertIsDisplayed()
        rule.onNode(hasTestTag("search_field") and hasSetTextAction()).performTextReplacement("gibtesnicht")
        waitForText("Keine Treffer")
        rule.onNodeWithContentDescription("Suche leeren").performClick() // leert
        rule.onNodeWithContentDescription("Suche leeren").performClick() // schließt
        waitForText("Willkommen")

        // 5) Archivieren → Archiv-Tab → zurückholen
        runBlocking { repo().setArchived(created.id, true) }
        waitGone("Projektplan")
        rule.onNodeWithTag("tab_archive").performClick()
        waitForText("Projektplan")
        runBlocking { repo().setArchived(created.id, false) }
        rule.onNodeWithTag("tab_notes").performClick()
        waitForText("Projektplan")

        // 6) Favoriten-Tab zeigt die Reiseplanung (Beispiel ist Favorit)
        rule.onNodeWithTag("tab_favorites").performClick()
        waitGone("Einkaufsliste")
        rule.onNodeWithText("Reiseplanung").assertIsDisplayed()
    }

    @Test fun duplicateFromEditor() {
        waitSeeded()
        val shopping = runBlocking { repo().activeNotes.first().first { it.title == "Einkaufsliste" } }
        rule.onNodeWithTag("note_list").performScrollToKey(shopping.id)
        rule.onNodeWithText("Einkaufsliste").performClick()
        rule.onNodeWithText("Duplizieren").performClick()
        rule.waitUntil(8_000) { runBlocking { repo().activeNotes.first().any { it.title == "Einkaufsliste (Kopie)" } } }
        val copy = runBlocking { repo().activeNotes.first().first { it.title == "Einkaufsliste (Kopie)" } }
        assertTrue(copy.checklist.contains("Milch"))
    }

    @Test fun emptyNewNoteIsNotSaved() {
        waitSeeded()
        val before = runBlocking { repo().activeNotes.first().size }
        rule.onNodeWithTag("fab_new_note").performClick()
        rule.onNodeWithTag("editor_back").performClick()
        rule.waitForIdle()
        Thread.sleep(800)
        rule.waitForIdle()
        assertEquals(before, runBlocking { repo().activeNotes.first().size })
    }

    @Test fun overviewAndSettings() {
        waitForText("Willkommen")
        rule.onNodeWithContentDescription("Weitere Optionen").performClick()
        rule.onNodeWithText("Übersicht").performClick()
        waitForText("Kategorien")
        rule.onNodeWithTag("overview").assertIsDisplayed()
        rule.onNodeWithContentDescription("Weitere Optionen").performClick()
        rule.onNode(androidx.compose.ui.test.hasText("Einstellungen") and androidx.compose.ui.test.hasAnyAncestor(androidx.compose.ui.test.isPopup())).performClick()
        waitForText("Darstellung")
        rule.onNodeWithText("Werbefrei mit Premium").performClick()
        waitForText("Einmal bezahlen – dauerhaft werbefrei. Kein Abonnement.")
    }
}
