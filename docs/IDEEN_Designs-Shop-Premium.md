# Notizen & Listen – Ideenliste: Designs, Shop, Premium, Animationen

Stand 17.11.2026 (Status bis Version 2.6 / Windows 1.3.0) · gilt für **Android (dev.flondy.mynotes)** und das **Windows-Studio**.
Status: ✅ in 2.4 gebaut · 🟡 vorbereitet (fehlt nur Store/Preis) · ⏳ später

Grundsätze: kein Abo-Zwang, keine erfundenen Preise, `remove_ads` bleibt genau, was es ist. Daten bleiben lokal.

---

## 1. Übersicht – was in 2.4 neu ist

| Bereich | Inhalt | Android | Windows |
|---|---|---|---|
| Designs | 7 Designs, hell + Abend | ✅ | ✅ |
| Papier | glatt, liniert, kariert, Punktraster | ✅ | ✅ |
| Design-Shop | Live-Vorschau, 30-s-Probe, „Freischalten – bald“ | ✅ | ✅ (Navigationsleiste + Strg+9) |
| Vorlagen | 6 Vorlagen in 9 Sprachen | ✅ | ✅ |
| Statistik | Kennzahlen, Fortschritt, Kategorien, 14 Tage | ✅ | ✅ (Strg+8) |
| Freischaltung | `Entitlements`, Rückfall auf kostenlos | 🟡 | 🟡 |

## 2. Designs (Skins)

| # | Design | Typ | Status |
|---|---|---|---|
| 1 | Leder & Papier (Standard) | kostenlos | ✅ |
| 2 | Graphit (kühles Grau, Gelb) | kostenlos | ✅ |
| 3 | Mitternacht (Nachtblau, Himmelblau) | Premium | ✅ Vorschau |
| 4 | Waldhütte (Tannengrün, Honig) | Premium | ✅ Vorschau |
| 5 | Pergament (Sepia, Siegelrot) | Premium | ✅ Vorschau |
| 6 | Kirschblüte (Beere, Rosa) | Premium | ✅ Vorschau |
| 7 | Ozean (Petrol, Türkis) | Premium | ✅ Vorschau |
| 8 | Saisonal: Herbst/Winter/Frühling, automatisch nach Datum | Premium | ⏳ |
| 9 | Hoher Kontrast (Barrierefreiheit) | **kostenlos** | ✅ 2.6 |
| 10 | AMOLED-Schwarz für den Abend | Premium | ✅ 2.6 |
| 11 | Eigene Farbe (Farbrad, Vorschau live) | Premium | ⏳ |
| 12 | Material You (Systemfarben ab Android 12) | kostenlos | ⏳ |

## 3. Papier & Optik

| Idee | Status |
|---|---|
| Papiermuster im Editor (liniert mit Randlinie, kariert, Punktraster) | ✅ |
| Muster auch auf Notizkarten der Startseite (dezent) | ⏳ |
| Notizbuch-Papier mit Lochrand | ✅ 2.6 (dazu Millimeterpapier) |
| Washi-Tape/Büroklammer-Sticker für angeheftete Notizen | ⏳ |
| Handschrift-Schrift für Titel (lizenzfreie Schrift, z. B. Caveat) | ⏳ |
| Schriftgröße im Editor (S/M/L) | ✅ 2.6 (4 Stufen) |
| Eigene Kartenfarben-Paletten je Design | ⏳ |
| App-Symbole passend zu den Designs (Activity-Aliase gibt es schon) | ⏳ |

## 4. Shop & Vorschau

| Idee | Status |
|---|---|
| Shop-Seite mit Abschnitten Designs / Papier / Vorlagen | ✅ |
| Mini-Vorschau je Design (Leder-Kopf, Ziernaht, Karten, Akzent-Knopf) | ✅ |
| „Vorschau 30 s“: ganze App wechselt live, Banner mit Countdown + „Beenden“, endet von selbst | ✅ |
| „Kostenlos“/„Premium“-Abzeichen mit wanderndem Glanz | ✅ |
| Gesperrt-Schloss, deaktivierter Knopf „Freischalten – bald“, Hinweis „nichts wird dauerhaft geändert“ | ✅ |
| Einstieg: Menü → Design-Shop, Einstellungen → Design & Papier, Windows: Navigationsleiste/Befehlspalette | ✅ |
| Vorher/Nachher-Schieber im Shop | ⏳ |
| Paket-Karte „Alle Designs“ mit Preis **aus dem Store** (`ProductDetails.formattedPrice`) | 🟡 |
| „Kauf wiederherstellen“ auch im Shop | ✅ 2.5 |
| Neu-Markierung für neue Designs (Version merken) | ✅ 2.6 |

## 5. Monetarisierung (nur Vorschläge – nichts angelegt)

