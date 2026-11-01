package com.example.prayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.model.ExpandedCardOptions
import com.example.prayer.model.PrayerType
import com.example.prayer.util.DateUtils
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class VoluntaryPrayerSlot(
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val description: String
)

data class PrayerRowData(
    val type: PrayerType,
    val customTitle: String? = null,
    val startTime: LocalTime,
    val endTime: LocalTime? = null,
    val isNext: Boolean = false,
    val isActive: Boolean = false,
    val activeProgress: Float = 0f,
    val sunnahInfo: String = "",
    val naflNote: String = "",
    val makruhNote: String = "",
    val voluntaryPrayers: List<VoluntaryPrayerSlot> = emptyList(),
    val milestoneLabel: String? = null,
    val milestoneTime: LocalTime? = null,
    val isMilestoneWarning: Boolean = false,
    val minuteOffset: Int = 0
)

@Composable
fun PrayerRowItem(
    data: PrayerRowData,
    isExpanded: Boolean,
    isCompleted: Boolean,
    isAlarmEnabled: Boolean,
    expandedCardOptions: ExpandedCardOptions,
    use24HourFormat: Boolean = false,
    onToggleExpand: () -> Unit,
    onToggleCompleted: () -> Unit,
    onToggleAlarm: () -> Unit,
    onAdjustOffset: ((Int) -> Unit)? = null,
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

    val startStr = DateUtils.localizeTime(data.startTime.format(timeFormatter), isBangla)
    val endStr = data.endTime?.format(timeFormatter)?.let { DateUtils.localizeTime(it, isBangla) }
    val timeDisplay = if (endStr != null) "$startStr – $endStr" else startStr

    val rawTitleText = data.customTitle ?: data.type.displayName
    val titleText = DateUtils.localizePrayerName(rawTitleText, isBangla)

    val subtitleText = buildString {
        if (data.sunnahInfo.isNotEmpty()) {
            append(data.sunnahInfo)
        } else {
            append(data.type.localizedDescription(isBangla))
        }
        if (data.milestoneLabel != null && data.milestoneTime != null) {
            val mTime = DateUtils.localizeTime(data.milestoneTime.format(timeFormatter), isBangla)
            append("  •  ${data.milestoneLabel}: $mTime")
        }
    }

    val iconTint = when {
        data.isActive -> if (isDark) Color(0xFF022C22) else Color.White
        isCompleted -> themeColors.emeraldStrong
        data.isNext -> themeColors.goldAccent
        else -> getPrayerAccentColor(data.type, isDark)
    }

    val cardTintColor = when {
        data.isActive -> Color(0xFF10B981)
        isCompleted -> Color(0xFF059669)
        data.isNext -> Color(0xFFF59E0B)
        else -> getPrayerAccentColor(data.type, isDark)
    }

    val configuration = LocalConfiguration.current
    val isCompactHeight = configuration.screenHeightDp < 700
    val isNarrowWidth = configuration.screenWidthDp < 370
    val canExpand = expandedCardOptions.enableExpansion

    AppleLiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        isActive = data.isActive,
        tintColor = cardTintColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (isNarrowWidth) 10.dp else 12.dp,
                    vertical = if (isCompactHeight) 8.dp else 10.dp
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prayer_row_${data.type.name.lowercase()}")
                    .then(
                        if (canExpand) {
                            Modifier.clickable { onToggleExpand() }
                        } else {
                            Modifier
                        }
                    )
            ) {
                // Left Side: Prayer Icon + Title + NOW/Offset Badges + Subtitle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(if (isNarrowWidth) 30.dp else 33.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    data.isActive -> Brush.linearGradient(
                                        colors = listOf(Color(0xFF34D399), Color(0xFF10B981))
                                    )
                                    isCompleted -> Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF10B981).copy(alpha = 0.25f),
                                            Color(0xFF059669).copy(alpha = 0.20f)
                                        )
                                    )
                                    else -> Brush.linearGradient(
                                        colors = listOf(
                                            cardTintColor.copy(alpha = if (isDark) 0.25f else 0.14f),
                                            cardTintColor.copy(alpha = if (isDark) 0.10f else 0.08f)
                                        )
                                    )
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (data.isActive) themeColors.goldAccent else cardTintColor.copy(alpha = 0.55f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = getPrayerIcon(data.type),
                            contentDescription = titleText,
                            tint = iconTint,
                            modifier = Modifier.size(if (isNarrowWidth) 15.dp else 17.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = titleText,
                                color = if (isCompleted && !data.isActive) {
                                    themeColors.secondaryText
                                } else {
                                    themeColors.primaryText
                                },
                                fontSize = if (isNarrowWidth) 14.5.sp else 15.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textDecoration = if (isCompleted && !data.isActive) TextDecoration.LineThrough else TextDecoration.None,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (data.isActive) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF10B981))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "এখন" else "NOW",
                                        color = Color(0xFF022C22),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            } else if (data.isNext) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = if (isDark) 0.24f else 0.16f))
                                        .border(0.8.dp, themeColors.goldAccent, RoundedCornerShape(5.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "পরবর্তী" else "NEXT",
                                        color = themeColors.goldAccent,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (data.minuteOffset != 0) {
                                val rawOffset = if (data.minuteOffset > 0) "+${data.minuteOffset}" else "${data.minuteOffset}"
                                val offsetText = if (isBangla) {
                                    "${DateUtils.toBengaliDigits(rawOffset)}মি"
                                } else {
                                    "${rawOffset}m"
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.18f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = offsetText,
                                        color = themeColors.skyAccent,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Right Side: Waqt Time Pill + Prayer Done Checkmark
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (isNarrowWidth) 5.dp else 6.dp)
                ) {
                    // Start - End Waqt Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (data.isActive) {
                                    Color(0xFF10B981).copy(alpha = if (isDark) 0.25f else 0.15f)
                                } else {
                                    themeColors.insetPodSurface
                                }
                            )
                            .border(
                                width = if (data.isActive) 1.2.dp else 0.8.dp,
                                color = if (data.isActive) themeColors.emeraldStrong else themeColors.subtleBorder,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(
                                horizontal = if (isNarrowWidth) 7.dp else 9.dp,
                                vertical = if (isCompactHeight) 4.dp else 5.dp
                            )
                    ) {
                        Text(
                            text = timeDisplay,
                            color = if (data.isActive) {
                                if (isDark) themeColors.goldAccent else themeColors.emeraldStrong
                            } else {
                                themeColors.primaryText
                            },
                            fontSize = if (isNarrowWidth) 11.sp else 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Prayer Completion Checkmark Tracker
                    val checkmarkBtnSize = if (isNarrowWidth) 30.dp else 32.dp
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(checkmarkBtnSize)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) {
                                    Color(0xFF10B981)
                                } else {
                                    themeColors.subtleSurface
                                }
                            )
                            .border(
                                width = if (isCompleted) 1.3.dp else 1.dp,
                                color = if (isCompleted) {
                                    themeColors.goldAccent
                                } else {
                                    themeColors.subtleBorder
                                },
                                shape = CircleShape
                            )
                            .clickable { onToggleCompleted() }
                            .testTag("prayer_done_toggle_${data.type.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = if (isCompleted) {
                                "$titleText completed"
                            } else {
                                "Mark $titleText as completed"
                            },
                            tint = if (isCompleted) {
                                Color(0xFF022C22)
                            } else {
                                themeColors.mutedText.copy(alpha = 0.55f)
                            },
                            modifier = Modifier.size(15.5.dp)
                        )
                    }
                }
            }

            // Expandable Details Drawer (ONLY shown when enabled in Settings AND tapped)
            AnimatedVisibility(
                visible = canExpand && isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(themeColors.insetPodSurface)
                        .border(0.8.dp, themeColors.subtleBorder, RoundedCornerShape(13.dp))
                        .padding(10.dp)
                        .testTag("prayer_expanded_${data.type.name.lowercase()}")
                ) {
                    // 1. Sunnah / Farz / Witr Rakat Breakdown
                    if (expandedCardOptions.showRakatBreakdown && data.sunnahInfo.isNotEmpty()) {
                        DetailInfoRow(
                            label = if (isBangla) "রাকাত বিবরণ" else "Rakat Breakdown",
                            value = data.sunnahInfo,
                            valueColor = themeColors.emeraldAccent
                        )
                    }

                    // 2. Solar / Midnight Milestone Cutoff
                    if (expandedCardOptions.showSolarMilestones && data.milestoneLabel != null && data.milestoneTime != null) {
                        Spacer(modifier = Modifier.height(5.dp))
                        DetailInfoRow(
                            label = data.milestoneLabel,
                            value = DateUtils.localizeTime(data.milestoneTime.format(timeFormatter), isBangla),
                            valueColor = if (data.isMilestoneWarning) themeColors.crimsonAccent else themeColors.skyAccent
                        )
                    }

                    // 3. Fiqh Makruh Boundary Warning Note
                    if (expandedCardOptions.showMakruhBoundaryNote && data.makruhNote.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(5.dp))
                        DetailInfoRow(
                            label = if (isBangla) "মাকরূহ ও ফিকহ নোট" else "Fiqh & Makruh Note",
                            value = data.makruhNote,
                            valueColor = themeColors.goldAccent
                        )
                    }

                    // 4. Voluntary / Nafl Prayers in this Waqt window
                    if (expandedCardOptions.showVoluntaryPrayers && data.voluntaryPrayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isBangla) "নফল ও মুস্তাহাব নামাজ" else "VOLUNTARY (NAFL) PRAYERS",
                            color = themeColors.skyAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.7.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        data.voluntaryPrayers.forEach { slot ->
                            val vStart = DateUtils.localizeTime(slot.startTime.format(timeFormatter), isBangla)
                            val vEnd = DateUtils.localizeTime(slot.endTime.format(timeFormatter), isBangla)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = DateUtils.localizePrayerName(slot.name, isBangla),
                                        color = themeColors.primaryText,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = slot.description,
                                        color = themeColors.secondaryText,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = "$vStart – $vEnd",
                                    color = themeColors.emeraldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // 5. Quick Local Mosque Minute Offset Adjuster
                    if (expandedCardOptions.showQuickOffsetAdjuster && onAdjustOffset != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(themeColors.subtleSurface)
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isBangla) "স্থানীয় মসজিদের সময় সমন্বয়" else "Local Mosque Minute Offset",
                                color = themeColors.secondaryText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(themeColors.insetPodSurface)
                                        .border(0.8.dp, themeColors.subtleBorder, CircleShape)
                                        .clickable { onAdjustOffset((data.minuteOffset - 1).coerceIn(-30, 30)) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Remove,
                                        contentDescription = if (isBangla) "১ মিনিট কমান" else "Decrease 1 minute",
                                        tint = themeColors.primaryText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                val rawOff = if (data.minuteOffset > 0) "+${data.minuteOffset}" else "${data.minuteOffset}"
                                Text(
                                    text = if (isBangla) "${DateUtils.toBengaliDigits(rawOff)}মি" else "${rawOff}m",
                                    color = if (data.minuteOffset != 0) themeColors.goldAccent else themeColors.primaryText,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(themeColors.insetPodSurface)
                                        .border(0.8.dp, themeColors.subtleBorder, CircleShape)
                                        .clickable { onAdjustOffset((data.minuteOffset + 1).coerceIn(-30, 30)) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = if (isBangla) "১ মিনিট বাড়ান" else "Increase 1 minute",
                                        tint = themeColors.primaryText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 6. Individual Prayer Alarm & Adhan Toggle
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(themeColors.subtleSurface)
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = if (isBangla) "ওয়াক্ত অ্যালার্ম" else "Waqt Alarm & Adhan",
                            color = themeColors.secondaryText,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = if (isAlarmEnabled) {
                                    if (isBangla) "চালু" else "ON"
                                } else {
                                    if (isBangla) "বন্ধ" else "OFF"
                                },
                                color = if (isAlarmEnabled) themeColors.emeraldAccent else themeColors.mutedText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(if (isAlarmEnabled) themeColors.emeraldStrong else themeColors.insetPodSurface)
                                    .border(0.8.dp, themeColors.subtleBorder, CircleShape)
                                    .clickable { onToggleAlarm() }
                                    .testTag("alarm_toggle_${data.type.name.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = if (isAlarmEnabled) Icons.Default.Alarm else Icons.Default.NotificationsOff,
                                    contentDescription = "$titleText alarm",
                                    tint = if (isAlarmEnabled) Color.White else themeColors.mutedText,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color
) {
    val themeColors = currentPrayerThemeColors()
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            color = themeColors.secondaryText,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun getPrayerIcon(type: PrayerType): ImageVector = when (type) {
    PrayerType.FAJR -> Icons.Default.WbTwilight
    PrayerType.SUNRISE -> Icons.Default.WbSunny
    PrayerType.ISHRAQ -> Icons.Default.WbSunny
    PrayerType.DHUHR -> Icons.Default.WbSunny
    PrayerType.ASR -> Icons.Default.Schedule
    PrayerType.MAGHRIB -> Icons.Default.WbTwilight
    PrayerType.ISHA -> Icons.Default.DarkMode
}

private fun getPrayerAccentColor(type: PrayerType, isDark: Boolean): Color = if (isDark) {
    when (type) {
        PrayerType.FAJR -> Color(0xFF38BDF8)
        PrayerType.SUNRISE -> Color(0xFFFDE047)
        PrayerType.ISHRAQ -> Color(0xFFFDE047)
        PrayerType.DHUHR -> Color(0xFF34D399)
        PrayerType.ASR -> Color(0xFFFBBF24)
        PrayerType.MAGHRIB -> Color(0xFFF472B6)
        PrayerType.ISHA -> Color(0xFF818CF8)
    }
} else {
    when (type) {
        PrayerType.FAJR -> Color(0xFF0284C7)
        PrayerType.SUNRISE -> Color(0xFFD97706)
        PrayerType.ISHRAQ -> Color(0xFFD97706)
        PrayerType.DHUHR -> Color(0xFF059669)
        PrayerType.ASR -> Color(0xFFD97706)
        PrayerType.MAGHRIB -> Color(0xFFDB2777)
        PrayerType.ISHA -> Color(0xFF4F46E5)
    }
}
