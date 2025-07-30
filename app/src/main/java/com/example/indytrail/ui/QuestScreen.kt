@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.indytrail.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indytrail.data.QuestStore
import com.example.indytrail.data.GlyphCatalog     // <- Mapping "PSI" -> sichtbares Zeichen
import com.example.indytrail.ui.theme.IndyGlyphs  // <- deine eingebettete Glyph-Schrift (optional)

@Composable
fun QuestScreen(
    questId: String,
    store: QuestStore,
    refreshTick: Int,          // erzwingt Recompose nach Scan
    onScanRequest: () -> Unit, // öffnet Scanner
    onBack: () -> Unit,
    onOpenEmitter: () -> Unit
) {
    val cyan  = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    val border = BorderStroke(1.dp, cyan.copy(alpha = 0.35f))

    // Aktuellen Stand für diese Quest holen
    @Suppress("UNUSED_EXPRESSION")
    refreshTick
    val progress = store.snapshot(questId)
    val slots = (1..4).map { i -> progress[i] } // List<String?>
    val foundCount = slots.count { !it.isNullOrBlank() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "QUEST $questId",
                        style = MaterialTheme.typography.titleLarge.copy(
                            letterSpacing = 2.sp
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // Headline
            Text(
                text = "Scan Glyphs",
                style = MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 1.5.sp,
                    color = cyan
                )
            )
            Spacer(Modifier.height(16.dp))

            // 4 Glyph-Slots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                slots.forEach { code ->
                    GlyphSlot(
                        code = code,                 // "PSI" / "R" / "F" / "X" oder null
                        modifier = Modifier.weight(1f),
                        shape = shape,
                        border = border
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Divider mit kleinen Runen
            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = cyan.copy(alpha = 0.25f)
                )
                Text(
                    " ᚠᛇᚱ ",
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = cyan.copy(alpha = 0.25f)
                )
            }

            Spacer(Modifier.height(24.dp))

            // Scan-Button
            Button(
                onClick = onScanRequest,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Scan Glyphs",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

// --- TEMP: immer sichtbarer Debug-Button, um Emitter zu öffnen ---
            OutlinedButton(
                onClick = onOpenEmitter,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "Open Lumen Emitter (debug)",
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.2.sp)
                )
            }


            Spacer(Modifier.height(12.dp))

            // Status
            Text(
                text = "$foundCount/4 glyphs found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Ein einzelner, gerahmter Slot. Zeigt das gemappte Symbol, bei null '?' */
@Composable
private fun GlyphSlot(
    code: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    border: BorderStroke
) {
    val filled = !code.isNullOrBlank()
    val symbol = GlyphCatalog.symbol(code) // Mapping zu sichtbarem Zeichen

    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = shape,
        tonalElevation = if (filled) 4.dp else 2.dp,
        border = border,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ) {
        // dezenter innerer Verlauf
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (filled) symbol else "?",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = IndyGlyphs        // <- falls vorhanden; sonst Zeile entfernen
                ),
                color = if (filled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
