package com.example.prayer.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.prayer.model.AppLanguage
import com.example.prayer.model.PrayerType

val LocalIsDarkTheme = compositionLocalOf { true }
val LocalAppLanguage = compositionLocalOf { AppLanguage.ENGLISH }

@Composable
fun isBanglaLanguage(): Boolean = LocalAppLanguage.current == AppLanguage.BANGLA

data class PrayerThemeColors(
    val isDark: Boolean,
    val primaryText: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val emeraldAccent: Color,
    val emeraldStrong: Color,
    val goldAccent: Color,
    val skyAccent: Color,
    val indigoAccent: Color,
    val crimsonAccent: Color,
    val topBarSurface: Color,
    val topBarBorder: Color,
    val subtleSurface: Color,
    val subtleBorder: Color,
    val insetPodSurface: Color
)

@Composable
fun currentPrayerThemeColors(): PrayerThemeColors {
    val isDark = LocalIsDarkTheme.current
    return if (isDark) {
        PrayerThemeColors(
            isDark = true,
            primaryText = Color(0xFFF8FAFC),
            secondaryText = Color(0xFFCBD5E1),
            mutedText = Color(0xFF94A3B8),
            emeraldAccent = Color(0xFF6EE7B7),
            emeraldStrong = Color(0xFF10B981),
            goldAccent = Color(0xFFFDE047),
            skyAccent = Color(0xFF7DD3FC),
            indigoAccent = Color(0xFFC7D2FE),
            crimsonAccent = Color(0xFFFDA4AF),
            topBarSurface = Color(0xFF0A1624).copy(alpha = 0.95f),
            topBarBorder = Color(0xFF38BDF8).copy(alpha = 0.35f),
            subtleSurface = Color(0xFF0F1E2E).copy(alpha = 0.85f),
            subtleBorder = Color(0xFF334155).copy(alpha = 0.70f),
            insetPodSurface = Color(0xFF07111D).copy(alpha = 0.85f)
        )
    } else {
        PrayerThemeColors(
            isDark = false,
            primaryText = Color(0xFF0F172A),
            secondaryText = Color(0xFF1E293B),
            mutedText = Color(0xFF475569),
            emeraldAccent = Color(0xFF047857),
            emeraldStrong = Color(0xFF059669),
            goldAccent = Color(0xFFB45309),
            skyAccent = Color(0xFF0369A1),
            indigoAccent = Color(0xFF4338CA),
            crimsonAccent = Color(0xFFBE123C),
            topBarSurface = Color(0xFFFFFFFF).copy(alpha = 0.98f),
            topBarBorder = Color(0xFFCBD5E1),
            subtleSurface = Color(0xFFF1F5F9).copy(alpha = 0.96f),
            subtleBorder = Color(0xFFCBD5E1),
            insetPodSurface = Color(0xFFF8FAFC)
        )
    }
}

/**
 * Dynamic Time-of-Day Celestial Frosted Glass Backdrop:
 * Supports both Dark Night Mode and Light Daylight Sanctuary Mode.
 */
