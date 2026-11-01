package com.example.prayer.ui.components

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.model.ForbiddenStatus
import com.example.prayer.util.DateUtils
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ForbiddenWarningBox(
    forbiddenStatus: ForbiddenStatus,
    use24HourFormat: Boolean = false,
    onInfoClick: () -> Unit,
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

    if (forbiddenStatus.isForbidden && forbiddenStatus.activeType != null) {
        val totalWindowSeconds = if (forbiddenStatus.startTime != null && forbiddenStatus.endTime != null) {
            (forbiddenStatus.endTime.toSecondOfDay() - forbiddenStatus.startTime.toSecondOfDay())
                .coerceAtLeast(1)
        } else 900
        val elapsedFraction = (1f - (forbiddenStatus.remainingSeconds.toFloat() / totalWindowSeconds.toFloat()))
            .coerceIn(0f, 1f)

        val minsLeft = forbiddenStatus.remainingSeconds / 60
        val secsLeft = forbiddenStatus.remainingSeconds % 60
        val countdownStr = DateUtils.localizeDigits(
            String.format(Locale.US, "%02d:%02d", minsLeft, secsLeft),
            isBangla
        )
        val startFormatted = forbiddenStatus.startTime?.format(timeFormatter)?.let {
            DateUtils.localizeTime(it, isBangla)
        } ?: ""
        val endFormatted = forbiddenStatus.endTime?.format(timeFormatter)?.let {
            DateUtils.localizeTime(it, isBangla)
        } ?: ""

        AppleLiquidGlassWarning(
            modifier = modifier
                .fillMaxWidth()
                .animateContentSize()
                .clickable { onInfoClick() }
                .testTag("forbidden_warning_banner"),
            shape = RoundedCornerShape(15.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE11D48).copy(alpha = if (isDark) 0.45f else 0.16f))
                                .border(1.dp, themeColors.crimsonAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = if (isBangla) "নামাজ নিষিদ্ধ সময়" else "Forbidden Time",
                                tint = if (isDark) Color.White else Color(0xFFBE123C),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isBangla) "মাকরূহ তাহরীমী • নামাজ নিষিদ্ধ" else "MAKRUH TAHRIMI • PROHIBITED",
                                color = themeColors.crimsonAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.6.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${forbiddenStatus.activeType.localizedTitle(isBangla)} ($startFormatted – $endFormatted)",
                                color = if (isDark) Color.White else Color(0xFF881337),
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Countdown Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.85f))
                            .border(1.dp, themeColors.crimsonAccent.copy(alpha = 0.7f), RoundedCornerShape(9.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "-$countdownStr",
                            color = if (isDark) Color(0xFFFECDD3) else Color(0xFFBE123C),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                LinearProgressIndicator(
                    progress = { elapsedFraction },
                    color = if (isDark) Color(0xFFFDA4AF) else Color(0xFFE11D48),
                    trackColor = if (isDark) Color.White.copy(alpha = 0.16f) else Color(0xFF881337).copy(alpha = 0.14f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    } else {
        // Compact Single-Line Permitted Status Bar (Never wraps onto 2 lines)
        val nextForbiddenTimeStr = forbiddenStatus.nextStartTime?.format(timeFormatter)?.let {
            DateUtils.localizeTime(it, isBangla)
        }
        AppleLiquidGlassPill(
            modifier = modifier
                .fillMaxWidth()
                .clickable { onInfoClick() }
                .testTag("permitted_status_banner"),
            shape = RoundedCornerShape(13.dp),
            tintColor = Color(0xFF10B981)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = if (isBangla) "নামাজের বৈধ সময়" else "Prayer Permitted",
                        tint = themeColors.emeraldAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val statusLine = buildString {
                        append(if (isBangla) "নামাজের বৈধ সময়" else "Prayer Permitted")
                        if (nextForbiddenTimeStr != null) {
                            append("  •  ")
                            append(
                                if (isBangla) "পরবর্তী মাকরূহ $nextForbiddenTimeStr"
                                else "Next Makruh $nextForbiddenTimeStr"
                            )
                        }
                    }
                    Text(
                        text = statusLine,
                        color = themeColors.primaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeColors.subtleSurface)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isBangla) "৩টি মাকরূহ সময়" else "3 Makruh Times",
                        color = themeColors.emeraldAccent,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = if (isBangla) "মাকরূহ সময়সূচির নিয়ম" else "Makruh Info",
                        tint = themeColors.emeraldAccent,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}
