# PROMPT für Claude Code: „Notizen & Listen Studio“ für Windows

Stand: 14.11.2026, erstellt aus dem echten Projekt `C:\Users\singl\AndroidStudioProjects\My Notes` (Version 2.3, versionCode 6).
Einfach komplett in Claude Code einfügen. Claude Code arbeitet die Phasen der Reihe nach ab und meldet sich nach jeder Phase.

---

## 0. Auftrag in einem Satz

Baue aus der Android-App **Notizen & Listen** (dev.flondy.mynotes) eine **Windows-Desktop-App** mit demselben Design „Leder & Papier“
und einem eigenen Bereich **„Geräte-Studio“**. Dort läuft dieselbe App live in einem **Handy**- und einem **Tablet-Rahmen**
(hoch und quer). So kann Marcel Desktop, Tablet und Handy in **einer** App ansehen und bedienen, mit schönen Animationen und einer klaren Übersicht.

## 1. Harte Regeln (vor allem anderen lesen)

1. **Die Android-App bleibt unangetastet.** Nichts in `AndroidStudioProjects\My Notes` ändern, nichts committen, keine Gradle-Dateien anfassen.
   Das Desktop-Projekt kommt in einen **neuen Ordner**: `C:\Users\singl\AndroidStudioProjects\My Notes Studio`.
   Code aus der Android-App wird **kopiert** und angepasst, nicht verschoben.
2. `applicationId`, Produkt-ID `remove_ads`, AdMob-IDs und Play-Billing kommen **nicht** in die Desktop-App. Unter Windows gibt es
   **keine Werbung und keine Käufe**. Keine Preise erfinden, keine Premium-Sperren.
3. **Nutzerdaten nie löschen.** Kein `fallbackToDestructiveMigration`. Datenbank-Schema exportieren, Migrationen nur additiv.
4. **Kein Cloud-Sync, keine Konten, keine Telemetrie.** Notizen bleiben lokal auf dem PC.
5. **Keine geteilten Bildschirme** (keine Liste links und Editor rechts als Standard). Marcel mag keine Splitscreens.
   Der Editor öffnet als eigene Seite mit Übergangsanimation oder auf Wunsch in einem **eigenen Fenster**.
6. Nur Funktionen bauen, die hier stehen. Nichts veröffentlichen, nichts hochladen, keinen Microsoft-Store-Eintrag anlegen.
7. Vor jeder größeren Änderung im neuen Projekt einen Git-Commit machen (eigenes Repo im neuen Ordner).
8. Alle Texte in den **9 Sprachen** der App (DE, EN, ES, FR, IT, PT, PL, RU, TR). Neue Texte sauber übersetzen.
9. Schrift **Nunito** (OFL, Lizenz liegt in `docs/licenses/Nunito-OFL.txt`), Lizenzdatei mitliefern.

## 2. Ausgangslage (aus dem Projekt gelesen)

- Kotlin 2.2.10, Jetpack Compose (Material 3), Room (DB v5, Schemas in `app/schemas`), DataStore, Lifecycle-ViewModel.
- Wichtige Dateien zum Übernehmen:

| Bereich | Android-Datei | Desktop |
|---|---|---|
| Datenmodell | `Note.kt`, `Category.kt`, `NoteDao.kt`, `CategoryDao.kt`, `AppDatabase.kt` | übernehmen (Room KMP) |
| Logik | `data/NoteRepository.kt`, `NoteFilters.kt`, `Checklist.kt`, `RichText.kt`, `BackupFormat.kt` | 1:1 übernehmen (reines Kotlin) |
| ViewModels | `NotesViewModel.kt`, `SettingsViewModel.kt` | übernehmen, Android-Context entfernen |
| Design | `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `ui/components/Leather.kt` | übernehmen (Farben exakt gleich) |
| Layout | `ui/components/Adaptive.kt` (`WindowLayout`: Kompakt < 600, Mittel < 840, Erweitert ≥ 840 dp) | **Herzstück** für Geräte-Studio |
| Screens | `ui/home`, `ui/editor`, `ui/settings`, `ui/trash`, `NoteCard`, `ReorderableNoteList`, `NoteActionsSheet` | übernehmen und anpassen |
| Texte | `res/values*/strings.xml` (269 Texte, 9 Sprachen) | nach `composeResources/values*/` kopieren |
| Nicht übernehmen | `ads/*`, `billing/*`, `icons/AppIconManager.kt`, `ui/premium/*`, `NotesActivity.kt` | entfällt |

- Farben (exakt aus `Color.kt`): Leder `#5A3426` / hell `#74452F` / dunkel `#3E2219` / tief `#2A1710`, Schrift auf Leder `#FFF3DC`,
  Papier `#F8EBD7`, Karte `#FFF8EA`, Linie `#EAD8BC`, Tinte `#3B2418`, Gold `#F6C445` / tief `#E0A526`, Blattgrün `#6DAA2C`, Beere `#D9443A`,
  Abend: Hintergrund `#1C130E`, Karte `#2F2118`, Text `#F6E9D5`.

## 3. Technik-Entscheidung

- **Compose Multiplatform (JetBrains) für Desktop/JVM**, damit die vorhandenen Compose-Screens fast unverändert laufen.
  Neueste **stabile** Version nehmen und die Kompatibilität mit der Kotlin-Version prüfen (Kompatibilitätstabelle von JetBrains).
- **Room KMP** mit `androidx.sqlite:sqlite-bundled` (BundledSQLiteDriver), DataStore-Preferences KMP, Lifecycle-ViewModel KMP.
- Datenordner: `%APPDATA%\Flondy\NotizenUndListen\` (Datenbank, Einstellungen, automatische Sicherungen).
- Verpackung: `compose.desktop` → `nativeDistributions` mit **MSI** (und portable ZIP). Paketname „Notizen & Listen Studio“,
  Hersteller „Flondy Dev.“, Icon aus dem App-Logo (`drawable-nodpi/app_logo.png` → `.ico` in 16/32/48/256 px).
- Projektstruktur (ein Modul reicht am Anfang):
```
My Notes Studio/
  composeApp/src/jvmMain/kotlin/dev/flondy/mynotes/studio/
    Main.kt                      // Fenster, Tray, Menüs, Tastenkürzel
    data/  (aus Android kopiert)  // Room, Repository, Backup, Filter
    ui/theme, ui/components, ui/home, ui/editor, ui/settings, ui/trash   (aus Android)
    shell/   StudioShell.kt, TitleBar.kt, NavRail.kt, CommandPalette.kt
    devices/ DeviceStudio.kt, DeviceFrame.kt, DeviceSpecs.kt, VirtualWindow.kt
    dashboard/ Overview.kt
  composeApp/src/jvmMain/composeResources/  (values*/strings.xml, font/, drawable/)
