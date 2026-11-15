# My Notes 2.0 – Modernisierungsbericht

Stand: 09.10.2026 · Branch `feature/modernisierung` · versionCode 3 / versionName 2.0

---

## Wiederherstellen & Zurücksetzen

| Sicherung | Ort |
|---|---|
| Git-Tag des Ausgangszustands | `backup/vor-modernisierung-2026-10-09` (Commit `b65152b`) |
| Komplettes Archiv (inkl. .git) | `AndroidStudioProjects\_Backups\My Notes_vor-Modernisierung_2026-10-09.tar.gz` |
| Datenbankschemas | `app/schemas/dev.flondy.mynotes.AppDatabase/3.json` (alt) und `4.json` (neu) |
| Branch mit allen Änderungen | `feature/modernisierung` – `main` ist unverändert |

```bash
git checkout main                                   # alte Version (unverändert)
git checkout feature/modernisierung                 # neue Version
git diff backup/vor-modernisierung-2026-10-09 --stat  # alle Änderungen
git reset --hard <commit>                           # auf einen Meilenstein zurück (nur im Branch!)
```
Meilensteine im Branch: `a15df3e` Schema v3 exportiert → `5abc13c` Modernisierung → `d86429a` Tests.

> ⚠️ Wichtig: Eine bereits installierte **2.0 mit Datenbank v4** kann nicht auf eine ältere App-Version
> zurückgestuft werden (Android erlaubt keine Downgrades; Room kann v4 → v3 nicht öffnen). Vor dem
> Release deshalb gründlich testen – siehe Checkliste unten.

---

## A. Projektanalyse (Ausgangszustand 1.2 / versionCode 2)

**Vorhanden:** Notizen anlegen/bearbeiten/löschen (Dialoge), Suche, 5 Sortierungen, Dark-Mode-Schalter,
Wiggle-Modus mit Drag-Griff, AdMob-Banner, Einmalkauf `remove_ads`, Room v3 mit Migration 2→3, Vollbildmodus.

**Gefundene Fehler / Risiken**
1. Bearbeiten überschrieb `timestamp` → Erstellungsdatum ging verloren, „Neueste/Älteste“ war nach jeder Bearbeitung falsch.
2. Drag & Drop per `translationY` innerhalb der LazyColumn → Karte verschwand beim Scrollen/hinter anderen Karten; Index-Verwechslung zwischen gefilterter und Gesamtliste.
3. Premium-Status nur im Arbeitsspeicher → nach jedem Start kurz Werbung; ausstehende (pending) Käufe wurden nie verarbeitet; Rückerstattungen entzogen Premium nicht.
4. Keine Einwilligung (UMP) vor dem Laden von Werbung in der EU.
5. Beispielnotizen wurden bei jeder leeren Datenbank erneut angelegt (auch nachdem der Nutzer alles gelöscht hatte).
6. Löschen ohne Rückfrage/Undo; `removeNote` nummerierte anhand eines evtl. veralteten Snapshots neu.
7. Billing Library 6.1 und targetSdk 35 – beides für Play-Updates seit 31.08.2026 nicht mehr ausreichend.
8. Doppelte AdMob-Dependency (23.0.0 + 23.1.0), `minifyEnabled = false`, `exportSchema = false`.
9. Alle Texte hartkodiert deutsch; App-Name „My Notes“; Standard-Lila-Theme statt Markenfarbe; 1.100 Zeilen in `MainActivity`.

---

## B. Implementierte Verbesserungen

**Architektur** (vorher 1 Datei mit 1.100 Zeilen):
```
MyNotesApp (DI) ─ AppDatabase/NoteDao/CategoryDao ─ data/NoteRepository, NoteFilters, Checklist, BackupFormat
                ─ SettingsDataStore (+ settings/AppSettings)
                ─ billing/BillingManager · ads/AdsManager, AdBanner · icons/AppIconManager
NotesActivity ─ ui/AppRoot (Navigation) ─ home · editor · settings · trash · premium · components · theme
NotesViewModel (bestehende API addNote/updateNote/removeNote/moveNote/deleteAllNotes erhalten) · SettingsViewModel
```

