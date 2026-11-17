# Patch Notes – Notizen & Listen 2.6 / Windows-Studio 1.3.0 „Designs & eigene Vorlagen“

**Android:** 2.6 (versionCode 9) · **Windows:** 1.3.0 (MSIX 1.3.0.0) · Stand 17.11.2026 · nicht veröffentlicht
Baut auf 2.5 / 1.2.0 auf (Design-Paket und Premium Plus). Store-Texte in 9 Sprachen: `docs/PLAY_STORE.md`.

## Deutsch

### Neu
- **3 neue Designs**, jeweils hell und dunkel:
  - **Hoher Kontrast** (kostenlos): Schwarz und Weiß mit kräftigem Gelb, gut lesbar.
  - **AMOLED Schwarz** (Premium): echtes Schwarz im Dunkelmodus, Akzent ist das Markengelb #FDEE51.
  - **Herbstlaub** (Premium): Rostbraun und Orange.
- **2 neue Papiere** (Premium):
  - **Notizbuch**: liniert, mit Randlinie und gestanzten Löchern.
  - **Millimeterpapier**: feines Raster, jede 5. Linie kräftiger.
- **„Neu“-Abzeichen** im Design-Shop für die neuen Designs und Papiere.
- **Eigene Vorlagen:**
  - Im Editor über ⋮ → „Als Vorlage speichern“. Die Vorlage übernimmt Titel, Text und Checklisten-Einträge ohne Haken.
  - Die Vorlagen erscheinen unter Menü → Vorlagen → „Meine Vorlagen“ und lassen sich dort auch löschen.
  - 3 sind kostenlos, mit **Premium Plus** unbegrenzt.
- **Schriftgröße im Editor:** Einstellungen → Darstellung, vier Stufen (A−, A, A+, A++).
- **Konfetti:** Sind alle Punkte einer Checkliste abgehakt, regnet es kurz Konfetti und es erscheint „Alles erledigt! 🎉“. Mit „Animationen aus“ kommt nur der Text.
- **Statistik:** neue Kachel „Tage in Folge“. Sie zählt die Tage, an denen du eine Notiz angelegt oder bearbeitet hast, bis heute oder gestern.

### Premium (unverändert fair)
- **Design-Paket** (einmalig): alle Premium-Designs und Papiere dauerhaft, die neuen eingeschlossen.
- **Premium Plus** (Abo): alle Premium-Designs und Papiere sowie unbegrenzt eigene Vorlagen. Auf Android ist außerdem die Werbung aus.
- Werbefrei (`remove_ads`) bleibt unverändert.

## English
- New designs: **High contrast** (free), **AMOLED black** and **Autumn leaves** (premium).
- New papers: **Notebook** and **Graph paper** (premium).
- “New” badge in the design shop.
- **Own templates:** ⋮ → “Save as template”. 3 are free, unlimited with Premium Plus.
- **Editor font size:** 4 steps.
- **Confetti** when a checklist is all done.
- **Statistics:** new tile “Days in a row”.

## Technik
| Bereich | Umsetzung |
|---|---|
| Designs/Papier | `Skin.CONTRAST`, `AMOLED`, `AUTUMN` sowie `PaperPattern.NOTEBOOK` und `MILLIMETER`, jeweils mit dem Feld `isNew`. |
| Vorlagen | `CustomTemplates` (JSON in den Einstellungen, Schlüssel `custom_templates`) und `Entitlements.canSaveTemplate`.<br>Die Berechtigung `plus` setzt Android aus dem Abo und Windows aus der Store-Lizenz. |
| Schriftgröße | Einstellung `editor_font_scale`. Der Editor-Inhalt nutzt dafür einen eigenen `LocalDensity.fontScale`. |
| Konfetti | `ui/components/ConfettiBurst` mit 70 Schnipseln, ca. 1,8 s. Es wird nur ausgelöst, wenn die Liste von „offen“ zu „alles erledigt“ wechselt. |
| Texte | 17 neue Texte × 9 Sprachen |
| Tests | Android 73 grün, Windows 61 grün. Neu sind Tests für die Vorlagen (hin und zurück), die Vorlagen-Grenze und die Tage-Serie.<br>Bildschirmfotos 21–23 (Herbstlaub/Notizbuch, AMOLED/Statistik, Hoher Kontrast) wurden angesehen. |
| Nicht getestet | Käufe (dafür müssen die Produkte in den Stores angelegt sein) und die Installation unter Windows bzw. auf dem Handy |

## Release
Wie in `PATCH_NOTES_2.5.md` beschrieben: Produkte anlegen, signiertes AAB (versionCode 9) bzw. MSIX `Notizen-und-Listen-Store-1.3.0.0.msix` hochladen.
Wenn 2.5 noch nicht veröffentlicht ist, reicht es, direkt 2.6 hochzuladen.
