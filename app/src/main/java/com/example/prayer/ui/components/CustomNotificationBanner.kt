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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.alarm.CustomBannerNotification
import com.example.prayer.util.DateUtils

@Composable
fun CustomNotificationBanner(
    notification: CustomBannerNotification?,
    onDismiss: () -> Unit,
    onSnooze: ((CustomBannerNotification) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isBangla = isBanglaLanguage()
    val themeColors = currentPrayerThemeColors()

    AnimatedVisibility(
        visible = notification != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        if (notification != null) {
            val accentColor = if (notification.isForbidden) Color(0xFFF43F5E) else Color(0xFF10B981)
            val badgeBgColor = if (notification.isForbidden) Color(0xFFF43F5E) else Color(0xFF10B981)
            val badgeTextColor = if (notification.isForbidden) Color.White else Color(0xFF022C22)

            AppleLiquidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .testTag("custom_notification_banner"),
                shape = RoundedCornerShape(16.dp),
                tintColor = accentColor,
                isActive = !notification.isForbidden
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeBgColor)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (notification.isForbidden) {
                                        Icons.Default.Warning
                                    } else {
                                        Icons.Default.NotificationsActive
                                    },
                                    contentDescription = null,
                                    tint = badgeTextColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = notification.badgeText,
                                    color = badgeTextColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Text(
                                text = DateUtils.localizeTime(notification.timeWindow, isBangla),
                                color = if (notification.isForbidden) themeColors.crimsonAccent else themeColors.emeraldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = notification.title,
                            color = themeColors.primaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = notification.subtitle,
                            color = themeColors.secondaryText,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!notification.isForbidden && onSnooze != null) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(themeColors.subtleSurface)
                                    .border(
                                        1.dp,
                                        themeColors.subtleBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onSnooze(notification) }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .testTag("custom_notification_snooze_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Snooze,
                                        contentDescription = if (isBangla) "৫ মিনিট স্নুজ" else "Snooze 5 minutes",
                                        tint = themeColors.skyAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isBangla) "৫মি" else "5m",
                                        color = themeColors.skyAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Prominent One-Tap Dismiss Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFFF43F5E), Color(0xFFE11D48))
                                    )
                                )
                                .border(
                                    1.dp,
                                    Color(0xFFFDA4AF).copy(alpha = 0.70f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onDismiss() }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("custom_notification_dismiss_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = if (isBangla) "নোটিফিকেশন বন্ধ করুন" else "Dismiss notification",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "বন্ধ" else "Dismiss",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
