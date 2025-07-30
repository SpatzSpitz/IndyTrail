@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.indytrail.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun LumenEmitterScreen(
    expected: List<String>,                 // z.B. ["1","2","3","4"] – Tastenfolge
    hintGlyphs: List<String?> = emptyList(),// optional: Codes wie ["PSI","R","F","X"]
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val cyan = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // --- Fortschrittsindex in expected ---
    var idx by remember { mutableStateOf(0) }

    // Bei neuer erwarteter Sequenz Fortschritt zurücksetzen
    LaunchedEffect(expected) { idx = 0 }

    fun press(key: String) {
        if (expected.getOrNull(idx) == key) {
            idx++
            if (idx == expected.size) {
                idx = 0
                onSuccess()
            }
        } else {
            idx = 0
        }
    }

    // --- Kamera‑Permission (für Torch) ---
    var hasCamera by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- Unsichtbare PreviewView, nur um CameraX zu binden (Torch-Steuerung) ---
    val previewView = remember { PreviewView(context).apply { alpha = 0f } }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(hasCamera) {
        if (!hasCamera) return@LaunchedEffect
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            try {
                provider.unbindAll()
                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview)
            } catch (t: Throwable) {
                android.util.Log.e("EMITTER", "Bind failed", t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // --- Aus expected die sichtbaren Runen ableiten ---
    // expected enthält Tasten ("1".."9") -> erst Code (PSI/R/...) -> dann Symbol.
// --- Aus expected die sichtbaren Runen ableiten ---
    val expectedSymbols: List<String> = remember(expected) {
        expected.mapNotNull { key ->
            val code = com.example.indytrail.data.GlyphCatalog.codeFromKey(key)
            code?.let { com.example.indytrail.data.GlyphCatalog.symbol(it) }
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "LUMEN EMITTER",
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp)
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
            // Unsichtbare Preview (öffnet die Kamera, damit Torch verfügbar ist)
            AndroidView(
                factory = { previewView },
                modifier = Modifier.size(1.dp)
            )

            // --- Fortschritts‑LEDs ---
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(expected.size) { i ->
                    val on = i < idx
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (on) cyan else cyan.copy(alpha = 0.2f))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Optional: Torch‑Buttons ---
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = camera != null,
                    onClick = { camera?.cameraControl?.enableTorch(true) }
                ) { Text("Torch ON") }

                OutlinedButton(
                    enabled = camera != null,
                    onClick = { camera?.cameraControl?.enableTorch(false) }
                ) { Text("Torch OFF") }
            }

            Spacer(Modifier.height(12.dp))

            // --- Runen‑Zeile aus expected (sichtbare Hinweise) ---
            if (expectedSymbols.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    expectedSymbols.forEach { sym ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            tonalElevation = 2.dp,
                            border = BorderStroke(1.dp, cyan.copy(alpha = 0.35f)),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ) {
                            Box(
                                Modifier
                                    .size(44.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = sym,
                                    // Falls du eine eigene Glyph‑Schrift nutzt:
                                    // fontFamily = IndyGlyphs,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        letterSpacing = 2.sp
                                    ),
                                    color = cyan
                                )
                            }
                        }
                    }
                }
            }

            // --- (Optional) zusätzlich die vom Quest gelieferten Codes als Chips ---
            if (hintGlyphs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    hintGlyphs.forEach { code ->
                        AssistChip(onClick = {}, label = { Text(code ?: "?") })
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- 3x3‑Tastenfeld (1..9) ---
            val keys = (1..9).map(Int::toString)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (r in 0 until 3) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (c in 1..3) {
                            val label = keys[r * 3 + (c - 1)]
                            Button(
                                onClick = { press(label) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.2.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Debug‑Anzeige
            Text(
                "Expected: " + expected.joinToString("-"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
