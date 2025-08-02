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
import com.example.indytrail.data.GlyphCatalog
import com.example.indytrail.data.QuestStore
import com.example.indytrail.data.Stations
import com.example.indytrail.ui.*
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

                // ----- Quest store -----
                val questStore = remember { QuestStore() }
                var showEmitter by remember { mutableStateOf(false) }
                var showPathfinder by remember { mutableStateOf(false) }
                var emitterExpected by remember { mutableStateOf(listOf("1","2","3","4")) } // test sequence
                val emitterHintGlyphs by remember { mutableStateOf(listOf<String?>()) }
                var showQuest by remember { mutableStateOf(false) }
                var currentQuestId by remember { mutableStateOf("Q1") }   // set via QR
                var questTick by remember { mutableStateOf(0) }
                var completedQuests by remember { mutableStateOf(setOf<String>()) }
                // ----- Overlay state (visual only) -----
                var overlayVisible by remember { mutableStateOf(false) }
                var overlayTitle by remember { mutableStateOf("") }
                var overlaySubtitle by remember { mutableStateOf("") }
                fun showOverlay(t: String, s: String) {
                    overlayTitle = t
                    overlaySubtitle = s
                    overlayVisible = true
                }

                // number of glyphs found for quest Q1
                val q1Found = questStore.foundCount("Q1")


                // -------- Calibration & feature unlocks --------
                var calibration by rememberSaveable { mutableStateOf(0) }       // 0..100
                var scannedIds by remember { mutableStateOf(setOf<String>()) }  // deduplicate station scans

                val lumenUnlocked      = true // calibration >= 20
                val pathfinderUnlocked = calibration >= 60
                val codexUnlocked      = calibration >= 85

                // -------- Navigation --------
                var showScanner by remember { mutableStateOf(false) }
                var currentStationId by remember { mutableStateOf<String?>(null) }

                // Root container to keep the overlay above other content
                Box(Modifier.fillMaxSize()) {

                    when {
                        showScanner -> {
                            ScanScreen(
                                calibrationPercent = calibration,
                                onResult = { value ->
                                    val route = parseScanUri(value)
                                    when (route) {
                                        is ScanRoute.Station -> {
                                            currentStationId = route.stationId
                                        }
                                        is ScanRoute.QuestOpen -> {
                                            currentQuestId = route.questId
                                            showQuest = true
                                        }
                                        is ScanRoute.QuestGlyph -> {
                                            val res = questStore.applyGlyph(route.questId, route.slot, route.glyph)
                                            currentQuestId = route.questId
                                            if (res !is QuestStore.ApplyResult.NoChange) {
                                                questTick++          // force recomposition
                                            }
                                            showQuest = true        // return to quest after scanning
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
                            val st = Stations.byId(currentStationId!!)
                            if (st != null) {
                                StationScreen(
                                    station = st,
                                    onBack = { currentStationId = null }
                                )
                            } else {
                                currentStationId = null
                            }
                        }

                        showQuest -> {
                            QuestScreen(
                                questId = currentQuestId,
                                store = questStore,
                                refreshTick = questTick,
                                onScanRequest = { showScanner = true },
                                onOpenEmitter = {
                                    // read current quest progress
                                    val progress = questStore.snapshot(currentQuestId)

                                    // map codes (e.g. PSI/R/F/X) to emitter keys ("1".."9")
                                    val expected: List<String> = (1..4).mapNotNull { slot ->
                                        GlyphCatalog.keyFromCode(progress[slot])
                                    }

                                    if (expected.size == 4) {
                                        emitterExpected = expected      // e.g. ["1","2","3","4"]
                                        showQuest = false               // close quest first
                                        showEmitter = true              // then show emitter
                                    } else {
                                        // optional hint if not all glyphs are scanned
                                        // showOverlay("Need 4 glyphs", "Scan all glyphs before using the emitter")
                                    }
                                },
                                completed = completedQuests.contains(currentQuestId),
                                onFinishQuest = {
                                    completedQuests = completedQuests - currentQuestId
                                    showQuest = false
                                },
                                onBack = { showQuest = false }
                            )
                        }

                        showEmitter -> {
                                LumenEmitterScreen(
                                    expected = emitterExpected,
                                    hintGlyphs = emitterHintGlyphs,
                                    requiredCode = "trail://quest/$currentQuestId",
                                onBack = { showEmitter = false },
                                onSuccess = {
                                    showEmitter = false
                                    completedQuests = completedQuests + currentQuestId
                                    showQuest = true
                                    // optional overlay/toast
                                    // showOverlay("Sequence Accepted", "Lumen Emitter ready")
                                }
                            )
                        }

                        showPathfinder -> {
                            PathfinderScreen(onBack = { showPathfinder = false })
                        }

                        else -> {
                            MenuScreen(
                                calibrationPercent = calibration,
                                lumenUnlocked = lumenUnlocked,
                                pathfinderUnlocked = pathfinderUnlocked,
                                codexUnlocked = codexUnlocked,
                                onTranslator = { showScanner = true },
                                onLumen = { /* TODO */ },
                                onPathfinder = { showPathfinder = true },
                                onCodex = { /* TODO */ }
                            )
                        }
                    }


                    // Render the achievement overlay above all content
                    AchievementOverlay(
                        visible = overlayVisible,
                        title = overlayTitle,
                        subtitle = overlaySubtitle,
                        onDismiss = { overlayVisible = false },
                        logoRes = R.drawable.indy_trail_logo,
                        tintLogoWithPrimary = false
                    )
                }
            }
        }
    }
}
