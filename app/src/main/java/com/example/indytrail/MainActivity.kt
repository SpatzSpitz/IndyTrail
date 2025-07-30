package com.example.indytrail

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.example.indytrail.core.ScanRoute
import com.example.indytrail.core.parseScanUri
import com.example.indytrail.ui.theme.IndyTrailTheme

class MainActivity : ComponentActivity() {

    private fun stationIdFrom(text: String): String? {
        val prefix = "trail://station/"
        return if (text.startsWith(prefix)) text.removePrefix(prefix) else null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            IndyTrailTheme {

                // -------- Quest‑Store --------
                val questStore = remember { com.example.indytrail.data.QuestStore() }
                var showEmitter by remember { mutableStateOf(false) }
                var emitterExpected by remember { mutableStateOf(listOf("1","2","3","4")) } // Test-Sequenz
                var emitterHintGlyphs by remember { mutableStateOf(listOf<String?>()) }
                var showQuest by remember { mutableStateOf(false) }
                var currentQuestId by remember { mutableStateOf("Q1") }   // wird aus QR gesetzt
                var questTick by remember { mutableStateOf(0) }
                // -------- Overlay‑State (nur Grafik, kein Sound) --------
                var overlayVisible by remember { mutableStateOf(false) }
                var overlayTitle by remember { mutableStateOf("") }
                var overlaySubtitle by remember { mutableStateOf("") }
                fun showOverlay(t: String, s: String) {
                    overlayTitle = t
                    overlaySubtitle = s
                    overlayVisible = true
                }

                // Anzahl gefundener Glyphen (Q1)
                // wenn du eine Variable willst:
                val q1Found = questStore.foundCount("Q1")


                // -------- Kalibrierung & Unlocks --------
                var calibration by rememberSaveable { mutableStateOf(0) }       // 0..100
                var scannedIds by remember { mutableStateOf(setOf<String>()) }  // dedup

                fun applyScan(value: String) {
                    val id = stationIdFrom(value) ?: return
                    if (!scannedIds.contains(id)) {
                        val prev = calibration
                        scannedIds = scannedIds + id
                        calibration = (calibration + 10).coerceAtMost(100)
                        if (prev < 20 && calibration >= 20) {
                            showOverlay("Calibration 20%", "LUMEN EMITTER unlocked (prototype)")
                        }
                    }
                }

                val lumenUnlocked      = true//calibration >= 20
                val pathfinderUnlocked = calibration >= 60
                val codexUnlocked      = calibration >= 85

                // -------- Navigation --------
                var showScanner by remember { mutableStateOf(false) }
                var currentStationId by remember { mutableStateOf<String?>(null) }

                // Box für z‑Reihenfolge (Overlay oben)
                Box(Modifier.fillMaxSize()) {

                    when {
                        showScanner -> {
                            com.example.indytrail.ui.ScanScreen(
                                calibrationPercent = calibration,
                                onResult = { value ->
                                    val route = com.example.indytrail.core.parseScanUri(value)
                                    when (route) {
                                        is com.example.indytrail.core.ScanRoute.Station -> {
                                            currentStationId = route.stationId
                                            // applyScan(value) // falls du weiterhin kalibrieren willst
                                        }
                                        is com.example.indytrail.core.ScanRoute.QuestOpen -> {
                                            currentQuestId = route.questId
                                            showQuest = true
                                        }
                                        is com.example.indytrail.core.ScanRoute.QuestGlyph -> {
                                            val res = questStore.applyGlyph(route.questId, route.slot, route.glyph)
                                            currentQuestId = route.questId
                                            if (res !is com.example.indytrail.data.QuestStore.ApplyResult.NoChange) {
                                                questTick++          // QuestScreen neu zeichnen
                                            }
                                            showQuest = true        // nach Scan zurück in den Quest
                                        }
                                        null -> {
                                            android.util.Log.d("SCAN", "Unbekanntes Format: $value")
                                        }
                                    }
                                    showScanner = false
                                },
                                onBack = { showScanner = false }
                            )
                        }

                        currentStationId != null -> {
                            val st = com.example.indytrail.data.Stations.byId(currentStationId!!)
                            if (st != null) {
                                com.example.indytrail.ui.StationScreen(
                                    station = st,
                                    onBack = { currentStationId = null }
                                )
                            } else {
                                currentStationId = null
                            }
                        }

                        showQuest -> {
                            com.example.indytrail.ui.QuestScreen(
                                questId = currentQuestId,
                                store = questStore,
                                refreshTick = questTick,
                                onScanRequest = { showScanner = true },
                                onOpenEmitter = {
                                    // 1) aktuellen Quest-Fortschritt holen
                                    val progress = questStore.snapshot(currentQuestId)

                                    // 2) Aus den Codes (z.B. "PSI","R","F","X") die Emitter-Tasten ("1".."9") ableiten
                                    //    => wichtig: Zieltyp angeben, sonst "Cannot infer type..."
                                    val expected: List<String> = (1..4).mapNotNull { slot ->
                                        com.example.indytrail.data.GlyphCatalog.keyFromCode(progress[slot])
                                    }

                                    if (expected.size == 4) {
                                        emitterExpected = expected      // z.B. ["1","2","3","4"]
                                        showQuest = false               // zuerst Quest schließen
                                        showEmitter = true              // dann Emitter zeigen
                                    } else {
                                        // Optional: Hinweis anzeigen (noch nicht alle Glyphen)
                                        // showOverlay("Need 4 glyphs", "Scan all glyphs before using the emitter")
                                    }
                                }

                                ,
                                onBack = { showQuest = false }
                            )
                        }

                        showEmitter -> {
                            com.example.indytrail.ui.LumenEmitterScreen(
                                expected = emitterExpected,
                                hintGlyphs = emitterHintGlyphs,     // NEU
                                onBack = { showEmitter = false },
                                onSuccess = {
                                    showEmitter = false
                                    // Optional: Overlay/Toast usw.
                                    // showOverlay("Sequence Accepted", "Lumen Emitter ready")
                                }
                            )
                        }

                        else -> {
                            com.example.indytrail.ui.MenuScreen(
                                calibrationPercent = calibration,
                                lumenUnlocked = lumenUnlocked,
                                pathfinderUnlocked = pathfinderUnlocked,
                                codexUnlocked = codexUnlocked,
                                onTranslator = { showScanner = true },
                                onLumen = { /* später */ },
                                onPathfinder = { /* später */ },
                                onCodex = { /* später */ }
                                // Falls deine MenuScreen‑Signatur noch onShowOverlay besitzt:
                                // , onShowOverlay = { t, s -> showOverlay(t, s) }
                            )
                        }
                    }


                    // --- Overlay ganz oben rendern
                    com.example.indytrail.ui.AchievementOverlay(
                        visible = overlayVisible,
                        title = overlayTitle,
                        subtitle = overlaySubtitle,
                        onDismiss = { overlayVisible = false },
                        logoRes = R.drawable.indy_trail_logo,   // Dateiname ohne .png
                        tintLogoWithPrimary = false
                    )
                }
            }
        }
    }
}
