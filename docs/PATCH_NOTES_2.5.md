# Patch Notes und Release-Checkliste – Notizen & Listen 2.5 / Windows 1.2.0 „Premium-Designs“

**Android:** Version 2.5 (versionCode 8) · **Windows (Microsoft Store):** 1.2.0 (MSIX 1.2.0.0, Store-ID 9PHGVPZMM3HD)
Stand: 17.11.2026 · nichts davon ist veröffentlicht.

## Was neu ist (DE)
- **Design-Paket** (Einmalkauf): Schaltet alle Premium-Designs und Papiermuster dauerhaft frei.
- **Premium Plus** (Abo, monatlich oder jährlich): Schaltet alle Premium-Designs und Papiermuster frei, solange das Abo läuft. Auf Android ist außerdem die Werbung aus.
- **Design-Shop:** neuer Bereich „Premium“.
  - Zwei Karten mit Kaufknöpfen. Die Preise kommen immer aus dem Store.
  - Ist ein Produkt noch nicht angelegt, steht „Bald verfügbar“ auf dem Knopf.
  - „Freischalten“ an jeder Premium-Karte kauft das Design-Paket.
  - Dazu „Käufe wiederherstellen“ und „Abo verwalten“.
- Hinweis zum Abo im Shop: verlängert sich automatisch, jederzeit kündbar.
- **Werbefrei (`remove_ads`) bleibt unverändert** und schaltet keine Designs frei.
- **Windows:** Käufe laufen über den Microsoft Store, in Version 1.2.0 aus dem Store. Der portable Ordner zeigt „Käufe nur in der Store-Version“.

## What's new (EN)
- **Design pack** (one-time): all premium designs and paper styles forever.
- **Premium Plus** (monthly or yearly subscription): all premium designs and papers while subscribed. On Android it also removes ads.
- **Design shop:** new “Premium” section with prices from the store, “Restore purchases” and “Manage subscription”.
- Ad-free (`remove_ads`) is unchanged.

## Technik
| Bereich | Umsetzung |
|---|---|
| Android | Der `BillingManager` hat zwei neue Produkte:<br>• `personalization_pack` (INAPP)<br>• `premium_plus` (SUBS) mit den Basisplänen `monthly` und `yearly`<br><br>Käufe werden bestätigt (acknowledge), und beim Start wird bei Google Play abgefragt, ob sie noch gelten. Der Zwischenspeicher liegt im DataStore (`design_pack_entitlement_cache`, `premium_plus_entitlement_cache`) und wird widerrufen, wenn Google Play nichts mehr meldet, z. B. nach Ablauf des Abos oder einer Erstattung. |
| Windows | `MyNotesStore.exe` (C#, .NET Framework 4.8, Windows.Services.Store) mit den Befehlen `licenses`, `products` und `buy`. In Kotlin heißt der Teil `store/StoreBridge.kt`.<br><br>Die Add-on-Produkt-IDs müssen so heißen: `personalization_pack`, `premium_plus_monthly`, `premium_plus_yearly`. |
| MSIX | Gebaut wird mit `tools/build_msix.sh` (makemsix). Das Paket enthält den Launcher, `lib\`, die Laufzeit Temurin 21 in `runtime\`, den Store-Helfer und die Logos.<br><br>Es ist nicht signiert, das übernimmt der Store. Das Werbe-Studio ist nicht enthalten. |
| Texte | 19 neue Texte in 9 Sprachen |
| Tests | Android 71 grün: ein Oberflächentest war beim ersten Lauf zeitabhängig rot und ging beim zweiten Lauf durch. `assembleDebug` erfolgreich.<br><br>Windows 59 grün. Das MSIX wurde mit makemsix entpackt und geprüft. |
| **Nicht getestet** | • echte Käufe (dafür müssen die Produkte in Play Console und Partner Center angelegt sein)<br>• Installation des MSIX unter Windows |

## Release-Checkliste – was du tun musst

### Google Play (Android 2.5)
1. In der Play Console unter **Monetarisieren → Produkte** zwei Produkte anlegen und die Preise selbst festlegen:
   - **Einmalkauf** mit der Produkt-ID `personalization_pack`, Name „Design-Paket“, Status aktiv.
   - **Abo** mit der Produkt-ID `premium_plus`, Name „Premium Plus“. Es braucht zwei Basispläne mit automatischer Verlängerung: `monthly` (1 Monat) und `yearly` (1 Jahr).
2. In Android Studio das signierte AAB bauen: **Build → Generate Signed App Bundle → release**. versionCode 8 ist schon gesetzt.
3. Zuerst in den **internen Test** hochladen, dich als Tester eintragen und auf dem Handy testen:
   - beide Käufe
   - wiederherstellen
   - Abo kündigen: Danach müssen die Premium-Designs nach Ablauf wieder gesperrt sein.
4. Versionshinweise für 9 Sprachen stehen in `docs/PLAY_STORE.md` unter 2.5.
5. In der Play Console „Datensicherheit“ prüfen. Käufe laufen über Google Play, neue Daten erhebt die App nicht.

### Microsoft Store (Windows 1.2.0)
1. Im Partner Center unter **Notizen & Listen → Add-ons** drei Add-ons mit genau diesen Produkt-IDs anlegen und die Preise festlegen: `personalization_pack` (dauerhaft), `premium_plus_monthly` und `premium_plus_yearly` (Abonnement).
2. Eine neue Einreichung anlegen:
   - Preis: kostenlos
   - Eigenschaften: Kategorie Produktivität, Datenschutz-URL
   - Altersfreigabe: Fragebogen ausfüllen
   - Paket hochladen: `Microsoft Store\Notizen-und-Listen-Store-1.2.0.0.msix`
   - Store-Texte DE und EN aus `Store-Texte.md`, dazu die Screenshots
   - Begründung für runFullTrust
3. Einreichen. Ich frage vor jedem Speichern oder Einreichen nach.
4. Nach der Freigabe die App aus dem Store installieren und die Add-ons testen. Mit einem Test-Konto geht das kostenlos über einen Aktionscode.

## Hinweise
- Die Store-Version speichert ihre Daten getrennt von der portablen Version, weil Windows die Daten von Store-Apps in einem eigenen Paket-Ordner ablegt. Übertragen geht über **Einstellungen → Sicherung exportieren/importieren**.
- Die Datenschutzerklärung (`My-Notes-privacy`) beschreibt die Android-App. Für den Store reicht sie, weil die Windows-App keine Daten sammelt. Ein kurzer Absatz zur Windows-Version wäre sauberer.