**Datenbank v4** – rein additive Migration 3→4 (nur `ADD COLUMN` + neue Tabelle `categories`):
`createdAt` (aus altem `timestamp` übernommen), `isPinned`, `isFavorite`, `color`, `categoryId`, `noteType`, `deletedAt`.
Kein `fallbackToDestructiveMigration()`. Schemas werden exportiert.

**UI/UX & Design**
- Designsystem: Markengelb `#FDEE51`, Ink `#0C0F1C`/`#111827`, Weiß `#F8FAFC`, eigene Typografie & Formen; Light Mode mit dunkler Primärfarbe (Kontrast), Gelb nur für Akzente
- Startseite mit Farbverlauf, Suchfeld, Filter-Chips, Sortier-/Filter-Sheet, Extended-FAB + Checklisten-FAB, Snackbar mit „Rückgängig“
- Notizkarten: Titel, Vorschau, Checklisten-Fortschritt, Kategorie, relatives Datum, Pin/Favorit, Suchtreffer-Hervorhebung, kompakte Ansicht
- Vollbild-Editor mit Autosave (600 ms), Speicherstatus, Wortanzahl, Erstellt/Bearbeitet, Farbe/Kategorie-Sheet, Verwerfen, Teilen, TXT-Export
- Tablet/Querformat: Inhalte max. 760 dp breit und zentriert; Edge-to-Edge mit korrekten Insets; `adjustResize` für die Tastatur

**Animationen:** Theme-Überblendung, `animateItem` (Ein-/Ausblenden, Platzmachen), Karten-Press-Scale, angehobene Drag-Karte,
FAB-Expand/Collapse, Editor-Übergang (Fade+Scale), Seitenübergänge, Premium-Emblem; alles abschaltbar und
automatisch aus, wenn Android-Animationen deaktiviert sind.

**Drag & Drop (neu geschrieben):** Overlay oberhalb der Liste, Platzhalter mit gestrichelter Einfügemarkierung,
Geste auf dem Container im Initial-Pass (keine Konflikte mit Scrollen/Klick), Auto-Scroll am Rand, stabile IDs,
variable Kartenhöhen, angeheftete Notizen bleiben in ihrer Gruppe, einmaliges Speichern beim Loslassen,
auch in Kategorie-/Favoritenfiltern korrekt (Teilmengen-Algorithmus). Deaktiviert bei Suche und automatischer Sortierung.
Langer Druck ohne Bewegung öffnet das Kontextmenü; TalkBack-Aktionen „nach oben/unten verschieben“.

**Neue Funktionen:** Favoriten · Anheften · Kategorien (Standardkategorien, anlegen/umbenennen/löschen) · 9 Notizfarben ·
echte Checklisten (abhaken, hinzufügen per Enter, löschen, per Griff verschieben, Erledigte ausblenden, Umwandeln Text↔Liste) ·
Papierkorb (30 Tage Auto-Bereinigung) · Teilen · TXT-Export · JSON-Backup/Import mit Vorschau & Duplikaterkennung ·
6 Sortierungen + „Favoriten zuerst“ (gespeichert) · Filter (Favoriten, Angeheftet, Listen, Kategorie, Heute/7/30 Tage, nur mit Inhalt) ·
5 App-Icons · neue Einstellungen · Deutsch & Englisch.

**Performance:** Filter/Sortierung auf `Dispatchers.Default`, stabile Keys + `contentType`, Reihenfolge-Updates schreiben nur
`displayOrder` (keine veralteten Objekte), Ad-SDK-Initialisierung im Hintergrund, kein Neuladen des Banners bei Recomposition,
R8 + Resource-Shrinking im Release.

---

## C. Monetarisierung

