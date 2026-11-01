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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.model.ActiveWaqtInfo
import com.example.prayer.model.ForbiddenStatus
import com.example.prayer.model.NextPrayerInfo
import com.example.prayer.model.PrayerSchedule
import com.example.prayer.model.PrayerType
import com.example.prayer.util.DateUtils
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Commercial Sanctuary Hero Dashboard Card:
 * Responsive across all screen widths/heights and fully localized for English & Bangla.
 */
@Composable
fun SolarArcGauge(
    schedule: PrayerSchedule,
    currentTime: LocalTime,
    nextPrayer: NextPrayerInfo,
    activeWaqt: ActiveWaqtInfo?,
    forbiddenStatus: ForbiddenStatus,
    calculationBadgeText: String = "Karachi • Hanafi",
    use24HourFormat: Boolean = false,
    autoJumuahFriday: Boolean = true,
    onBadgeClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val themeColors = currentPrayerThemeColors()
    val isDark = themeColors.isDark
    val isBangla = isBanglaLanguage()
    val timeFormatter = remember(use24HourFormat) {
        if (use24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
        } else {
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        }
    }

    val isFriday = schedule.date.dayOfWeek == java.time.DayOfWeek.FRIDAY && autoJumuahFriday
    val activeWaqtTitle = if (activeWaqt != null) {
        val rawTitle = if (activeWaqt.prayerType == PrayerType.DHUHR && isFriday) "Jumu'ah" else activeWaqt.displayName
        DateUtils.localizePrayerName(rawTitle, isBangla)
    } else null

    val nextPrayerTitle = run {
        val rawTitle = if (nextPrayer.prayerType == PrayerType.DHUHR && isFriday) {
            "Jumu'ah"
        } else {
            nextPrayer.prayerType.displayName
        }
        DateUtils.localizePrayerName(rawTitle, isBangla)
    }

    // Calculate Sun/Moon position along the 24h cycle
    val currentSec = currentTime.toSecondOfDay()
    val sunriseSec = schedule.sunrise.toSecondOfDay()
    val maghribSec = schedule.maghrib.toSecondOfDay()
    val isDaytime = currentSec in sunriseSec..maghribSec

    val celestialProgress = if (isDaytime) {
        val span = (maghribSec - sunriseSec).coerceAtLeast(1)
        ((currentSec - sunriseSec).toFloat() / span.toFloat()).coerceIn(0f, 1f)
    } else {
        val nightDuration = (86400 - maghribSec) + sunriseSec
        val elapsedNight = if (currentSec > maghribSec) {
            currentSec - maghribSec
        } else {
            (86400 - maghribSec) + currentSec
        }
        (elapsedNight.toFloat() / nightDuration.toFloat().coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "HeroCelestialGlow")
    val sunGlow by infiniteTransition.animateFloat(
        initialValue = 0.86f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunGlow"
    )

    val isViewingToday = schedule.date == LocalDate.now()
    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 700
    val isNarrowWidth = configuration.screenWidthDp < 370
    val rightColumnWidth = if (isNarrowWidth) 136.dp else 148.dp

    val heroTint = when {
        forbiddenStatus.isForbidden -> Color(0xFFF43F5E)
        activeWaqt != null -> Color(0xFF10B981)
        else -> Color(0xFF38BDF8)
    }

    AppleLiquidGlassCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("solar_arc_gauge"),
        shape = RoundedCornerShape(22.dp),
        isActive = activeWaqt != null && !forbiddenStatus.isForbidden,
        tintColor = heroTint
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isNarrowWidth) 12.dp else 14.dp,
                    vertical = if (isCompactHeight) 8.dp else 11.dp
                )
        ) {
            // TOP SECTION: Left = Active Waqt / Next Prayer, Right = Mini Solar Arc + Countdown Pod
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Side: Status Chip + Main Waqt Title + Time Range + Next Prayer Subtitle
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    // Status Chip + Fiqh Calibration Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val chipBg = when {
                            !isViewingToday -> Color(0xFF818CF8).copy(alpha = if (isDark) 0.24f else 0.16f)
                            forbiddenStatus.isForbidden -> Color(0xFFF43F5E).copy(alpha = if (isDark) 0.26f else 0.16f)
                            activeWaqt != null -> Color(0xFF10B981).copy(alpha = if (isDark) 0.24f else 0.16f)
                            else -> Color(0xFF38BDF8).copy(alpha = if (isDark) 0.24f else 0.16f)
                        }
                        val chipBorder = when {
                            !isViewingToday -> Color(0xFFC7D2FE)
                            forbiddenStatus.isForbidden -> Color(0xFFFDA4AF)
                            activeWaqt != null -> themeColors.emeraldStrong
                            else -> themeColors.skyAccent
                        }
                        val chipLabel = when {
                            !isViewingToday -> if (isBangla) "নির্বাচিত তারিখ" else "SELECTED DATE"
                            forbiddenStatus.isForbidden -> if (isBangla) "মাকরূহ ওয়াক্ত" else "MAKRUH WINDOW"
                            activeWaqt != null -> if (isBangla) "চলমান ওয়াক্ত" else "ACTIVE WAQT"
                            else -> if (isBangla) "পরবর্তী ওয়াক্ত" else "NEXT SALAT"
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(chipBg)
                                .border(0.9.dp, chipBorder.copy(alpha = 0.7f), RoundedCornerShape(50))
                                .padding(horizontal = 7.dp, vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.5.dp)
                                    .clip(CircleShape)
                                    .background(chipBorder)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = chipLabel,
                                color = if (isDark) chipBorder else themeColors.primaryText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Compact Calibration Badge
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(50))
                                .background(themeColors.subtleSurface)
                                .border(0.8.dp, themeColors.subtleBorder, RoundedCornerShape(50))
                                .then(
                                    if (onBadgeClick != null) {
                                        Modifier.clickable { onBadgeClick() }
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("hero_calibration_badge")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = themeColors.goldAccent,
                                    modifier = Modifier.size(9.5.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = calculationBadgeText,
                                    color = themeColors.secondaryText,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 4.dp else 6.dp))

                    // Primary Prayer Name + Time Window
                    if (isViewingToday && activeWaqt != null && activeWaqtTitle != null) {
                        Text(
                            text = activeWaqtTitle,
                            color = themeColors.primaryText,
                            fontSize = if (isCompactHeight) 22.sp else 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        val startFormatted = DateUtils.localizeTime(activeWaqt.startTime.format(timeFormatter), isBangla)
                        val endFormatted = DateUtils.localizeTime(activeWaqt.endTime.format(timeFormatter), isBangla)
                        Text(
                            text = if (isBangla) "ওয়াক্ত: $startFormatted – $endFormatted" else "Waqt: $startFormatted – $endFormatted",
                            color = themeColors.emeraldAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val nextTimeFormatted = DateUtils.localizeTime(nextPrayer.prayerTime.format(timeFormatter), isBangla)
                        Text(
                            text = if (isBangla) {
                                "পরবর্তী: $nextPrayerTitle $nextTimeFormatted"
                            } else {
                                "Next: $nextPrayerTitle at $nextTimeFormatted"
                            },
                            color = themeColors.secondaryText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        val nextTimeFormatted = DateUtils.localizeTime(nextPrayer.prayerTime.format(timeFormatter), isBangla)
                        Text(
                            text = nextPrayerTitle,
                            color = themeColors.primaryText,
                            fontSize = if (isCompactHeight) 22.sp else 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                        Text(
                            text = if (isViewingToday) {
                                if (isBangla) "শুরু হবে $nextTimeFormatted" else "Starts at $nextTimeFormatted"
                            } else {
                                val fajrStr = DateUtils.localizeTime(schedule.fajr.format(timeFormatter), isBangla)
                                val ishaStr = DateUtils.localizeTime(schedule.isha.format(timeFormatter), isBangla)
                                if (isBangla) "ফজর $fajrStr • ইশা $ishaStr" else "Fazr $fajrStr • Isha $ishaStr"
                            },
                            color = themeColors.skyAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val dhuhrLabel = if (isFriday) {
                            if (isBangla) "জুমুআহ" else "Jumu'ah"
                        } else {
                            if (isBangla) "যোহর" else "Duhr"
                        }
                        val dhuhrTimeStr = DateUtils.localizeTime(schedule.dhuhr.format(timeFormatter), isBangla)
                        val asrTimeStr = DateUtils.localizeTime(schedule.asr.format(timeFormatter), isBangla)
                        val asrLabel = if (isBangla) "আসর" else "Asr"
                        Text(
                            text = "$dhuhrLabel $dhuhrTimeStr • $asrLabel $asrTimeStr",
                            color = themeColors.secondaryText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Right Side: Clean Horizon Solar Arc + High-Contrast Countdown Pod
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(rightColumnWidth)
                ) {
                    // Compact Celestial Horizon Arc
                    Box(
                        contentAlignment = Alignment.BottomCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompactHeight) 48.dp else 56.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokePx = 2.8.dp.toPx()
                            val orbGlowRadiusPx = 11.dp.toPx()
                            val topPaddingPx = orbGlowRadiusPx + strokePx + 2.dp.toPx()
                            val bottomBaselinePaddingPx = 5.dp.toPx()
                            val horizonY = size.height - bottomBaselinePaddingPx

                            val maxRadiusFromHeight = (horizonY - topPaddingPx).coerceAtLeast(12f)
                            val maxRadiusFromWidth = (size.width * 0.82f) / 2f
                            val arcRadius = minOf(maxRadiusFromWidth, maxRadiusFromHeight)
                            val arcWidth = arcRadius * 2f
                            val centerX = size.width / 2f

                            // Dashed horizon baseline
                            drawLine(
                                color = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFF0F172A).copy(alpha = 0.14f),
                                start = Offset(centerX - arcRadius - 8.dp.toPx(), horizonY),
                                end = Offset(centerX + arcRadius + 8.dp.toPx(), horizonY),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                            )

                            val arcTopLeft = Offset(centerX - arcRadius, horizonY - arcRadius)
                            val arcSize = Size(arcWidth, arcRadius * 2f)

                            // Background semi-circle track
                            drawArc(
                                color = if (isDark) Color.White.copy(alpha = 0.13f) else Color(0xFF0F172A).copy(alpha = 0.11f),
                                startAngle = 180f,
                                sweepAngle = 180f,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )

                            // Active progress arc
                            val activeArcBrush = if (isDaytime) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF34D399),
                                        Color(0xFFFDE047),
                                        Color(0xFFF59E0B)
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF38BDF8),
                                        Color(0xFF818CF8),
                                        Color(0xFFC084FC)
                                    )
                                )
                            }

                            drawArc(
                                brush = activeArcBrush,
                                startAngle = 180f,
                                sweepAngle = 180f * celestialProgress,
                                useCenter = false,
                                topLeft = arcTopLeft,
                                size = arcSize,
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )

                            // Sun/Moon Orb position along semi-circle
                            val angleRad = PI * (1.0 - celestialProgress.toDouble())
                            val orbX = centerX + (arcRadius * cos(angleRad)).toFloat()
                            val orbY = horizonY - (arcRadius * sin(angleRad)).toFloat()
                            val orbColor = if (isDaytime) Color(0xFFFDE047) else Color(0xFF7DD3FC)

                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        orbColor.copy(alpha = 0.55f * sunGlow),
                                        Color.Transparent
                                    ),
                                    center = Offset(orbX, orbY),
                                    radius = 10.dp.toPx() * sunGlow
                                ),
                                center = Offset(orbX, orbY),
                                radius = 10.dp.toPx() * sunGlow
                            )
                            drawCircle(
                                color = orbColor,
                                radius = 4.2.dp.toPx(),
                                center = Offset(orbX, orbY)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 1.dp)
                        ) {
                            Icon(
                                imageVector = if (isDaytime) Icons.Default.WbSunny else Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = if (isDaytime) themeColors.goldAccent else themeColors.skyAccent,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isDaytime) {
                                    if (isBangla) "দিবা প্রহর" else "Solar Day"
                                } else {
                                    if (isBangla) "রাত্রি প্রহর" else "Night Watch"
                                },
                                color = themeColors.mutedText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // Live Digital Countdown Pod
                    val countdownSecs = when {
                        !isViewingToday -> 0L
                        forbiddenStatus.isForbidden -> forbiddenStatus.remainingSeconds
                        activeWaqt != null -> activeWaqt.remainingSeconds
                        else -> nextPrayer.remainingSeconds
                    }
                    val countdownTitle = when {
                        !isViewingToday -> if (isBangla) "তারিখ দেখুন" else "DATE PREVIEW"
                        forbiddenStatus.isForbidden -> if (isBangla) "মাকরূহ শেষ হতে" else "MAKRUH ENDS IN"
                        activeWaqt != null -> if (isBangla) "${activeWaqtTitle ?: ""} শেষ হতে" else "${(activeWaqtTitle ?: "").uppercase()} ENDS IN"
                        else -> if (isBangla) "$nextPrayerTitle শুরু হতে" else "${nextPrayerTitle.uppercase()} IN"
                    }
                    val countdownColor = when {
                        !isViewingToday -> themeColors.indigoAccent
                        forbiddenStatus.isForbidden -> themeColors.crimsonAccent
                        activeWaqt != null -> themeColors.goldAccent
                        else -> themeColors.emeraldAccent
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(themeColors.insetPodSurface)
                            .border(
                                1.dp,
                                countdownColor.copy(alpha = if (isDark) 0.42f else 0.35f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = if (isCompactHeight) 4.dp else 5.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = countdownTitle,
                                color = themeColors.secondaryText,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isViewingToday) {
                                    formatHeroCountdown(countdownSecs, isBangla)
                                } else {
                                    DateUtils.formatGregorianHeaderDate(schedule.date, isBangla)
                                },
                                color = countdownColor,
                                fontSize = if (isViewingToday) {
                                    if (isNarrowWidth) 14.5.sp else 15.5.sp
                                } else 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 6.dp else 9.dp))

            // BOTTOM SECTION: 4 Essential Daily Milestones (Sahri Ends • Sunrise • Iftar • Midnight)
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HeroMilestonePill(
                    icon = Icons.Default.WbTwilight,
                    label = if (isBangla) "সেহরির শেষ" else "Sahri Ends",
                    time = DateUtils.localizeTime(schedule.fajr.format(timeFormatter), isBangla),
                    accentColor = themeColors.skyAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_timing_sahri")
                )
                HeroMilestonePill(
                    icon = Icons.Default.WbSunny,
                    label = if (isBangla) "সূর্যোদয়" else "Sunrise",
                    time = DateUtils.localizeTime(schedule.sunrise.format(timeFormatter), isBangla),
                    accentColor = themeColors.goldAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_timing_sunrise")
                )
                HeroMilestonePill(
                    icon = Icons.Default.Restaurant,
                    label = if (isBangla) "ইফতার" else "Iftar",
                    time = DateUtils.localizeTime(schedule.maghrib.format(timeFormatter), isBangla),
                    accentColor = themeColors.emeraldAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_timing_iftar")
                )
                HeroMilestonePill(
                    icon = Icons.Default.Schedule,
                    label = if (isBangla) "মধ্যরাত" else "Midnight",
                    time = DateUtils.localizeTime(schedule.midnight.format(timeFormatter), isBangla),
                    accentColor = themeColors.indigoAccent,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_timing_midnight")
                )
            }
        }
    }
}

