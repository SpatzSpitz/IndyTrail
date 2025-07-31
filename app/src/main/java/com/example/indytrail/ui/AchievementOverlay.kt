package com.example.indytrail.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AchievementOverlay(
    visible: Boolean,
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    @DrawableRes logoRes: Int? = null,
    tintLogoWithPrimary: Boolean = false,
    autoHideMillis: Long = 1600L
) {
    // Auto‑hide
    LaunchedEffect(visible, title, subtitle) {
        if (visible) {
            delay(autoHideMillis)
            onDismiss()
        }
    }

    // Panel shows a bit after the overlay becomes visible
    var showBanner by remember(visible, title, subtitle) { mutableStateOf(false) }
    LaunchedEffect(visible, title, subtitle) {
        showBanner = false
        if (visible) {
            delay(140)
            showBanner = true
        }
    }

    // Logo rotation (longer)
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(visible, title, subtitle) {
        if (visible) {
            rotation.snapTo(0f)
            rotation.animateTo(360f, animationSpec = tween(1200, easing = LinearEasing))
        }
    }

    // Panel slide from left (slower), but final position is anchored at x = 0 (left edge)
    val startPad = 12.dp
    val endPad = 12.dp
    val logoSize = 72.dp
    val gap = 1.dp
    val panelStartOffsetPx = with(LocalDensity.current) { 56.dp.roundToPx().toFloat() } // how far from left it starts

    val panelX = remember { Animatable(0f) }
    LaunchedEffect(showBanner) {
        if (showBanner) {
            panelX.snapTo(-panelStartOffsetPx)
            panelX.animateTo(0f, animationSpec = tween(700, easing = FastOutSlowInEasing))
        }
    }

    // Content inset so text starts to the right of the logo
    val contentInsetStart = startPad + logoSize + gap

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(150)) + slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            ),
            exit = fadeOut(tween(180)) + slideOutVertically(
                targetOffsetY = { -it / 2 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            )
        ) {
            // Top container area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, start = startPad, end = endPad)
                    .heightIn(min = 72.dp),
            ) {
                // --- Panel (background) spans full width and sits behind the logo
                AnimatedVisibility(
                    visible = showBanner,
                    enter = fadeIn(tween(380)),
                    exit  = fadeOut(tween(200))
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(18.dp),
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .matchParentSize() // fills the whole top strip (left to right)
                            .graphicsLayer { translationX = panelX.value } // slide in from left
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
                                        )
                                    )
                                )
                                // leave space for the logo on the left
                                .padding(start = contentInsetStart, end = 16.dp, top = 12.dp, bottom = 12.dp)
                        ) {
                            Text(
                                title,
                                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // --- Logo plaque pinned at the far left, above the panel
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .align(Alignment.CenterStart)             // left in this top strip
                        .graphicsLayer { rotationZ = rotation.value }
                        .clip(RoundedCornerShape(36.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f),
                            shape = RoundedCornerShape(36.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (logoRes != null) {
                        Image(
                            painter = painterResource(logoRes),
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = if (tintLogoWithPrimary)
                                ColorFilter.tint(MaterialTheme.colorScheme.primary)
                            else null
                        )
                    }
                }
            }
        }
    }
}




@Composable
private fun LogoBadge(
    badgeSize: Dp,
    rotation: Float,
    @DrawableRes logoRes: Int?,
    tintLogo: Boolean,
    ringColor: Color,
    accentColor: Color
) {
    Box(Modifier.size(badgeSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val r = minOf(w, h) / 2f
            val center = Offset(w / 2f, h / 2f)

            // outer ring (thin)
            drawCircle(
                color = ringColor.copy(alpha = 0.35f),
                radius = r - 2.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
            // gold ring
            drawCircle(
                color = accentColor.copy(alpha = 0.95f),
                radius = r - 6.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )
            // inner ring (cyan)
            drawCircle(
                color = ringColor.copy(alpha = 0.9f),
                radius = r - 12.dp.toPx(),
                style = Stroke(width = 2.dp.toPx())
            )

            // rotating notches
            val notchR = r - 9.dp.toPx()
            val notchLen = 6.dp.toPx()
            val count = 8
            for (i in 0 until count) {
                val ang = Math.toRadians((i * (360.0 / count) + rotation).mod(360.0))
                val dir = Offset(
                    x = kotlin.math.cos(ang).toFloat(),
                    y = kotlin.math.sin(ang).toFloat()
                )
                val start = center + dir * (notchR - notchLen)
                val end = center + dir * notchR
                drawLine(
                    color = accentColor.copy(alpha = 0.9f),
                    start = start, end = end, strokeWidth = 2.dp.toPx()
                )
            }
        }

        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = null,
                modifier = Modifier
                    .size(badgeSize * 0.55f)
                    .rotate(0f),
                colorFilter = if (tintLogo) ColorFilter.tint(ringColor) else null
            )
        }
    }
}
