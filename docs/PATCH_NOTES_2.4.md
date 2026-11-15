# Patch Notes – Notizen & Listen 2.4 „Designs & Vorlagen“

**Android:** Version 2.4 (versionCode 7) · Zweig `feature/2.4-designs-shop` · Stand 15.11.2026
**Windows-Studio:** Version 1.1.0 (gleicher Funktionsumfang)
Store-Texte (Versionshinweise in 9 Sprachen, je ≤ 500 Zeichen): `docs/PLAY_STORE.md`

---

## Deutsch

### Neu
- **Design-Shop** (Menü ☰ → Design-Shop oder Einstellungen → Design & Papier)
  - 7 Designs, jeweils für hell und Abend:
    - kostenlos: Leder & Papier, Graphit
    - Premium: Mitternacht, Waldhütte, Pergament, Kirschblüte, Ozean
  - 4 Papiermuster für den Editor:
    - kostenlos: Glatt, Liniert (mit Randlinie)
    - Premium: Kariert, Punktraster
  - Jede Karte zeigt eine Live-Mini-Vorschau des Designs.
- **Vorschau 30 Sekunden:** Premium-Designs und -Papiere lassen sich ausprobieren, die ganze App wechselt live mit. Ein Banner zeigt den Countdown und hat einen „Beenden“-Knopf. Die Vorschau endet von selbst, nichts wird gespeichert.
- **Vorlagen:** Einkaufsliste, Besprechung, Packliste, Wochenplan, Rezept, Tagebuch – in allen 9 Sprachen. Aufruf über Menü → Vorlagen oder im Shop; der Editor öffnet vorausgefüllt.
- **Statistik** (Menü → Statistik):
  - Anzahl Notizen, Checklisten, erledigte Punkte und Wörter
  - Fortschrittsring der Checklisten
  - Balken je Kategorie
  - Aktivität der letzten 14 Tage

  Alles wird nur auf dem Gerät berechnet.

### Verbessert
- Beim Designwechsel blenden alle Farben weich über: Leder, Papier, Karten, Linien und Akzent.
- Shop-Animationen:
  - Karten erscheinen gestaffelt.
  - Der Auswahlrahmen wächst animiert.
  - Über das Premium-Abzeichen läuft ein Glanz.
- Statistik-Animationen: Zahlen zählen hoch, Balken wachsen, der Ring zeichnet sich.
- „Animationen“ aus (in den Einstellungen) schaltet auch die neuen Animationen ab.
- Eigene Akzentfarben (Minze, Lavendel, Koralle) gelten weiter. Mit „Gelb“ wird der Akzent des Designs verwendet.

### Premium & Käufe
- Premium-Inhalte sind in 2.4 **nur zum Ansehen** da. Der Knopf „Freischalten – bald“ ist bewusst deaktiviert: Es gibt keinen Kauf und keine Preise in der App.
- **Werbefrei (`remove_ads`) bleibt unverändert** und schaltet keine Designs frei.
- Notizen, Kategorien und Käufe bleiben erhalten. Die Datenbank ist unverändert (Version 5), es läuft keine Migration.

---

## English

### New
- **Design shop** (menu ☰ → Design shop, or Settings → Design & paper)
  - 7 designs, each with a light and an evening version:
    - free: Leather & Paper, Graphite
    - premium: Midnight, Forest cabin, Parchment, Cherry blossom, Ocean
  - 4 paper styles for the editor:
    - free: Plain, Lined
    - premium: Squared, Dotted
  - Each card shows a live mini preview of the design.
- **30-second preview:** you can try premium content and the whole app switches live. A banner shows the countdown and has an End button. The preview ends on its own and nothing is saved.
- **Templates:** shopping list, meeting, packing list, weekly plan, recipe, journal – in all 9 languages.
- **Statistics:**
  - notes, checklists, ticked-off items and words
  - checklist progress ring
  - bars per category
  - activity of the last 14 days

  Everything is calculated on your device only.

### Improved
- Switching designs fades all colours smoothly.
- New animations in the shop and the statistics. They follow the “Animations” setting.

### Premium & purchases
- In 2.4, premium content can only be previewed. “Unlock – coming soon” is disabled on purpose: there is no purchase and no price in the app.
- Ad-free (`remove_ads`) is unchanged and does not unlock designs. All notes and purchases are kept.

---

## Technisch (für die Entwicklung)

| Bereich | Datei |
|---|---|
| Designs, Papier, Vorschau, Freischaltung | `ui/theme/Skins.kt` (`Skin`, `SkinPalette`, `PaperPattern`, `ShopPreview`, `Entitlements`, `Modifier.paperPattern`) |
| Theme | `ui/theme/Theme.kt` – `MyNotesTheme(skin, pattern)`, `LocalSkin`, `LocalPaperPattern`, Vorschau-Zeitgeber |
| Shop | `ui/shop/ShopScreen.kt`, `ui/shop/TemplatePicker.kt` |
| Statistik | `ui/stats/StatsScreen.kt` (`NoteStats.of`) |
| Vorlagen | `data/NoteTemplates.kt`, `NotesViewModel.openTemplate` |
| Einstellungen | DataStore-Schlüssel `skin`, `paper_pattern`, `debug_unlock_all` (Test-Schalter nur in Debug-Builds) |
| Navigation | `Route.STATS`, `Route.SHOP`; Menüeinträge Vorlagen/Statistik/Design-Shop; Einstellungen → Design & Papier |
| Texte | 45 neue Texte × 9 Sprachen |
| Tests | `ShopLogicTest` (Freischaltung, Rückfall, Vorschau, Vorlagen in 9 Sprachen, Statistik). Gesamt **71 grün**, `assembleDebug` erfolgreich |

**Später (vorbereitet):**
1. Einmalkauf, z. B. `personalization_pack`, in der Play Console anlegen. Den Preis legst du dort fest.
2. Den Kauf im `BillingManager` abfragen.
3. Nach dem Kauf `unlocked = {"all"}` speichern. Für einzelne Designs `skin:<NAME>` oder `paper:<NAME>` speichern.

**Vor dem Hochladen prüfen:**
- Auf dem Handy testen: Shop, Vorschau (läuft ab), Vorlagen, Statistik, alle Designs hell und dunkel.
- Den Release-Build in Android Studio signieren (`bundleRelease`).
