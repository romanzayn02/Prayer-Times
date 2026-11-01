package com.example.prayer.ui.components

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.prayer.util.DateUtils
import java.time.LocalDate

@Composable
fun CalendarDateDialog(
    selectedDate: LocalDate,
    realTodayDate: LocalDate,
    hijriOffset: Int,
    onSelectDate: (LocalDate) -> Unit,
    onResetToday: () -> Unit,
    onHijriOffsetChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isBangla = isBanglaLanguage()
    val themeColors = currentPrayerThemeColors()
    val hijriStr = remember(selectedDate, hijriOffset, isBangla) {
        DateUtils.getHijriDate(selectedDate, hijriOffset, isBangla)
    }
    val bengaliStr = remember(selectedDate, isBangla) {
        DateUtils.getBengaliDate(selectedDate, isBangla)
    }
    val gregorianStr = remember(selectedDate, isBangla) {
        DateUtils.formatFullGregorianDate(selectedDate, isBangla)
    }
    val isFriday = selectedDate.dayOfWeek == java.time.DayOfWeek.FRIDAY
    val isToday = selectedDate == realTodayDate

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppleLiquidGlassCard(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("calendar_date_dialog"),
            shape = RoundedCornerShape(26.dp),
            tintColor = Color(0xFF818CF8)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            Color(0xFF818CF8).copy(alpha = 0.35f),
                                            Color(0xFF38BDF8).copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .border(1.dp, themeColors.indigoAccent.copy(alpha = 0.65f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = themeColors.indigoAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isBangla) "হিজরি, বাংলা ও ইংরেজি তারিখ" else "Islamic, Bangla & Gregorian Date",
                                color = themeColors.primaryText,
                                fontSize = 16.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (isBangla) "যেকোনো তারিখের নামাজের সময়সূচি দেখুন" else "Browse prayer schedules for any date",
                                color = themeColors.secondaryText,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = if (isBangla) "বন্ধ করুন" else "Close",
                            tint = themeColors.primaryText.copy(alpha = 0.80f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Triple Calendar Summary Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(themeColors.insetPodSurface)
                        .border(1.dp, themeColors.indigoAccent.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isBangla) "হিজরি চান্দ্র বর্ষপঞ্জি" else "HIJRI LUNAR CALENDAR",
                                color = themeColors.skyAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                            if (isFriday) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFDE047).copy(alpha = 0.22f))
                                        .border(0.8.dp, themeColors.goldAccent, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "পবিত্র জুমুআহ" else "JUMU'AH FRIDAY",
                                        color = themeColors.goldAccent,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = hijriStr,
                            color = themeColors.primaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isBangla) "ইংরেজি ও বাংলা বর্ষপঞ্জি" else "GREGORIAN & BANGLA CALENDAR",
                            color = themeColors.emeraldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = gregorianStr,
                            color = themeColors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = bengaliStr,
                            color = themeColors.secondaryText,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Date Stepper Controls
                Text(
                    text = if (isBangla) "তারিখ পরিবর্তন করুন" else "JUMP TO DATE",
                    color = themeColors.indigoAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DateStepChip(
                        label = if (isBangla) "-১ মাস" else "-1 Mo",
                        onClick = { onSelectDate(selectedDate.minusMonths(1)) },
                        modifier = Modifier.weight(1f)
                    )
                    DateStepChip(
                        label = if (isBangla) "-৭ দিন" else "-7 Days",
                        onClick = { onSelectDate(selectedDate.minusWeeks(1)) },
                        modifier = Modifier.weight(1f)
                    )
                    DateStepChip(
                        label = if (isBangla) "-১ দিন" else "-1 Day",
                        onClick = { onSelectDate(selectedDate.minusDays(1)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DateStepChip(
                        label = if (isBangla) "+১ দিন" else "+1 Day",
                        onClick = { onSelectDate(selectedDate.plusDays(1)) },
                        modifier = Modifier.weight(1f)
                    )
                    DateStepChip(
                        label = if (isBangla) "+৭ দিন" else "+7 Days",
                        onClick = { onSelectDate(selectedDate.plusWeeks(1)) },
                        modifier = Modifier.weight(1f)
                    )
                    DateStepChip(
                        label = if (isBangla) "+১ মাস" else "+1 Mo",
                        onClick = { onSelectDate(selectedDate.plusMonths(1)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!isToday) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val todayShort = DateUtils.formatGregorianHeaderDate(realTodayDate, isBangla)
                    Button(
                        onClick = {
                            onResetToday()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("calendar_dialog_reset_today")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = null,
                            tint = Color(0xFF451A03),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isBangla) "আজকের তারিখে ফিরুন ($todayShort)" else "Return to Today ($todayShort)",
                            color = Color(0xFF451A03),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hijri Moon-Sighting Offset
                Text(
                    text = if (isBangla) "হিজরি চাঁদ দেখার সমন্বয়" else "HIJRI MOON-SIGHTING ADJUSTMENT",
                    color = themeColors.skyAccent,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(-2, -1, 0, 1, 2).forEach { offset ->
                        val isSelected = hijriOffset == offset
                        val rawNum = if (offset > 0) "+$offset" else "$offset"
                        val label = if (isBangla) {
                            "${DateUtils.toBengaliDigits(rawNum)} দিন"
                        } else {
                            "$rawNum d"
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) Color(0xFF10B981)
                                    else themeColors.subtleSurface
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color(0xFF6EE7B7) else themeColors.subtleBorder,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onHijriOffsetChange(offset) }
                                .padding(vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color(0xFF022C22) else themeColors.primaryText,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBangla) "সম্পন্ন" else "Done",
                        color = Color(0xFF022C22),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DateStepChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = currentPrayerThemeColors()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(themeColors.subtleSurface)
            .border(1.dp, themeColors.indigoAccent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = themeColors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
