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
| Tests | Android 74 grün, Windows 61 grün. Neu sind Tests für die Vorlagen (hin und zurück), die Vorlagen-Grenze und die Tage-Serie.<br>Bildschirmfotos 21–23 (Herbstlaub/Notizbuch, AMOLED/Statistik, Hoher Kontrast) wurden angesehen. |
| Nicht getestet | Käufe (dafür müssen die Produkte in den Stores angelegt sein) und die Installation unter Windows bzw. auf dem Handy |

## Nachtrag 17.11.2026: Testphase
- Beim Kauf von Premium Plus nimmt die App jetzt zuerst das Gratis-Angebot `trial-7-days`, wenn Google Play es dem Nutzer anbietet. Vorher wäre der Basisplan ohne Testphase gewählt worden.
- Im Shop steht dann über den Abo-Knöpfen „7 Tage gratis testen“ (9 Sprachen, Text `shop_trial_days`). Wer schon getestet hat, sieht den Hinweis nicht, weil Play ihm das Angebot nicht mehr liefert.
- Tests: Android 74 grün.

## Store-Produkte (angelegt am 17.11.2026)
| Store | Produkt | Typ | Preis (DE) | Testphase | Status |
|---|---|---|---|---|---|
| Google Play | `personalization_pack` | Einmalkauf | 4,99 € | – | aktiv |
| Google Play | `premium_plus` / Basisplan `monthly` | Abo | 1,99 €/Monat | Angebot `trial-7-days` | aktiv |
| Google Play | `premium_plus` / Basisplan `yearly` | Abo | 14,99 €/Jahr | Angebot `trial-7-days` | aktiv |
| Microsoft Store | `personalization_pack` (9NMXHN2J1NV6) | Dauerhaft | 4,99 € | – | Entwurf fertig |
| Microsoft Store | `premium_plus_monthly` (9P6F48PVDQH3) | Abo, 1 Monat | 1,99 € | 1 Woche | Entwurf fertig |
| Microsoft Store | `premium_plus_yearly` (9P570D5M5H1S) | Abo, 1 Jahr | 14,99 € | 1 Woche | Entwurf fertig |

In Play sind die Preise netto eingetragen (4,19 / 1,67 / 12,60 €), damit Kunden in Deutschland 4,99 / 1,99 / 14,99 € zahlen.
Die Microsoft-Add-ons lassen sich erst einreichen, wenn die App selbst veröffentlicht ist und die Steuer- und Auszahlungsdaten im Partner Center hinterlegt sind.

## Release
1. Google Play: signiertes AAB 2.6 (versionCode 9) hochladen. Die Produkte sind schon aktiv.
2. Partner Center: Steuer- und Auszahlungsdaten eintragen, dann die App-Einreichung mit `Notizen-und-Listen-Store-1.3.0.0.msix` und den Texten aus `Store-Texte.md`.
3. Nach der Veröffentlichung der App die drei Add-ons „An den Store übermitteln“.

Wenn 2.5 noch nicht veröffentlicht ist, reicht es, direkt 2.6 hochzuladen.