```

## 4. Konzept: Aufbau der App

**Hauptfenster (Desktop):** eigene Titelleiste in Leder mit Ziernaht, links eine schmale **Navigationsleiste** (Navigation Rail), rechts der Inhalt.
Keine geteilte Ansicht: Jede Seite nutzt die volle Breite.

Bereiche in der Navigationsleiste:

| # | Bereich | Inhalt |
|---|---|---|
| 1 | **Übersicht** (Start) | Dashboard: Begrüßung nach Tageszeit, Kacheln (Notizen gesamt, offene Checklisten-Punkte, Favoriten, Archiv, Papierkorb), zuletzt bearbeitet, angeheftete Notizen, Kategorien mit Anzahl, „Neue Notiz“/„Neue Liste“ groß |
| 2 | **Notizen** | Startseite wie in der App: Raster/Liste/Übersicht, Chips (Alle · Notizen · Listen · Kategorien), Suche, Sortieren & Filtern, Drag & Drop |
| 3 | **Favoriten** | wie Tab in der App |
| 4 | **Archiv** | wie in der App |
| 5 | **Geräte-Studio** | die App live im Handy- und Tablet-Rahmen (siehe Abschnitt 5) |
| 6 | **Papierkorb** | wie in der App, 30 Tage |
| 7 | **Einstellungen** | Design hell/dunkel/System, Sprache (9), Kartenansicht, Animationen an/aus, Sicherung & Import, Speicherort öffnen, Info & Lizenzen |

**Editor:** öffnet mit Übergangsanimation als ganze Seite (Zurück mit `Esc`), Lesebreite 760–880 dp zentriert, Formatierungsleiste unten.
Zusätzlich: Rechtsklick → „In neuem Fenster öffnen“ (eigenes, kleines Leder-Fenster für die Notiz, z. B. Einkaufsliste neben dem Browser).

**Desktop-Extras:**
- Tastenkürzel: `Strg+N` neue Notiz, `Strg+L` neue Liste, `Strg+F` Suche, `Strg+K` Befehlspalette, `Strg+S` speichern (Autosave bleibt),
  `Strg+B/I/U` Format, `Entf` in den Papierkorb, `Strg+Z` Rückgängig, `Strg+D` duplizieren, `Strg+1…7` Bereiche, `F11` Vollbild.
- **Befehlspalette** (`Strg+K`): Notizen finden, Befehle ausführen, Bereiche wechseln.
- Rechtsklick-Kontextmenü auf Karten (Anheften, Favorit, Farbe, Kategorie, Duplizieren, Archivieren, Löschen, In neuem Fenster).
- Maus: Hover-Effekte, Mausrad, Drag & Drop mit der Maus (gleiche Logik wie `ReorderableNoteList`).
- Dateien ins Fenster ziehen: `.txt` → neue Notiz, `.json` (Backup aus der Android-App) → Import mit Vorschau.
- System-Tray-Symbol: „Neue Notiz“, „Öffnen“, „Beenden“. Fenstergröße und -position merken.
- **Austausch mit dem Handy ohne Cloud:** Backup-Format v2 der App lesen und schreiben (`BackupFormat.kt`), Duplikaterkennung wie in der App.
  So kommt eine Sicherung vom Handy auf den PC und zurück.
- Automatische lokale Sicherung beim Beenden (letzte 10 behalten) im Datenordner.

## 5. Geräte-Studio (Tablet- und Handy-Abteilung)

Ziel: Dieselbe App **live und bedienbar** in Geräterahmen ansehen, nicht als Bild.

**So funktioniert es technisch:**
- `DeviceFrame(spec, orientation) { content }` zeichnet einen Rahmen (Handy: abgerundet, Kamera-Punkt; Tablet: schmaler Rand)
  und darin eine Fläche mit **fester Größe in dp** (z. B. 411 × 891 dp).
- In dieser Fläche läuft der normale App-Inhalt (`AppRoot` aus der Android-App, angepasst) mit eigenem `WindowLayout`:
  `CompositionLocalProvider(LocalWindowLayout provides WindowLayout.from(widthDp, heightDp))`.
  Dafür `Adaptive.kt` so umbauen, dass das Layout aus einem CompositionLocal kommt statt aus der echten Fenstergröße.
- Skalierung: Der Rahmen wird mit `Modifier.graphicsLayer(scaleX, scaleY)` passend eingepasst (Zoom-Regler 50–150 %, „Einpassen“).
  Die Fläche selbst behält ihre dp-Größe, damit die Layout-Regeln wie auf dem echten Gerät greifen.
- Alle Geräte teilen **dieselbe Datenbank**: Eine Änderung im Handy-Rahmen erscheint sofort im Tablet-Rahmen und im Desktop.

**Gerätevorlagen (`DeviceSpecs.kt`):**

| Name | Größe (dp) | Hinweis |
|---|---|---|
| Handy kompakt | 360 × 780 | kleine Handys |
| Handy | 411 × 891 | Pixel-Größe, wie Layout-Test 2.2 |
| Handy quer | 891 × 411 | Navigation seitlich |
| Tablet hoch | 800 × 1280 | wie Layout-Test 2.2 |
| Tablet quer | 1280 × 800 | Hauptfall für Tablets |
| Großes Tablet quer | 1366 × 1024 | Erweitert ≥ 840 dp |

**Bedienleiste im Geräte-Studio:**
- Gerät wählen (Chips), **Drehen**-Knopf (hoch ↔ quer), Zoom, hell/dunkel nur für diesen Rahmen, Sprache nur für diesen Rahmen
  (zum Prüfen der 9 Sprachen).
- Ansicht **„Nebeneinander“**: Handy und Tablet quer gleichzeitig nebeneinander, beide bedienbar. Das ist ein Vergleich, kein Splitscreen der App.
- **Bildschirmfoto**-Knopf: speichert den Rahmeninhalt als PNG in `Bilder\Notizen & Listen Studio\` (für Store und Werbung; echte App-Oberfläche).
  Wahlweise mit oder ohne Geräterahmen.
- Anzeige der aktiven Layout-Regel: „Kompakt · Hochformat“, „Erweitert · Querformat · Navigation seitlich“.

## 6. Design „Leder & Papier“ für den Desktop

- **Titelleiste:** randloses Fenster (`undecorated = true`), eigene Leiste 44 dp in Leder mit feiner Narbung (`leather_grain.png` aus der App)
  und gestrichelter Goldnaht 6 dp vom Rand. Links App-Logo + „Notizen & Listen“, Mitte Suchfeld (öffnet die Befehlspalette),
  rechts Minimieren/Maximieren/Schließen im Leder-Stil (Schließen-Hover in Beere `#D9443A`). Ziehen über `WindowDraggableArea`,
  Doppelklick maximiert.