@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    activePrayerType: PrayerType? = null,
    isForbidden: Boolean = false
) {
    val isDark = LocalIsDarkTheme.current
    val infiniteTransition = rememberInfiniteTransition(label = "CelestialAtmosphere")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val drift by infiniteTransition.animateFloat(
        initialValue = -0.03f,
        targetValue = 0.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(8500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val baseSkyColors = if (isDark) {
        when {
            isForbidden -> listOf(
                Color(0xFF1E0813),
                Color(0xFF280B18),
                Color(0xFF120B1D),
                Color(0xFF0A0F1D)
            )
            activePrayerType == PrayerType.FAJR -> listOf(
                Color(0xFF071A2E),
                Color(0xFF0C263B),
                Color(0xFF151C36),
                Color(0xFF091322)
            )
            activePrayerType == PrayerType.DHUHR -> listOf(
                Color(0xFF062229),
                Color(0xFF0A2E36),
                Color(0xFF0D2132),
                Color(0xFF081523)
            )
            activePrayerType == PrayerType.ASR -> listOf(
                Color(0xFF1A1829),
                Color(0xFF1F2235),
                Color(0xFF121D2E),
                Color(0xFF091320)
            )
            activePrayerType == PrayerType.MAGHRIB -> listOf(
                Color(0xFF1D102B),
                Color(0xFF281432),
                Color(0xFF14182E),
                Color(0xFF091120)
            )
            else -> listOf(
                Color(0xFF061523),
                Color(0xFF0A1E2E),
                Color(0xFF0E182C),
                Color(0xFF07101C)
            )
        }
    } else {
        when {
            isForbidden -> listOf(
                Color(0xFFFFF1F2),
                Color(0xFFFFE4E6),
                Color(0xFFFDF2F8),
                Color(0xFFF8FAFC)
            )
            activePrayerType == PrayerType.FAJR -> listOf(
                Color(0xFFE0F2FE),
                Color(0xFFECFDF5),
                Color(0xFFF0F9FF),
                Color(0xFFF8FAFC)
            )
            activePrayerType == PrayerType.ASR -> listOf(
                Color(0xFFFEF3C7),
                Color(0xFFECFDF5),
                Color(0xFFF8FAFC),
                Color(0xFFF1F5F9)
            )
            else -> listOf(
                Color(0xFFECFDF5),
                Color(0xFFE6F4F1),
                Color(0xFFF0F9FF),
                Color(0xFFF8FAFC)
            )
        }
    }

    val primaryOrbColor = when {
        isForbidden -> Color(0xFFF43F5E)
        activePrayerType == PrayerType.FAJR -> Color(0xFF38BDF8)
        activePrayerType == PrayerType.DHUHR -> Color(0xFF10B981)
        activePrayerType == PrayerType.ASR -> Color(0xFFF59E0B)
        activePrayerType == PrayerType.MAGHRIB -> Color(0xFFEC4899)
        else -> Color(0xFF14B8A6)
    }

    val secondaryOrbColor = when {
        isForbidden -> Color(0xFFF59E0B)
        activePrayerType == PrayerType.FAJR -> Color(0xFF818CF8)
        activePrayerType == PrayerType.DHUHR -> Color(0xFFFBBF24)
        activePrayerType == PrayerType.ASR -> Color(0xFF10B981)
        activePrayerType == PrayerType.MAGHRIB -> Color(0xFF8B5CF6)
        else -> Color(0xFF38BDF8)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = baseSkyColors))
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val orbAlpha = if (isDark) 0.25f else 0.14f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryOrbColor.copy(alpha = orbAlpha * pulse),
                        secondaryOrbColor.copy(alpha = orbAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(w * (0.50f + drift), h * 0.14f),
                    radius = w * 0.85f
                ),
                center = Offset(w * (0.50f + drift), h * 0.14f),
                radius = w * 0.85f
            )

            drawCircle(
                color = if (isDark) Color.White.copy(alpha = 0.04f) else Color(0xFF047857).copy(alpha = 0.06f),
                radius = w * 0.44f,
                center = Offset(w * 0.5f, h * 0.16f),
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = if (isDark) Color(0xFFFDE047).copy(alpha = 0.045f) else Color(0xFFD97706).copy(alpha = 0.06f),
                radius = w * 0.32f,
                center = Offset(w * 0.5f, h * 0.16f),
                style = Stroke(width = 1.dp.toPx())
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = (if (isDark) 0.16f else 0.10f) * pulse),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.12f, h * (0.52f + drift)),
                    radius = w * 0.65f
                ),
                center = Offset(w * 0.12f, h * (0.52f + drift)),
                radius = w * 0.65f
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryOrbColor.copy(alpha = if (isDark) 0.15f else 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.82f),
                    radius = w * 0.70f
                ),
                center = Offset(w * 0.88f, h * 0.82f),
                radius = w * 0.70f
            )
        }
    }
}

/**
 * Refined Frosted Glass Card supporting both Dark Night and Light Daylight themes.
 */
