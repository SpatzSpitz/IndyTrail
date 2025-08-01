@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.indytrail.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@Composable
fun LumenEmitterScreen(
    expected: List<String>,                 // z.B. ["1","2","3","4"] – Tastenfolge
    hintGlyphs: List<String?> = emptyList(),// optional: Codes wie ["PSI","R","F","X"]
    requiredCode: String = "trail://quest/Q1",
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    ImmersiveSystemBars()

    val cyan = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var idx by remember { mutableStateOf(0) }
    LaunchedEffect(expected) { idx = 0 }

    fun press(key: String): Boolean {
        return if (expected.getOrNull(idx) == key) {
            idx++
            idx == expected.size
        } else {
            idx = 0
            false
        }
    }

    var hasCamera by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCamera = granted }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val previewView = remember { PreviewView(context) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val scope = rememberCoroutineScope()

    val scanner = remember { BarcodeScanning.getClient() }
    var codeVisible by remember { mutableStateOf(false) }
    var lastValid by remember { mutableStateOf(0L) }

    LaunchedEffect(lastValid) {
        if (lastValid == 0L) return@LaunchedEffect
        delay(2500)
        if (System.currentTimeMillis() - lastValid >= 2500) {
            codeVisible = false
        }
    }

    LaunchedEffect(codeVisible) {
        if (!codeVisible) {
            idx = 0
        }
    }

    val flashPatterns = remember {
        mapOf(
            "1" to ".-..",
            "2" to "..-.",
            "3" to "-...",
            "4" to "--..",
            "5" to ".-.-",
            "6" to "-..-",
            "7" to "...-",
            "8" to ".--.",
            "9" to "-.-."
        )
    }

    suspend fun flash(pattern: String) {
        val cam = camera ?: return
        pattern.forEachIndexed { i, c ->
            val onTime = if (c == '-') 300L else 100L
            cam.cameraControl.enableTorch(true)
            delay(onTime)
            cam.cameraControl.enableTorch(false)
            if (i != pattern.lastIndex) delay(120)
        }
    }

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
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build().also { analysis ->
                        analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(
                                    mediaImage, imageProxy.imageInfo.rotationDegrees
                                )
                                scanner.process(image)
                                    .addOnSuccessListener { barcodes ->
                                        val value = barcodes.firstOrNull()?.rawValue
                                        if (value == requiredCode) {
                                            codeVisible = true
                                            lastValid = System.currentTimeMillis()
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else imageProxy.close()
                        }
                    }
                camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (t: Throwable) {
                android.util.Log.e("EMITTER", "Bind failed", t)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(hasCamera) {
        onDispose {
            try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (_: Exception) { }
        }
    }

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
                            contentDescription = "Back",
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
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                EmitterOverlay(codeVisible)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                val keys = remember { (1..9).map(Int::toString).shuffled() }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (r in 0 until 3) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (c in 1..3) {
                                val label = keys[r * 3 + (c - 1)]
                                Button(
                                    onClick = {
                                        val ok = press(label)
                                        val pattern = flashPatterns[label] ?: ".-.."
                                        scope.launch {
                                            flash(pattern)
                                            if (ok) {
                                                delay(150)
                                                onSuccess()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = codeVisible
                                ) {
                                    Text(
                                        run {
                                            val code = com.example.indytrail.data.GlyphCatalog.codeFromKey(label)
                                            code?.let { com.example.indytrail.data.GlyphCatalog.symbol(it) } ?: label
                                        },
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
                }
            }
        }
    }
}

@Composable
private fun EmitterOverlay(valid: Boolean) {
    val frameColor = if (valid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val scrim = MaterialTheme.colorScheme.background.copy(alpha = 0.6f)
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val frameSize = kotlin.math.min(w, h) * 0.7f
        val left = (w - frameSize) / 2f
        val top = (h - frameSize) / 2f
        val stroke = 4.dp.toPx()
        val radius = 20.dp.toPx()

        drawRect(scrim, size = Size(w, top))
        drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, frameSize))
        drawRect(scrim, topLeft = Offset(left + frameSize, top), size = Size(w - (left + frameSize), frameSize))
        drawRect(scrim, topLeft = Offset(0f, top + frameSize), size = Size(w, h - (top + frameSize)))

        drawRoundRect(
            color = frameColor,
            topLeft = Offset(left, top),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = stroke)
        )
    }
}