**AdMob:** adaptiver Anker-Banner, nur auf der Startseite, nie im Editor oder bei offener Tastatur; Lifecycle pause/resume/destroy;
Ladefehler blenden den Platz aus; Testanzeigen im Debug-Build (offizielle Google-Test-ID), echte ID nur im Release.
Bewusst **keine** Interstitials/Rewarded Ads – sie würden das Schreiben unterbrechen.

**Einwilligung:** Google UMP 3.2 – Formular vor dem ersten Laden von Anzeigen (EWR/UK), „Werbeeinwilligung verwalten“ in den
Einstellungen. Werbefreie Nutzer sehen weder Formular noch Werbung.
👉 In der AdMob-Konsole unter *Datenschutz & Mitteilungen* muss eine **DSGVO-Mitteilung** veröffentlicht sein, sonst zeigt UMP nichts an.

**Billing 8.3:** Produkt-ID `remove_ads` unverändert; bei jedem Start + `onResume` Abfrage bei Google Play (Quelle der Wahrheit);
lokaler Cache nur gegen Werbe-Aufblitzen, wird bei Rückerstattung widerrufen; Acknowledge mit Wiederholung; Pending-Käufe mit Hinweis;
„Käufe wiederherstellen“ mit Rückmeldung; automatische Wiederverbindung. Debug-Schalter „Werbefrei simulieren“ existiert **nur** in Debug-Builds
(`BuildConfig.DEBUG`), im Release ist er nicht vorhanden.

**Premium-Seite:** dunkles Design, Goldakzente, Vorteile, Preis ausschließlich aus Google Play, „Einmal bezahlen – dauerhaft werbefrei. Kein Abonnement.“,
Wiederherstellen, Hinweis bei ausstehender Zahlung bzw. wenn das Produkt nicht verfügbar ist.

**Neue Einnahmemöglichkeiten (Vorschlag, nicht implementiert – benötigt neue Produkte in der Play Console):**
| Produkt-ID (Vorschlag) | Typ | Inhalt | Bestandskunden |
|---|---|---|---|
| `remove_ads` (bestehend) | Einmalkauf | Werbefreiheit | unverändert |
| `personalization_pack` | Einmalkauf | zusätzliche Themes, Karten-Designs, Hintergründe | separat, `remove_ads` wird **nicht** umgedeutet |
| `premium_bundle` | Einmalkauf | `remove_ads` + `personalization_pack` | Rabatt für Käufer von `remove_ads` über eigenes Produkt/Promo-Code |

Die App-Icons sind in 2.0 bewusst **kostenlos**, weil noch kein passendes Produkt existiert. Grundfunktionen bleiben immer kostenlos.

---

## D. Qualitätssicherung

**Builds (im Cloud-Workspace ausgeführt):** `assembleDebug` ✅ · `assembleRelease` mit R8/Shrinking ✅ · `bundleRelease` (AAB, unsigniert) ✅ · `lintDebug`: 0 Fehler ✅

**Automatisierte Tests – 41/41 bestanden** (`./gradlew testDebugUnitTest`, Robolectric, ohne Gerät):
| Bereich | Tests |
|---|---|
| Migration v3→v4 mit echten Daten (Umlaute, Emoji, 100.000 Zeichen, IDs, Reihenfolge), Schema-Validierung, Öffnen mit Room | 2 |
| Suche (Titel/Inhalt, Groß-/Klein), keine Treffer, leere Suche, alle Sortierungen, Kombination, Filter, Datum, Reorder-Regeln, Reihenfolge-Berechnung (hoch/runter/erste/letzte/Teilmenge) | 16 |
| Repository: Bearbeiten behält ID/Erstelldatum/Reihenfolge, Reihenfolge nach „Neustart“, Papierkorb/Wiederherstellen/Bereinigung, Kategorie löschen, Backup-Roundtrip, ungültige Backups, 50 parallele Schreibvorgänge | 8 |
| Drag & Drop-Gesten: Tippen öffnet, nach unten, an erste/letzte Position, Auto-Scroll, Lang drücken ohne Bewegung, angeheftete Gruppe, deaktiviert | 8 |
| End-to-End auf der echten Activity: Start, neue Notiz, Bearbeiten, Suche, Keine-Treffer, Papierkorb/Wiederherstellen, leere Notiz nicht gespeichert, Einstellungen/Premium | 3 |
| Checklisten-Format | 4 |