@Composable
fun AppleLiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    isActive: Boolean = false,
    tintColor: Color = Color(0xFF10B981),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val backgroundBrush = if (isDark) {
        if (isActive) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF064E3B).copy(alpha = 0.92f),
                    Color(0xFF0F293A).copy(alpha = 0.95f),
                    Color(0xFF0B192C).copy(alpha = 0.97f)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF132235).copy(alpha = 0.94f),
                    tintColor.copy(alpha = 0.16f),
                    Color(0xFF0D1B2A).copy(alpha = 0.97f)
                )
            )
        }
    } else {
        if (isActive) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFF0FDF4),
                    Color(0xFFFFFFFF),
                    Color(0xFFECFDF5)
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    tintColor.copy(alpha = 0.08f),
                    Color(0xFFF8FAFC)
                )
            )
        }
    }

    val borderBrush = if (isDark) {
        if (isActive) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF34D399).copy(alpha = 0.92f),
                    Color(0xFFFDE047).copy(alpha = 0.70f),
                    Color(0xFF10B981).copy(alpha = 0.80f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.25f),
                    tintColor.copy(alpha = 0.24f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        }
    } else {
        if (isActive) {
            Brush.linearGradient(
                colors = listOf(
                    Color(0xFF059669).copy(alpha = 0.90f),
                    Color(0xFFD97706).copy(alpha = 0.75f),
                    Color(0xFF10B981).copy(alpha = 0.85f)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE2E8F0),
                    tintColor.copy(alpha = 0.35f),
                    Color(0xFFCBD5E1)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundBrush)
            .border(
                width = if (isActive) 1.5.dp else 1.1.dp,
                brush = borderBrush,
                shape = shape
            ),
        content = content
    )
}

/**
 * Frosted Glass Pill for date navigation, status bars, and chips (Dark & Light theme aware)
 */
@Composable
fun AppleLiquidGlassPill(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(14.dp),
    tintColor: Color = Color(0xFF10B981),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bgColors = if (isDark) {
        listOf(
            Color(0xFF16263B).copy(alpha = 0.84f),
            tintColor.copy(alpha = 0.18f),
            Color(0xFF0F1C2E).copy(alpha = 0.88f)
        )
    } else {
        listOf(
            Color(0xFFFFFFFF).copy(alpha = 0.96f),
            tintColor.copy(alpha = 0.10f),
            Color(0xFFF1F5F9).copy(alpha = 0.96f)
        )
    }

    val borderColors = if (isDark) {
        listOf(
            Color.White.copy(alpha = 0.28f),
            tintColor.copy(alpha = 0.45f),
            Color.White.copy(alpha = 0.18f)
        )
    } else {
        listOf(
            tintColor.copy(alpha = 0.45f),
            Color(0xFF0F172A).copy(alpha = 0.16f),
            tintColor.copy(alpha = 0.35f)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.horizontalGradient(colors = bgColors))
            .border(
                width = 1.1.dp,
                brush = Brush.horizontalGradient(colors = borderColors),
                shape = shape
            ),
        content = content
    )
}

/**
 * High-Contrast Crimson Alert Card for Makrooh Tahrimi Forbidden Windows
 */
@Composable
fun AppleLiquidGlassWarning(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val bgColors = if (isDark) {
        listOf(
            Color(0xFF881337).copy(alpha = 0.90f),
            Color(0xFF4C0519).copy(alpha = 0.94f)
        )
    } else {
        listOf(
            Color(0xFFFFE4E6).copy(alpha = 0.98f),
            Color(0xFFFECDD3).copy(alpha = 0.95f)
        )
    }

    val borderColors = if (isDark) {
        listOf(
            Color(0xFFFDA4AF).copy(alpha = 0.90f),
            Color(0xFFF43F5E).copy(alpha = 0.60f)
        )
    } else {
        listOf(
            Color(0xFFE11D48).copy(alpha = 0.80f),
            Color(0xFFBE123C).copy(alpha = 0.55f)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(Brush.horizontalGradient(colors = bgColors))
            .border(
                width = 1.3.dp,
                brush = Brush.verticalGradient(colors = borderColors),
                shape = shape
            ),
        content = content
    )
}