- **Navigationsleiste:** 84 dp breit, Leder dunkel `#3E2219`, Symbole hell `#FFF3DC`, aktiver Eintrag mit Gold-Pille und Gold-Symbol.
  „+“ (Neue Notiz) oben als goldener Knopf.
- **Inhalt:** Papierhintergrund `#F8EBD7`, Karten `#FFF8EA` mit 16 dp Ecken, feiner Linie `#EAD8BC` und weichem Schatten.
  Inhaltsbreite maximal 1360 dp, Raster mit adaptiver Kartenbreite (wie in der App: 156/190/216 dp, Desktop zusätzlich 240 dp).
- **Übersicht-Dashboard:** Kopfband in Leder mit Begrüßung (Nunito ExtraBold 32), darunter 5 Kennzahl-Kacheln mit Symbolen
  (Material Symbols wie in der App), danach Bereiche „Angeheftet“, „Zuletzt bearbeitet“, „Kategorien“.
- **Geräte-Studio:** dunkler Leder-Arbeitstisch als Hintergrund (`#2A1710` mit Narbung), Geräte schweben mittig mit weichem Schatten,
  Bedienleiste als Papierkarte oben.
- **Abendmodus:** alle Flächen aus der Night-Palette, Leder bleibt Leder.
- Schrift: Nunito (Überschriften ExtraBold/Black, Text SemiBold/Regular). Mindestgröße 13 sp am Desktop.
- Mauszeiger: Hand auf Karten und Knöpfen, Verschieben-Zeiger beim Ziehen.
- Fokus-Ring für Tastatur (2 dp Gold), alles per Tastatur erreichbar.

## 7. Animationen (genaue Vorgaben)

Alle Animationen zentral in `ui/theme/Motion.kt` (Dauern + Easing). Einstellung „Animationen“ aus → alle Dauern 0.
Standard-Easing: `FastOutSlowInEasing`, Federn: `spring(dampingRatio = 0.8f, stiffness = 380f)`.