**Nicht automatisiert getestet – bitte manuell auf einem echten Gerät prüfen:**
- [ ] Update-Installation: aktuelle Play-Version (1.2) installieren, Notizen anlegen, dann 2.0 **darüber** installieren → alle Notizen da?
- [ ] Echter Kauf mit Lizenztester-Konto (Play Console → Lizenztests), Abbruch, „Bereits gekauft“, Wiederherstellen, Rückerstattung
- [ ] Bestandskäufer: Konto mit bestehendem `remove_ads`-Kauf → keine Werbung
- [ ] UMP-Formular (EU-Gerät oder Debug-Geografie) und „Werbeeinwilligung verwalten“
- [ ] Echte Werbebanner im Release (interner Test-Track)
- [ ] Drag & Drop „im Gefühl“ auf echtem Touchscreen, langer Text, 1.000 Notizen (Performance)
- [ ] App-Icon-Wechsel auf Pixel-/Samsung-/Xiaomi-Launcher (keine doppelten Icons)
- [ ] TalkBack, große Schrift (200 %), Querformat, Tablet, Android 7 (minSdk 24)

---

## E. Google Play

- **Release-Name:** `2.0 – Premium Redesign` · **versionCode 3**, **versionName 2.0**
- Store-Texte, Versionshinweise, Screenshots, Feature Graphic: siehe [`PLAY_STORE.md`](PLAY_STORE.md)

**Release-Checkliste**
1. [ ] Branch prüfen, in Android Studio *Build → Generate Signed App Bundle* mit **deinem bestehenden Upload-Key**
2. [ ] Manuelle Tests oben auf dem internen Test-Track
3. [ ] AdMob: DSGVO-Mitteilung veröffentlicht, App-ID/Banner-ID unverändert
4. [ ] **Data Safety** prüfen: Werbe-ID & App-Interaktionen (AdMob), Kaufverlauf (Billing), Absturz-/Diagnosedaten (SDKs);
       Notizen werden **nicht** erhoben (lokal; Android-Auto-Backup bleibt aktiv – in der Datenschutzerklärung erwähnen)
5. [ ] Datenschutzerklärung (`marcel-2025.github.io/My-Notes-privacy`) um Einwilligungsdialog, lokales Backup und Auto-Backup ergänzen
6. [ ] Store-Texte DE/EN eintragen, neue Screenshots
7. [ ] Gestaffelter Rollout (z. B. 20 % → 100 %), Absturzberichte in der Play Console beobachten

**Hinweise zu Android Studio:** Kotlin 2.1.21, Room 2.7.2, compileSdk/targetSdk 36 – beim ersten Öffnen *Sync Project with Gradle Files*
und ggf. die Android-16-SDK-Plattform installieren. Die neuen Tests laufen mit `./gradlew testDebugUnitTest`.

---

## F. Roadmap (priorisiert nach Nutzen / Aufwand / Risiko)

| Prio | Thema | Nutzen | Aufwand | Risiko |
|---|---|---|---|---|
| 1 | Erinnerungen pro Notiz (Benachrichtigung, exakte Alarme nur mit Erlaubnis) | sehr hoch (Bindung) | mittel | mittel (Berechtigungen) |
| 2 | Startbildschirm-Widget (Glance) für Notiz/Checkliste | hoch | mittel | gering |
| 3 | Automatische lokale Backups (wöchentlich, Ordner per SAF) | hoch (Vertrauen) | gering | gering |
| 4 | `personalization_pack` (Themes, Karten-Designs) als 2. Einmalkauf | Umsatz | mittel | gering |
| 5 | Serverseitige Kaufprüfung (Play Developer API) | Sicherheit | mittel | gering |
| 6 | Mehrfachauswahl (teilen, verschieben, löschen) | mittel | gering | gering |
| 7 | Rich-Text/Markdown-Formatierung, Bilder in Notizen | mittel | hoch | mittel |
| 8 | Optionaler, verschlüsselter Cloud-Sync (nur nach ausdrücklicher Wahl) | hoch | sehr hoch | hoch (Datenschutz) |
| 9 | App-Sperre (Biometrie) für einzelne Notizen | mittel, gut als Premium | mittel | gering |
| 10 | Weitere Sprachen (ES, FR, IT, TR) | Reichweite | gering | gering |

