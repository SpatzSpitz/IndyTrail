package com.example.indytrail.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    calibrationPercent: Int,          // << neu
    onResult: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // —— Immerse: Status- & Navigationsleiste im Scanner ausblenden
    LaunchedEffect(Unit) {
        val activity = context as Activity
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            // beim Verlassen wieder normales Verhalten
            val activity = context as Activity
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // —— Kamera-Rechte
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "TRANSLATOR CORE",
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->                       // <— WICHTIG: Padding vom Scaffold übernehmen
        if (!hasPermission) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Kamerazugriff benötigt.")
            }
            return@Scaffold
        }

        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        val scanner = remember { BarcodeScanning.getClient() }
        var delivered by remember { mutableStateOf(false) }

        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)          // <— Alles unterhalb der App-Bar positionieren
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)

                    val preview = Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().apply {
                            setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage, imageProxy.imageInfo.rotationDegrees
                                    )
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val value = barcodes.firstOrNull()?.rawValue
                                            if (value != null && !delivered) {
                                                delivered = true
                                                onResult(value)
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else imageProxy.close()
                            }
                        }

                    val selector = CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        try {
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                        } catch (_: Exception) { }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            ScannerOverlay() // thematischer Rahmen/Ornamente
        }

        DisposableEffect(Unit) {
            onDispose {
                try { cameraProviderFuture.get().unbindAll() } catch (_: Exception) { }
            }
        }
    }
}

@Composable
private fun ScannerOverlay() {
    val cyan = MaterialTheme.colorScheme.primary      // Hieroglyphen (blau)
    val gold = MaterialTheme.colorScheme.secondary    // (nur Scan-Linie nutzt gold)
    val bg   = MaterialTheme.colorScheme.background

    // Puls (innenrahmen) & Scan-Linie
    val pulse = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.35f, targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "alpha"
    ).value
    val scanY = rememberInfiniteTransition(label = "scan").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ), label = "v"
    ).value

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // ===== (1) MITTLERER SCAN-FRAME – UNVERÄNDERT
            val frameSize = minOf(w, h) * 0.70f
            val left = (w - frameSize) / 2f
            val top  = (h - frameSize) / 2f
            val rThin = 3.dp.toPx()
            val radius = 20.dp.toPx()

            drawRoundRect(
                color = cyan.copy(alpha = pulse),
                topLeft = Offset(left, top),
                size = Size(frameSize, frameSize),
                cornerRadius = CornerRadius(radius, radius),
                style = Stroke(width = rThin)
            )

            val yScan = top + frameSize * scanY
            drawLine(
                color = gold.copy(alpha = 0.55f),
                start = Offset(left + 10.dp.toPx(), yScan),
                end   = Offset(left + frameSize - 10.dp.toPx(), yScan),
                strokeWidth = 2.5.dp.toPx()
            )

            // ===== (2) Außenbereich abdunkeln (gleiches Theme-Feeling)
            val scrim = bg.copy(alpha = 0.78f)
            drawRect(scrim, size = Size(w, top)) // oben
            drawRect(scrim, topLeft = Offset(0f, top), size = Size(left, frameSize)) // links
            drawRect(scrim, topLeft = Offset(left + frameSize, top),
                size = Size(w - (left + frameSize), frameSize)) // rechts
            drawRect(scrim, topLeft = Offset(0f, top + frameSize),
                size = Size(w, h - (top + frameSize))) // unten

            // ===== (3) KURZE HIEROGLYPHEN-CLUSTER DIREKT AM RAND (ohne Linien)
            val inset = 6.dp.toPx()         // minimaler Innenabstand zum Rand
            val txtSize = 16.sp.toPx()
            val clusterTop    = "ᚠᛇᚻ᛫ᚠᛇ"
            val clusterBottom = "ᛟᚦᛁᚾᚷ"
            val clusterSide   = "ᚨᚱᛏ᛫"   // wird zeichenweise gestapelt

            val nc = drawContext.canvas.nativeCanvas
            val p = android.graphics.Paint().apply {
                isAntiAlias = true
                textSize = txtSize
                letterSpacing = 0.10f
                color = cyan.copy(alpha = 0.95f).toArgb()
            }

            // — oben (zentriert, Baseline knapp unter dem Rand)
            run {
                val tw = p.measureText(clusterTop)
                val x = (w - tw) / 2f
                val y = inset + txtSize
                nc.drawText(clusterTop, x, y, p)
            }
            // — unten (zentriert, Baseline knapp über dem Rand)
            run {
                val tw = p.measureText(clusterBottom)
                val x = (w - tw) / 2f
                val y = h - inset
                nc.drawText(clusterBottom, x, y, p)
            }
            // — links (kurzer vertikaler Stapel, zentriert)
            run {
                val chars = clusterSide.toCharArray()
                val vStep = txtSize * 1.15f
                val totalH = vStep * chars.size
                var y0 = (h - totalH) / 2f + txtSize
                val x = inset + 2.dp.toPx()
                for (ch in chars) {
                    nc.drawText(ch.toString(), x, y0, p)
                    y0 += vStep
                }
            }
            // — rechts (kurzer vertikaler Stapel, zentriert, rechtsbündig)
            run {
                val chars = clusterSide.toCharArray()
                val vStep = txtSize * 1.15f
                val totalH = vStep * chars.size
                var y0 = (h - totalH) / 2f + txtSize
                val cw = p.measureText("ᚠ").coerceAtLeast(10f)
                val x = w - inset - 2.dp.toPx() - cw
                for (ch in chars) {
                    nc.drawText(ch.toString(), x, y0, p)
                    y0 += vStep
                }
            }
        }
        // kein Hinweis-Text mehr
    }
}





