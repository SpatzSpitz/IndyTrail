# IndyTrail

Ein Android‑Abenteuer‑Trail im Stil einer uralten, hyper‑fortgeschrittenen Zivilisation.  
Spieler scannen QR‑„Glyphen“, kalibrieren ein antikes Übersetzungsgerät, schalten Tools frei und lösen Rätsel – u. a. mit dem **Lumen Emitter** (geregelte Blitz‑Sequenzen).

---

## Screenshots
> Lege Bilder in `docs/screenshots/` ab und passe die Pfade an.
>
> ![Menu](docs/screenshots/menu.png)
> ![Scanner](docs/screenshots/scanner.png)
> ![Quest](docs/screenshots/quest.png)
> ![Emitter](docs/screenshots/emitter.png)

---

## Aktuelle Features
- 📷 **QR‑Scanner** (CameraX + ML Kit Barcode)
- 🔓 **Translator Core** (Scanner) – immer verfügbar
- 🔦 **Lumen Emitter** (Torch‑Steuerung, 1–9‑Tasten, Fortschritts‑LEDs, Runen‑Hinweise)
- 🧩 **Quest Q1 (Glyph‑Suche)**: 4 QR‑Codes → Emitter‑Sequenz
- 📈 **Kalibrierungsleiste** + Freischaltlogik (z. B. 20 % → Lumen)
- 🏅 **Achievement‑Overlay** (Logo, animiertes Banner)
- 🎨 **Theme**: Cavern‑Gradient, Gold/Cyan, Runen‑Zierelemente

### Roadmap
- 🗺️ Pathfinder Protocol (Navigation)
- 📚 Codex Archive (Notizen/Questlog)
- 🔊 SFX/Jingle‑System (für Achievements & UI)
- 💾 Persistenz (DataStore) für Quest‑ und Kalibrierungs‑State
- 🧠 Rätselsammlung & Content‑Pflege (offline‑fähig)

---

## Tech‑Stack
- **Kotlin**, **Jetpack Compose** (Material 3)
- **CameraX** (core/camera2/lifecycle/view), **ML Kit Barcode**
- Min SDK **24**, Target/Compile SDK **36**
- Komplett **offline** nutzbar (keine Netzabhängigkeit im Spiel)

---

## Setup & Build
1. Projekt in **Android Studio** (mit Android SDK 36) öffnen.
2. `local.properties` muss deinen SDK‑Pfad enthalten (wird normalerweise automatisch gesetzt).
3. Auf **echtem Gerät** starten (empfohlen wegen Kamera/Blitz).  
4. Berechtigung: `CAMERA` (im Manifest vorhanden).

---

## QR‑Schema (Routen)
Die App versteht interne Routen via `trail://…`‑URIs:

**Station öffnen**
trail://station/S1

**Quest öffnen**
trail://quest/Q1

**Glyph für Quest‑Slot setzen** *(Slots sind 1‑basiert)*:
trail://quest/Q1/slot/1/glyph/PSI
trail://quest/Q1/slot/2/glyph/R
trail://quest/Q1/slot/3/glyph/F
trail://quest/Q1/slot/4/glyph/X


> Diese Beispiel‑Glyphen sind aktuell auf die Emitter‑Tasten **1‑2‑3‑4** gemappt.  
> Das Mapping ist zentral in `data/GlyphCatalog.kt` definiert und beliebig anpassbar.

---

## App‑Navigation (Kurzüberblick)
- **MenuScreen**: Geräte‑Konsole, zeigt Kalibrierung & freigeschaltete Tools.
- **ScanScreen**: QR‑Erkennung (CameraX + ML Kit) und Routing über `trail://…`.
- **QuestScreen (Q1)**: 4 Slots, „Scan Glyphs“, Debug‑Button „Open Lumen Emitter“.
 - **LumenEmitterScreen**: 3×3‑Tasten mit Glyphen, LEDs für Fortschritt und kamerabasierte Blitz‑Sequenzen.

---

## Projektstruktur (Auszug)
IndyTrail/
├─ app/
│ ├─ src/main/java/com/example/indytrail/
│ │ ├─ core/ # ScanRoute, parseScanUri
│ │ ├─ data/
│ │ │ ├─ Stations.kt # Beispielstationen
│ │ │ ├─ QuestStore.kt # In‑Memory‑Fortschritt pro Quest
│ │ │ └─ GlyphCatalog.kt # Code↔Taste, Symbol‑Mapping
│ │ └─ ui/
│ │ ├─ MenuScreen.kt
│ │ ├─ ScanScreen.kt
│ │ ├─ QuestScreen.kt
│ │ ├─ LumenEmitterScreen.kt
│ │ ├─ AchievementOverlay.kt
│ │ └─ theme/ # Farben, Typografie
│ └─ src/main/res/
│ ├─ drawable-nodpi/indy_trail_logo.png
│ └─ ...
└─ build.gradle.kts, settings.gradle.kts, gradle.properties, ...


---

## Glyphen & Emitter‑Mapping anpassen
`app/src/main/java/com/example/indytrail/data/GlyphCatalog.kt`

- **`codeToSymbol`**: bestimmt, **wie** ein Code im UI angezeigt wird (Rune/Zeichen).
- **`keyToCode`**: bestimmt, **welche Taste (1–9)** zu **welchem Code** gehört.
- Hilfsfunktionen:
  - `symbol(code)` → sichtbares Zeichen
  - `codeFromKey(key)` → Code aus Tasten‑Eingabe
  - `keyFromCode(code)` → Taste für Code

Beispiel:
```kotlin
private val keyToCode = mapOf(
  "1" to "PSI",
  "2" to "R",
  "3" to "F",
  "4" to "X",
  "5" to "U",
  "6" to "T",
  "7" to "S",
  "8" to "E",
  "9" to "B",
)