Größtes Potenzial für **Nutzerbindung**: Erinnerungen + Widget. Für **Umsatz**: Personalisierungspaket und App-Sperre als faire Einmalkäufe.

---

# Version 2.1 – Design „Leder & Papier“ (21.10.2026)

Branch `feature/design-leder` · versionCode 4 · versionName 2.1 · Datenbank v5

## Sicherungen
| Was | Wo |
|---|---|
| Design 2.0 (Premium Dark/Gelb) | Branch `design/v2.0-premium-dark`, Tag `backup/design-v2.0-2026-10-21` |
| Deine Änderungen (AGP 9.4.1, Kotlin 2.2.10, Gradle 9.6, Icon) | Commit `2561f36` |

Zurück zum alten Design: `git checkout design/v2.0-premium-dark`
⚠️ Wer 2.1 (DB v5) installiert hat, kann nicht auf 2.0 (DB v4) zurück – Android erlaubt keine Downgrades.

## Neu
- **Design:** Leder-Kopfzeilen mit feiner Narbung und Ziernaht, Papierkarten, Gold-Akzente, Blattgrün, Schrift **Nunito** (OFL, `docs/licenses/Nunito-OFL.txt`), Abend-Variante (dunkel)
- **App-Icon:** dein Leder-Notizbuch – mit passendem dunkelbraunen Hintergrund statt blauem Rand, sicher für Kreis-/Squircle-Masken; Themen-Icon (Android 13) und Legacy-Icons (Android 7) ergänzt
- **Startseite:** ☰-Seitenmenü, Lupe, ⋮-Menü (Ansicht, Sortieren & Filtern, Hell/Dunkel, Einstellungen); Chips *Alle · Notizen · Listen · Kategorien mit Icons*
- **3 Ansichten:** Raster (2 Spalten, Tablet automatisch mehr), Liste, Übersicht (Angeheftet · Favoriten · Kategorien mit Anzahl · Meine Notizen)
- **Untere Navigation:** Notizen · Favoriten · Archiv
- **Suche:** „n Ergebnisse für …“ + Sortier-Menü direkt in der Ergebniszeile; findet auch Checklisten-Einträge
- **Drag & Drop jetzt auch im Raster** (Overlay-Technik, zweidimensionale Zielerkennung)
- **Archiv:** Notizen archivieren / zurückholen (mit „Rückgängig“)
- **Duplizieren** (Kontextmenü und Editor)
- **Kategorien mit Icon** (19 Symbole), neue Standardkategorie „Reisen“ (nur Neuinstallationen); bestehende Kategorien bekommen automatisch ein passendes Icon
- **Editor:** Titel- und Kategorie-Bereich, Checkliste **und** Notiztext in einer Notiz, Formatierungsleiste (fett, kursiv, unterstrichen, Aufzählung, Nummerierung, Link), Aufzählungen setzen sich bei Enter fort, Zeichenanzahl, unten *Löschen · Duplizieren · Speichern*
- **Kartendatum:** „Heute, 09:41“, „Gestern, 18:30“, „25. Mai 2024“

## Datenbank v4 → v5 (getestet)
Nur neue Spalten (`isArchived`, `checklist`, Kategorie `icon`/`color`). Einzige Datenübernahme: Bei bisherigen
Checklisten wird die Liste aus `content` nach `checklist` verschoben – nichts wird gelöscht. Backup-Format v2;
Backups aus 2.0 (v1) werden beim Import automatisch umgewandelt.