| Idee | Hinweis | Status |
|---|---|---|
| **`personalization_pack`** – Einmalkauf: alle Premium-Designs + Papiermuster | in der Play Console anlegen, Preis dort festlegen; bei Kauf `unlocked = {"all"}` | 🟡 |
| `remove_ads` unverändert lassen | schaltet keine Designs frei | ✅ |
| Bündel „Werbefrei + Designs“ | eigenes Produkt, nie `remove_ads` umdeuten | ⏳ |
| Einzelne Designs als kleine Käufe (`skin_<name>`) | `Entitlements` kennt schon `skin:<NAME>` | 🟡 |
| Saisonale Designs als Zusatz-Paket | ⏳ |
| Rewarded Ad: Premium-Design 24 h testen | nur mit Einwilligung (UMP), freiwillig | ⏳ |
| Keine Abos, keine Pay-Walls vor Grundfunktionen | Grundsatz | ✅ |

## 6. Neue Funktionen

| Idee | Status |
|---|---|
| Vorlagen: Einkauf, Besprechung, Packliste, Wochenplan, Rezept, Tagebuch | ✅ |
| Eigene Vorlagen speichern („Als Vorlage sichern“) | ✅ 2.6 (3 kostenlos, unbegrenzt mit Premium Plus) |
| Statistik: Notizen, Checklisten, erledigt, Wörter, Fortschritt, Kategorien, 14 Tage | ✅ |
| Serien/Streak („5 Tage in Folge notiert“) – freundlich, ohne Druck | ✅ 2.6 |
| Erinnerungen an Notizen (Benachrichtigung) | ⏳ |
| Widget: Checkliste auf dem Startbildschirm | ⏳ |
| Wiederkehrende Checklisten (wöchentlich zurücksetzen) | ⏳ |
| Notiz sperren (Fingerabdruck) | ⏳ |
| Bilder/Fotos in Notizen | ⏳ |
| Sprachnotiz → Text | ⏳ |
| PDF-Export einer Notiz | ⏳ |
| Windows: Notiz in eigenem Fenster „immer im Vordergrund“ (Sticky Note) | ⏳ |

## 7. Animationen

| Animation | Status |
|---|---|
| Designwechsel: alle Farben blenden weich über (inkl. Leder, Karten, Linien) | ✅ |
| Shop-Karten erscheinen gestaffelt, leicht skaliert | ✅ |
| Auswahlrahmen wächst/färbt sich animiert | ✅ |
| Premium-Glanz läuft über das Abzeichen | ✅ |
| Vorschau-Banner klappt auf, Countdown-Balken schrumpft | ✅ |
| Statistik: Zahlen zählen hoch, Balken wachsen, Ring zeichnet sich | ✅ |
| „Animationen aus“ in den Einstellungen schaltet alles ab | ✅ |
| Konfetti, wenn eine Checkliste komplett erledigt ist | ✅ 2.6 |
| Seite umblättern beim Öffnen einer Notiz | ⏳ |
| Häkchen zeichnet sich beim Abhaken | ⏳ |

## 8. Verbesserungen

| Idee | Status |
|---|---|
| Gespeicherte Premium-Wahl ohne Freischaltung fällt sicher auf kostenlos zurück | ✅ |
| Test-Schalter „alle Premium-Inhalte“ nur in Debug-Builds | ✅ |
| Werbe-Studio: Beispieldaten zeigen alle Designs für Werbebilder | ✅ |
| Tests für Freischaltung, Vorschau, Vorlagen (9 Sprachen), Statistik | ✅ |
| Shop/Statistik auch auf Tablets im Raster (adaptive Spalten) | ✅ |
| Vorlagen-Texte vom Nutzer bearbeitbar | ⏳ |
| Statistik exportieren (Bild teilen) | ⏳ |

## 9. Was Marcel noch tun muss

1. In Android Studio auf dem Handy testen (Shop, Vorschau, Vorlagen, Statistik, Designs hell/dunkel).
2. Entscheiden, ob Premium-Designs verkauft werden sollen → dann Produkt (z. B. `personalization_pack`) in der Play Console anlegen und Preis festlegen. Danach den Kauf im `BillingManager` anschließen (ca. 1 Stunde Arbeit).
3. Bis dahin ist 2.4 voll nutzbar: kostenlose Designs/Papiere, Vorlagen und Statistik sind fertig; Premium lässt sich nur ansehen.


## 10. Neu seit 2.5 / 2.6 (17.11.2026)
| Idee | Status |
|---|---|
| Design-Paket `personalization_pack` (Einmalkauf) – Android + Microsoft Store | ✅ 2.5 eingebaut · Produkt in Play Console/Partner Center anlegen |
| Premium Plus `premium_plus` (Abo monatlich/jährlich; Windows: `premium_plus_monthly`/`_yearly`) | ✅ 2.5 eingebaut · Produkte anlegen |
| Microsoft Store: MSIX mit Store-Käufen (Helfer `MyNotesStore.exe`) | ✅ 1.2.0 · Einreichung offen |
| Weitere Designs: Herbstlaub (Premium) | ✅ 2.6 |
| Saisonale Designs automatisch nach Datum | ⏳ |
| Material You / eigene Farbe | ⏳ |
| Vorher/Nachher-Schieber im Shop | ⏳ |
| Erinnerungen, Widget, wiederkehrende Checklisten | ⏳ (nächster großer Funktionsblock) |