fun computeSolarMilestoneCountdownSeconds(
    now: LocalTime,
    todayTarget: LocalTime,
    tomorrowTarget: LocalTime
): Long {
    return if (now.isBefore(todayTarget)) {
        Duration.between(now, todayTarget).seconds.coerceAtLeast(0L)
    } else {
        val untilMidnight = Duration.between(now, LocalTime.MAX).seconds + 1L
        val fromMidnight = Duration.between(LocalTime.MIN, tomorrowTarget).seconds
        (untilMidnight + fromMidnight).coerceAtLeast(0L)
    }
}

@Composable
private fun HeroMilestonePill(
    icon: ImageVector,
    label: String,
    time: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val themeColors = currentPrayerThemeColors()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(11.dp))
            .background(themeColors.insetPodSurface)
            .border(0.8.dp, themeColors.subtleBorder, RoundedCornerShape(11.dp))
            .padding(horizontal = 4.dp, vertical = 5.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(10.dp)
                )
                Spacer(modifier = Modifier.width(2.5.dp))
                Text(
                    text = label,
                    color = themeColors.secondaryText,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = time,
                color = themeColors.primaryText,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatHeroCountdown(totalSeconds: Long, isBangla: Boolean): String {
    val safeSec = totalSeconds.coerceAtLeast(0L)
    val hrs = safeSec / 3600
    val mins = (safeSec % 3600) / 60
    val secs = safeSec % 60
    val formatted = String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
    return DateUtils.localizeDigits(formatted, isBangla)
}