| # | Wo | Animation | Dauer |
|---|---|---|---|
| 1 | App-Start | Logo skaliert 0.85 → 1 und blendet ein, Ledernaht „zeichnet“ sich einmal um die Titelleiste | 600 ms |
| 2 | Bereichswechsel | Inhalt gleitet 24 dp von unten + Einblenden; alte Seite blendet aus | 280 ms |
| 3 | Navigationsleiste | Gold-Pille gleitet zum neuen Eintrag (`animateDpAsState` auf y), Symbol springt leicht (Feder) | 250 ms |
| 4 | Karten-Liste | Karten erscheinen gestaffelt (je 30 ms Verzögerung, max. 12 Karten), `animateItem` beim Umsortieren | 220 ms |
| 5 | Karten-Hover | Anheben 2 → 8 dp Schatten, Skalierung 1.0 → 1.015 | 150 ms |
| 6 | Karte öffnen | **Shared Element**: Karte wächst zum Editor (Titel und Hintergrund wandern mit, `SharedTransitionLayout`) | 380 ms |
| 7 | Drag & Drop | gezogene Karte schwebt (Schatten 16 dp, leichte Drehung 1,5°), Platzhalter mit gestrichelter Goldlinie, Nachbarn weichen per Feder aus | Feder |
| 8 | Checkliste abhaken | Häkchen zeichnet sich (Pfad-Animation), Text wird durchgestrichen (Linie wächst von links), Eintrag rutscht nach unten | 300 ms |
| 9 | Löschen | Karte schrumpft + blendet aus, Snackbar „Rückgängig“ gleitet aus Leder-Unterkante | 250 ms |
| 10 | Favorit | Stern füllt sich Gold mit kurzem Funkeln (3 Punkte) | 400 ms |
| 11 | Hell ↔ Dunkel | kreisförmige Enthüllung vom Schalter aus (`clip` mit wachsendem Kreis) | 500 ms |
| 12 | Dashboard-Kacheln | Zahlen zählen hoch (0 → Wert), Kacheln blenden gestaffelt ein | 700 ms |
| 13 | Befehlspalette | von oben einfliegen + Hintergrund abdunkeln | 180 ms |
| 14 | Geräte-Studio: Gerät wechseln | Rahmen morpht Breite/Höhe/Ecken (`animateDpAsState`), Inhalt blendet über | 450 ms |
| 15 | Geräte-Studio: Drehen | Rahmen dreht 90° (`graphicsLayer.rotationZ`), danach Layout-Wechsel hoch ↔ quer; Inhalt blendet neu ein | 500 ms |
| 16 | Geräte-Studio: Nebeneinander | Handy gleitet von links, Tablet von rechts herein | 400 ms |
| 17 | Bildschirmfoto | kurzer weißer Blitz über dem Rahmen + Miniatur fliegt zur Bildschirmfoto-Leiste | 350 ms |
| 18 | Leer-Zustände | Notizbuch-Symbol „atmet“ langsam (Skalierung 1 ↔ 1.03) | 2,4 s Schleife |
| 19 | Import | Fortschrittsband in Gold, danach Haken-Animation | – |

## 8. Umsetzung in Phasen (nach jeder Phase stoppen und berichten)

**Phase 1 – Projekt & Daten**
- Neuer Ordner, Git-Repo, Gradle mit Compose Multiplatform Desktop, Room KMP, DataStore.
- `data/*`, Entities, DAOs, Repository, Filter, Checkliste, RichText, Backup kopieren. Android-Abhängigkeiten ersetzen.
- Datenbank im Datenordner, Schema exportieren (Version 5 wie Android, damit Backups passen).
- Tests: Repository-, Filter-, Checklisten- und Backup-Tests aus `app/src/test` übernehmen und grün machen.
- ✔ Abnahme: `gradlew :composeApp:jvmTest` grün, leeres Fenster startet.

