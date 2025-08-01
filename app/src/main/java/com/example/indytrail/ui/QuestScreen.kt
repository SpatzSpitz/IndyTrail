@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.indytrail.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indytrail.data.QuestStore
import com.example.indytrail.data.GlyphCatalog     // mapping "PSI" -> visible symbol
import com.example.indytrail.ui.theme.IndyGlyphs  // optional custom glyph font
import kotlinx.coroutines.delay

@Composable
fun QuestScreen(
    questId: String,
    store: QuestStore,
    refreshTick: Int,          // force recomposition after scan
    onScanRequest: () -> Unit, // open scanner
    onBack: () -> Unit,
    onOpenEmitter: () -> Unit,
    completed: Boolean = false,
    onFinishQuest: () -> Unit = {}
) {
    ImmersiveSystemBars()

    val cyan  = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    val border = BorderStroke(1.dp, cyan.copy(alpha = 0.35f))

    // obtain current progress for this quest
    @Suppress("UNUSED_EXPRESSION")
    refreshTick
    val progress = store.snapshot(questId)
    val slots = (1..4).map { i -> progress[i] } // List<String?>
    val foundCount = slots.count { !it.isNullOrBlank() }
    val emitterEnabled = foundCount == 4

    var showCompletion by remember(completed) { mutableStateOf(false) }
    LaunchedEffect(completed) {
        if (completed) {
            delay(600)
            showCompletion = true
        } else {
            showCompletion = false
        }
    }

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

            OutlinedButton(
                onClick = onOpenEmitter,
                shape = RoundedCornerShape(14.dp),
                enabled = emitterEnabled,
                modifier = Modifier.alpha(if (emitterEnabled) 1f else 0.5f)
            ) {
                Text(
                    "Open Lumen Emitter",
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

            AnimatedVisibility(
                visible = showCompletion,
                enter = fadeIn() + scaleIn()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Quest Complete",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        ),
                        color = cyan
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onFinishQuest,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            "Finish Quest",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.5.sp
                            )
                        )
                    }
                }
            }
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