## Bewusste Abweichungen von den Store-Grafiken
- Formatierung über unsichtbare Steuerzeichen (Markdown-ähnlich) statt Rich-Text-Engine – robust, suchbar, kompatibel
- Zeichenanzahl ohne Limit (statt „142/1000“)
- Übersicht nennt den Bereich „Kategorien“ statt „Meine Listen“, um Verwechslung mit Checklisten zu vermeiden
- „Cloud Sync“ im Beispiel-Screenshot ist nur Notizinhalt – die App hat keine Cloud-Synchronisierung

## Qualitätssicherung 2.1
`testDebugUnitTest` **58/58 bestanden** (u. a. Migration 4→5, Formatierung, Archiv, Duplizieren, Backup v1/v2,
Raster-Drag-&-Drop links/rechts/hoch/runter, End-to-End mit Suche, Archiv- und Favoriten-Tab, Übersicht, Einstellungen) ·
`assembleRelease` + `bundleRelease` (R8) ✅ · Lint: 0 Fehler. Bildschirmfotos hell/dunkel visuell geprüft.

**Bitte manuell auf dem Gerät prüfen:** Update von 2.0 → 2.1 mit echten Notizen, Drag & Drop im Raster „im Gefühl“,
Formatierungsleiste mit Tastatur, Icon auf dem Startbildschirm (Pixel/Samsung), dunkler Modus.


---

## Version 2.2 – Tablets & Querformat (versionCode 5)

Ziel: Auf Tablets – vor allem im Querformat – gut aussehen und bequem bedienbar sein, ohne geteilte Ansichten (kein Liste/Detail nebeneinander).

**Umsetzung**
- `ui/components/Adaptive.kt`: `WindowLayout` leitet aus Breite/Höhe (dp) Breitenklasse (Kompakt < 600, Mittel < 840, Erweitert ≥ 840), Querformat und „niedrig“ (< 480 dp) ab. Keine neue Bibliothek.
- **Seitliche Navigation (Navigation Rail)** auf dem Leder statt Leiste unten: Tablets quer, große Tablets (≥ 840 dp) und Handys quer. Der „+“-Button sitzt oben in der Leiste.
- **Kompakte Kopfzeile** (eine Zeile mit Logo, Titel, Suche, Menü) im Querformat.
- **Startseite:** Inhaltsbreite bis 1360 dp; Raster mit adaptiver Kartenbreite (156/190/216 dp); Liste mit 2–3 Spalten; Übersicht mit Kartenzeilen (2–3) und Kategorie-Kacheln (2/3/4 Spalten); breitere Favoriten-Karten.
- **Editor, Einstellungen, Kategorien, Papierkorb:** Lesebreite 760 dp bzw. 880 dp auf großen Tablets; Editor-Leiste unten auf Textbreite zentriert und im Handy-Querformat flacher.
- **Querformat-Ränder:** seitliche Navigationsleiste und Kamera-Aussparung werden ausgespart (Fläche dahinter in Leder-Braun).
- Handy im Hochformat bleibt unverändert.

**Tests:** 63 Unit-/UI-Tests grün, neu `WindowLayoutTest` (5 Fälle). Zusätzlich wurden Bildschirmfotos in 4 Größen (411×891, 891×411, 800×1280, 1280×800 dp) geprüft (temporäres Roborazzi, nicht im Projekt).


---

## Version 2.3 – 9 Sprachen (versionCode 6)