**Phase 2 – Design & Grundgerüst**
- Theme, Farben, Nunito, `Leather.kt`, eigene Titelleiste, Navigationsleiste, Bereiche als leere Seiten, Animationen 2 und 3.
- Sprachen: `strings.xml` (9 Sprachen) nach `composeResources`, Sprachwahl in den Einstellungen.
- ✔ Abnahme: Fenster in Leder & Papier, Bereichswechsel animiert, Sprache umschaltbar.

**Phase 3 – Notizen wie in der App**
- Home (Raster/Liste/Übersicht, Chips, Suche, Sortieren & Filtern), Karten, Kontextmenü, Drag & Drop mit Maus,
  Editor (Formatierung, Checkliste + Text, Autosave), Favoriten, Archiv, Papierkorb, Kategorien, Einstellungen.
- Animationen 4–11.
- ✔ Abnahme: alle Funktionen der Android-Version 2.3 außer Werbung/Kauf/App-Icon-Wechsel laufen.

**Phase 4 – Desktop-Extras**
- Tastenkürzel, Befehlspalette, Notiz in eigenem Fenster, Tray, Datei-Drag-&-Drop, automatische Sicherung, Fensterzustand merken,
  Backup-Import/-Export kompatibel mit der Android-App (mit einer echten Sicherung testen, die Marcel aus der App exportiert).
- Übersicht-Dashboard mit Animation 12 und 13.

**Phase 5 – Geräte-Studio**
- `Adaptive.kt` auf CompositionLocal umstellen, `DeviceFrame`, `DeviceSpecs`, Bedienleiste, Drehen, Nebeneinander, Zoom,
  Sprache/Design je Rahmen, Bildschirmfoto als PNG. Animationen 14–17.
- ✔ Abnahme: Handy hoch zeigt untere Navigation, Tablet quer zeigt seitliche Navigation und mehr Spalten (gleiche Regeln wie App 2.2).
  Änderung in einem Rahmen erscheint sofort in den anderen.

**Phase 6 – Feinschliff & Paket**
- Leer-Zustände, Fokus/Tastatur, Animationen aus, Abendmodus, Fehlermeldungen (z. B. defekte Backup-Datei).
- Icon `.ico`, `packageMsi` und portable ZIP. Version „1.0.0“ der Studio-App.
- `README.md` im neuen Projekt: Starten, Bauen, Datenordner, Austausch mit dem Handy, bekannte Grenzen.
- ✔ Abnahme: MSI installiert und startet auf Windows 10/11, Daten bleiben nach Update der Studio-App erhalten.

## 9. Tests und Prüfung

- Unit-Tests (übernommen + neu): Repository, Filter/Sortierung, Checkliste, RichText, Backup v1/v2 Roundtrip, Migration (falls nötig).
- Compose-UI-Tests (Desktop `runComposeUiTest`): Notiz anlegen, suchen, abhaken, löschen/wiederherstellen, Bereichswechsel,
  Geräte-Studio: Gerät wechseln und drehen → richtige `WindowLayout`-Klasse.
- Bildschirmfotos jeder Seite in hell/dunkel und für alle 6 Gerätevorlagen erzeugen und ansehen (nichts abgeschnitten, kein Text überlappt).
- Leistung: 1.000 Notizen flüssig scrollen und ziehen.
- Am Ende kurzer Bericht: was gebaut ist, Testergebnisse, Bildschirmfotos, offene Punkte.

## 10. Bewusst nicht enthalten (später möglich)

- Microsoft Store (MSIX) – erst nach Marcels Entscheidung, nichts anlegen.
- Synchronisierung zwischen Handy und PC über Cloud oder WLAN – nur Backup-Datei.
- Werbung oder Käufe unter Windows.
- Gemeinsames KMP-Modul mit der Android-App – erst wenn das Studio stabil läuft und Marcel es will (Android-Projekt dafür sichern).
