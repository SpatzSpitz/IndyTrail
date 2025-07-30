package com.example.indytrail.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.indytrail.ui.theme.CavernBottom
import com.example.indytrail.ui.theme.CavernMid
import com.example.indytrail.ui.theme.CavernTop

// -------- Calibration bar (styled) --------
@Composable
fun CalibrationBar(percent: Int, modifier: Modifier = Modifier) {
    val pAnim by animateFloatAsState(
        targetValue = percent.coerceIn(0, 100) / 100f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "calib"
    )

    val cyan  = MaterialTheme.colorScheme.primary
    val gold  = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val face  = MaterialTheme.colorScheme.surface

    val glowPulse = rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.35f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    ).value

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Calibration ${percent.coerceIn(0, 100)}%",
            style = MaterialTheme.typography.labelLarge,
            color = cyan
        )
        Spacer(Modifier.height(8.dp))

        Box(
            Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(face.copy(alpha = 0.88f), track.copy(alpha = 0.92f))
                    )
                )
                .border(1.dp, cyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Canvas(Modifier.fillMaxWidth().height(20.dp)) {
                val w = size.width
                val h = size.height
                val r = 10.dp.toPx()
                val barH = h * 0.46f
                val y = (h - barH) / 2f

                // inset track
                drawRoundRect(
                    color = track.copy(alpha = 0.65f),
                    topLeft = Offset(0f, y),
                    size = Size(w, barH),
                    cornerRadius = CornerRadius(r, r),
                    style = Stroke(2.dp.toPx())
                )
                drawRoundRect(
                    color = face.copy(alpha = 0.35f),
                    topLeft = Offset(2.dp.toPx(), y + 2.dp.toPx()),
                    size = Size(w - 4.dp.toPx(), barH - 4.dp.toPx()),
                    cornerRadius = CornerRadius(r * .85f, r * .85f)
                )

                // progress
                val progW = (w * pAnim).coerceIn(0f, w)
                if (progW > 0f) {
                    drawRoundRect(
                        color = cyan.copy(alpha = 0.22f * glowPulse),
                        topLeft = Offset(0f, y - 3.dp.toPx()),
                        size = Size(progW, barH + 6.dp.toPx()),
                        cornerRadius = CornerRadius(r, r)
                    )
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                gold.copy(alpha = 0.85f),
                                gold.copy(alpha = 1f),
                                gold.copy(alpha = 0.85f)
                            ),
                            startX = 0f, endX = progW
                        ),
                        topLeft = Offset(0f, y),
                        size = Size(progW, barH),
                        cornerRadius = CornerRadius(r, r)
                    )

                    // diamond cap
                    val capX = progW.coerceIn(12.dp.toPx(), w - 8.dp.toPx())
                    val capY = y + barH / 2f
                    val capR = 6.dp.toPx()
                    val diamond = Path().apply {
                        moveTo(capX, capY - capR)
                        lineTo(capX + capR, capY)
                        lineTo(capX, capY + capR)
                        lineTo(capX - capR, capY)
                        close()
                    }
                    drawPath(diamond, color = cyan.copy(alpha = 0.9f))
                    drawPath(diamond, color = gold, style = Stroke(1.5.dp.toPx()))
                }

                // rune ticks
                val nc = drawContext.canvas.nativeCanvas
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textSize = 12.sp.toPx()
                    letterSpacing = 0.12f
                    color = cyan.copy(alpha = 0.85f).toArgb()
                }
                listOf(0.2f, 0.6f, 0.85f).forEach { t ->
                    val x = (w * t).coerceIn(12.dp.toPx(), w - 12.dp.toPx())
                    drawLine(
                        color = cyan.copy(alpha = 0.55f),
                        start = Offset(x, y - 4.dp.toPx()),
                        end   = Offset(x, y + barH + 4.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    val label = "ᚠᛇ"
                    nc.drawText(label, x - paint.measureText(label) / 2f, y - 6.dp.toPx(), paint)
                }
            }
        }
    }
}

// -------- Menu --------
@Composable
fun MenuScreen(
    calibrationPercent: Int,
    questGlyphsFound: Int = 0,      // ← Defaultwert
    lumenUnlocked: Boolean,
    pathfinderUnlocked: Boolean,
    codexUnlocked: Boolean,
    onTranslator: () -> Unit,
    onLumen: () -> Unit,
    onPathfinder: () -> Unit,
    onCodex: () -> Unit,
) {
    // If you use immersive system bars you already have ImmersiveSystemBars() elsewhere.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CavernTop, CavernMid, CavernBottom)))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            "DEVICE CONSOLE",
            style = MaterialTheme.typography.titleLarge.copy(
                letterSpacing = 2.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        CalibrationBar(percent = calibrationPercent)
        Spacer(Modifier.height(16.dp))

        MenuCard(
            title = "TRANSLATOR CORE",
            subtitle = if (questGlyphsFound > 0) "glyphs — $questGlyphsFound/4" else "unlocked — available",
            unlocked = true,
            onClick = onTranslator
        )
        Spacer(Modifier.height(12.dp))

        MenuCard(
            title = "LUMEN EMITTER",
            subtitle = if (lumenUnlocked) "unlocked — available" else "locked — calibration required",
            unlocked = lumenUnlocked,
            onClick = onLumen
        )
        Spacer(Modifier.height(12.dp))

        MenuCard(
            title = "PATHFINDER PROTOCOL",
            subtitle = if (pathfinderUnlocked) "unlocked — navigation" else "locked — navigation",
            unlocked = pathfinderUnlocked,
            onClick = onPathfinder
        )
        Spacer(Modifier.height(12.dp))

        MenuCard(
            title = "CODEX ARCHIVE",
            subtitle = if (codexUnlocked) "unlocked — field notes" else "locked — quest log / notes",
            unlocked = codexUnlocked,
            onClick = onCodex
        )
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = if (unlocked) 0.35f else 0.15f)
    val container = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)

    ElevatedCard(
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = container),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, shape)
            .clickable(enabled = unlocked, role = Role.Button) { onClick() }
    ) {
        Column(Modifier.padding(vertical = 14.dp, horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (unlocked) Icons.Filled.LockOpen else Icons.Filled.Lock
                Icon(
                    icon, contentDescription = null,
                    tint = if (unlocked) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
                Text(
                    " ᚠᛇᚱ ",
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
                    letterSpacing = 2.sp
                )
                Divider(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