- **Neue Übersetzungen:** Spanisch (`values-es`), Französisch (`values-fr`), Italienisch (`values-it`), Portugiesisch (`values-pt`, brasilianisch, gilt auch für Portugal), Polnisch (`values-pl`), Russisch (`values-ru`), Türkisch (`values-tr`) – jeweils alle 269 Texte und 3 Pluralformen (Russisch/Polnisch mit one/few/many/other). Englisch (`values`) und Deutsch (`values-de`) wie bisher.
- **Sprach-Umschalter:** Einstellungen → Sprache (Systemsprache oder eine der 9 Sprachen, jeweils in eigener Schreibweise).
  - Android 13+: offizielle App-Sprache über `LocaleManager`; zusätzlich in den Systemeinstellungen („App-Sprachen“) dank `res/xml/locales_config.xml` und `android:localeConfig`.
  - Android 7–12: Auswahl in SharedPreferences, angewendet in `attachBaseContext` von Activity und Application (`settings/AppLanguage.kt`), Activity wird neu gestartet.
- **App Bundle:** `bundle { language { enableSplit = false } }` – alle Sprachen werden ausgeliefert, sonst hätte der Umschalter auf Geräten mit anderer Systemsprache keine Texte.
- Bestehende Notizen und Kategorien bleiben unverändert (es sind Nutzerdaten); Beispielnotizen und Standardkategorien entstehen bei Neuinstallation in der jeweiligen Sprache.
- **Tests:** 66 grün, neu `TranslationsTest` (alle Texte vorhanden, gleiche Platzhalter, Pluralformen, Sprachliste = locales_config). Bildschirmfotos in Russisch, Französisch, Türkisch, Polnisch (Handy) und Spanisch (Tablet quer) geprüft.


---

## Version 2.4 – Designs & Vorlagen (versionCode 7)

- **Designs (Skins):** `ui/theme/Skins.kt` – 7 Paletten (Leder & Papier, Graphit kostenlos; Mitternacht, Waldhütte, Pergament, Kirschblüte, Ozean Premium), je hell + Abend. `MyNotesTheme(skin, pattern)` übernimmt Farben, Zusatzfarben blenden animiert über. Eigene Akzentfarbe gilt weiter (außer „Gelb“ = Akzent des Designs).
- **Papiermuster:** glatt, liniert (kostenlos), kariert, Punktraster (Premium) – gezeichnet hinter den Editor-Karten (`Modifier.paperPattern`).
- **Design-Shop** (`ui/shop/ShopScreen.kt`): Live-Mini-Vorschau je Design, Abzeichen „Kostenlos/Premium“ mit Glanz, gestaffeltes Einblenden. Premium: „Vorschau 30 s“ (`ShopPreview`, nur im Speicher, endet von selbst) und ein **deaktivierter** Knopf „Freischalten – bald“. Es gibt **keinen** Kauf und keine Preise in der App.
- **Freischaltung vorbereitet:** `Entitlements` (kostenlos immer; Premium nur mit Eintrag `skin:<NAME>`/`paper:<NAME>` oder `all`). Gespeicherte Premium-Wahl ohne Freischaltung fällt auf Leder & Papier/Glatt zurück. In Debug-Builds gibt es einen Test-Schalter (`debug_unlock_all`), im Release nicht. `remove_ads` bleibt unverändert und schaltet **keine** Designs frei.
- **Vorlagen** (`data/NoteTemplates.kt`): 6 Vorlagen in allen 9 Sprachen (Menü → Vorlagen oder im Shop), öffnen den Editor vorausgefüllt.
- **Statistik** (`ui/stats/StatsScreen.kt`): Kennzahlen, Checklisten-Fortschritt (Ring), Balken je Kategorie, Aktivität 14 Tage – nur lokal berechnet.
- **Einstellungen:** neuer Eintrag „Design & Papier“ → Shop. Menü: Vorlagen, Statistik, Design-Shop.
- **Neue Einstellungen (DataStore):** `skin`, `paper_pattern`, `debug_unlock_all`. Datenbank unverändert (Version 5), keine Migration nötig.
- 45 neue Texte in allen 9 Sprachen.
- **Für den späteren Kauf (Vorschlag, nicht angelegt):** Einmalkauf-Produkt `personalization_pack` (alle Premium-Designs + Papiermuster) in der Play Console anlegen, Preis dort festlegen; in `BillingManager` abfragen und bei Kauf `unlocked = {"all"}` setzen.
