package com.example.prayer.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EditLocationAlt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.prayer.alarm.PrayerAlarmScheduler
import com.example.prayer.alarm.PrayerNotificationHelper
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.model.AlarmToneMode
import com.example.prayer.model.AppLanguage
import com.example.prayer.model.AppThemeMode
import com.example.prayer.model.AsrJuristicMethod
import com.example.prayer.model.CalculationMethod
import com.example.prayer.model.ExpandedCardOptions
import com.example.prayer.model.ForbiddenType
import com.example.prayer.model.PrayerSettingsRepository
import com.example.prayer.model.PrayerType
import com.example.prayer.model.UserLocation
import com.example.prayer.ui.components.AppleLiquidGlassCard
import com.example.prayer.ui.components.currentPrayerThemeColors
import com.example.prayer.util.DateUtils
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs

enum class SettingsCategory(val label: String, val banglaLabel: String) {
    ALL("All Settings", "সব সেটিংস"),
    LOCATION("Location & GPS", "অবস্থান ও জিপিএস"),
    CALCULATION("Calculation & Fiqh", "গণনা ও ফিকহ"),
    CALIBRATION("Minute Offsets", "মিনিট সমন্বয়"),
    ALARMS("Alarms & Audio", "এলার্ম ও শব্দ"),
    DISPLAY("Language & Theme", "ভাষা ও থিম");

    fun localizedLabel(isBangla: Boolean): String = if (isBangla) banglaLabel else label
}

@Composable
fun SettingsScreen(
    currentLocation: UserLocation,
    isRefreshingGps: Boolean,
    onRefreshGps: () -> Unit,
    onOpenLocationPicker: () -> Unit,
    onSettingsChanged: () -> Unit,
    onNavigateBack: () -> Unit,
    initialCategory: SettingsCategory = SettingsCategory.ALL
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val themeColors = currentPrayerThemeColors()
    val isDark = themeColors.isDark
    var selectedCategory by remember { mutableStateOf(initialCategory) }

    // Reactive local state synced with PrayerSettingsRepository & PrayerAlarmScheduler
    var appLanguage by remember {
        mutableStateOf(PrayerSettingsRepository.getAppLanguage(context))
    }
    val isBangla = appLanguage == AppLanguage.BANGLA

    var calculationMethod by remember {
        mutableStateOf(PrayerSettingsRepository.getCalculationMethod(context))
    }
    var asrJuristicMethod by remember {
        mutableStateOf(PrayerSettingsRepository.getAsrJuristicMethod(context))
    }
    var minuteOffsets by remember {
        mutableStateOf(PrayerSettingsRepository.getMinuteOffsets(context))
    }
    var alarmToneMode by remember {
        mutableStateOf(PrayerSettingsRepository.getAlarmToneMode(context))
    }
    var appThemeMode by remember {
        mutableStateOf(PrayerSettingsRepository.getAppThemeMode(context))
    }
    var expandedCardOptions by remember {
        mutableStateOf(PrayerSettingsRepository.getExpandedCardOptions(context))
    }
    var use24HourFormat by remember {
        mutableStateOf(PrayerSettingsRepository.is24HourFormat(context))
    }
    var autoJumuahFriday by remember {
        mutableStateOf(PrayerSettingsRepository.isAutoJumuahEnabled(context))
    }
    var hijriOffset by remember {
        mutableIntStateOf(DateUtils.getHijriOffset(context))
    }
    var salatAlarmsEnabled by remember {
        mutableStateOf(PrayerAlarmScheduler.isSalatAlarmsEnabled(context))
    }
    var forbiddenRemindersEnabled by remember {
        mutableStateOf(PrayerAlarmScheduler.isForbiddenRemindersEnabled(context))
    }

    val individualPrayerAlarms = remember {
        mutableStateMapOf<PrayerType, Boolean>().apply {
            PrayerSettingsRepository.fiveDailyPrayers.forEach {
                put(it, PrayerAlarmScheduler.isPrayerAlarmEnabled(context, it))
            }
        }
    }

    val notificationsGranted = remember {
        PrayerNotificationHelper.areSystemNotificationsEnabled(context)
    }
    val exactAlarmsGranted = remember {
        PrayerAlarmScheduler.canScheduleExactAlarms(context)
    }
    val fullScreenGranted = remember {
        PrayerNotificationHelper.canUseFullScreenIntent(context)
    }

    val resolvedZone = remember(currentLocation) {
        PrayerCalculator.resolveZoneId(currentLocation.latitude, currentLocation.longitude)
    }
    val resolvedOffsetHours = remember(currentLocation) {
        PrayerCalculator.resolveTimeZoneOffset(LocalDate.now(), currentLocation.latitude, currentLocation.longitude)
    }
    val offsetFormatted = remember(resolvedOffsetHours, isBangla) {
        val sign = if (resolvedOffsetHours >= 0) "+" else "-"
        val totalMinutes = (abs(resolvedOffsetHours) * 60.0).toInt()
        val hrs = totalMinutes / 60
        val mins = totalMinutes % 60
        val raw = String.format(Locale.US, "UTC%s%02d:%02d", sign, hrs, mins)
        DateUtils.localizeDigits(raw, isBangla)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("settings_screen")
    ) {
        // High-Visibility Sticky Commercial Top Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(themeColors.topBarSurface)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(themeColors.subtleSurface)
                            .border(1.dp, themeColors.subtleBorder, CircleShape)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isBangla) "হোম স্ক্রিনে ফিরুন" else "Back to Dashboard",
                            tint = themeColors.primaryText,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "সেটিংস ও সময় সমন্বয়" else "Settings & Calibration",
                            color = themeColors.primaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${calculationMethod.localizedShortName(isBangla)} • ${asrJuristicMethod.localizedShortName(isBangla)} • $offsetFormatted",
                            color = themeColors.emeraldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Reset to Defaults Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(themeColors.subtleSurface)
                        .border(1.dp, themeColors.subtleBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            PrayerSettingsRepository.resetAllSettingsToDefaults(context)
                            appLanguage = PrayerSettingsRepository.getAppLanguage(context)
                            calculationMethod = PrayerSettingsRepository.getCalculationMethod(context)
                            asrJuristicMethod = PrayerSettingsRepository.getAsrJuristicMethod(context)
                            minuteOffsets = PrayerSettingsRepository.getMinuteOffsets(context)
                            alarmToneMode = PrayerSettingsRepository.getAlarmToneMode(context)
                            appThemeMode = PrayerSettingsRepository.getAppThemeMode(context)
                            expandedCardOptions = PrayerSettingsRepository.getExpandedCardOptions(context)
                            use24HourFormat = PrayerSettingsRepository.is24HourFormat(context)
                            autoJumuahFriday = PrayerSettingsRepository.isAutoJumuahEnabled(context)
                            hijriOffset = DateUtils.getHijriOffset(context)
                            salatAlarmsEnabled = PrayerAlarmScheduler.isSalatAlarmsEnabled(context)
                            forbiddenRemindersEnabled = PrayerAlarmScheduler.isForbiddenRemindersEnabled(context)
                            PrayerSettingsRepository.fiveDailyPrayers.forEach {
                                individualPrayerAlarms[it] = true
                            }
                            onSettingsChanged()
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("reset_all_settings_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = if (isBangla) "ডিফল্টে রিসেট করুন" else "Reset Defaults",
                            tint = themeColors.goldAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBangla) "রিসেট" else "Reset",
                            color = themeColors.goldAccent,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Organized Category Filter Tabs
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                SettingsCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (isSelected) Color(0xFF10B981)
                                else themeColors.subtleSurface
                            )
                            .border(
                                1.dp,
                                if (isSelected) Color(0xFF34D399) else themeColors.subtleBorder,
                                RoundedCornerShape(50)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                            .testTag("settings_tab_${category.name.lowercase()}")
                    ) {
                        Text(
                            text = category.localizedLabel(isBangla),
                            color = if (isSelected) Color(0xFF022C22) else themeColors.primaryText,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(themeColors.topBarBorder)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Scrollable Organized Sections
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 28.dp),
            modifier = Modifier
                .fillMaxSize()
                .testTag("settings_scroll_list")
        ) {
            // 1. LOCATION & COORDINATES SECTION
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.LOCATION) {
                item {
                    SettingsSectionCard(
                        sectionNumber = if (isBangla) "০১" else "01",
                        title = if (isBangla) "অবস্থান ও সৌর স্থানাঙ্ক" else "Location & Solar Coordinates",
                        subtitle = if (isBangla) "জিপিএস অটো-সিঙ্ক, শহর নির্বাচন ও টাইমজোন সমন্বয়" else "GPS auto-sync, city presets & timezone calibration",
                        icon = Icons.Default.LocationOn,
                        accentColor = Color(0xFF38BDF8),
                        isBangla = isBangla
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(themeColors.insetPodSurface)
                                .border(1.dp, themeColors.skyAccent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = DateUtils.localizeLocationName(currentLocation.locationName, isBangla),
                                        color = themeColors.primaryText,
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(
                                                if (currentLocation.isGps) Color(0xFF10B981).copy(alpha = 0.20f)
                                                else Color(0xFF38BDF8).copy(alpha = 0.20f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (currentLocation.isGps) {
                                                if (isBangla) "লাইভ জিপিএস" else "LIVE GPS"
                                            } else {
                                                if (isBangla) "নির্ধারিত শহর" else "PRESET / MANUAL"
                                            },
                                            color = if (currentLocation.isGps) themeColors.emeraldAccent else themeColors.skyAccent,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                val latLngFormatted = DateUtils.localizeDigits(
                                    String.format(
                                        Locale.US,
                                        "Lat: %.4f° N  •  Lng: %.4f° E",
                                        currentLocation.latitude,
                                        currentLocation.longitude
                                    ),
                                    isBangla
                                )
                                Text(
                                    text = latLngFormatted,
                                    color = themeColors.emeraldAccent,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) {
                                        "সক্রিয় টাইমজোন: ${resolvedZone.id} ($offsetFormatted)"
                                    } else {
                                        "Active Timezone: ${resolvedZone.id} ($offsetFormatted)"
                                    },
                                    color = themeColors.secondaryText,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onRefreshGps,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_refresh_gps_button")
                            ) {
                                if (isRefreshingGps) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF022C22),
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(15.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = Color(0xFF022C22),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isRefreshingGps) {
                                        if (isBangla) "সিঙ্ক হচ্ছে..." else "Syncing..."
                                    } else {
                                        if (isBangla) "জিপিএস সিঙ্ক" else "Sync GPS"
                                    },
                                    color = Color(0xFF022C22),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            OutlinedButton(
                                onClick = onOpenLocationPicker,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("settings_change_location_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditLocationAlt,
                                    contentDescription = null,
                                    tint = themeColors.skyAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBangla) "শহর পরিবর্তন" else "Change City",
                                    color = themeColors.skyAccent,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 2. CALCULATION AUTHORITY & FIQH JURISPRUDENCE SECTION
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.CALCULATION) {
                item {
                    SettingsSectionCard(
                        sectionNumber = if (isBangla) "০২" else "02",
                        title = if (isBangla) "গণনা পদ্ধতি ও ফিকহ (মাযহাব)" else "Calculation Method & Fiqh",
                        subtitle = if (isBangla) "জ্যোতির্বিজ্ঞান মানদণ্ড ও আসরের ছায়ার ফিকহী হিসাব" else "Astronomical twilight authority & Asr shadow jurisprudence",
                        icon = Icons.Default.CompassCalibration,
                        accentColor = Color(0xFF10B981),
                        isBangla = isBangla
                    ) {
                        Text(
                            text = if (isBangla) "আসরের মাযহাব (ছায়ার হিসাব)" else "ASR JURISTIC METHOD (MADHAB)",
                            color = themeColors.emeraldAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AsrJuristicMethod.entries.forEach { method ->
                                val isSelected = asrJuristicMethod == method
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFF10B981).copy(alpha = if (isDark) 0.25f else 0.16f)
                                            else themeColors.subtleSurface
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) themeColors.emeraldStrong else themeColors.subtleBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            asrJuristicMethod = method
                                            PrayerSettingsRepository.setAsrJuristicMethod(context, method)
                                            onSettingsChanged()
                                        }
                                        .padding(10.dp)
                                        .testTag("juristic_method_${method.name.lowercase()}")
                                ) {
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = method.localizedShortName(isBangla),
                                                color = themeColors.primaryText,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = if (isBangla) "নির্বাচিত" else "Selected",
                                                    tint = themeColors.emeraldStrong,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = method.localizedDescription(isBangla),
                                            color = themeColors.secondaryText,
                                            fontSize = 10.5.sp,
                                            lineHeight = 13.5.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = if (isBangla) "সুবহে সাদিক ও শফক গণনা কর্তৃপক্ষ" else "TWILIGHT CALCULATION AUTHORITY",
                            color = themeColors.emeraldAccent,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CalculationMethod.entries.forEach { method ->
                                val isSelected = calculationMethod == method
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFF10B981).copy(alpha = if (isDark) 0.22f else 0.14f)
                                            else themeColors.subtleSurface
                                        )
                                        .border(
                                            width = if (isSelected) 1.4.dp else 1.dp,
                                            color = if (isSelected) themeColors.emeraldStrong else themeColors.subtleBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            calculationMethod = method
                                            PrayerSettingsRepository.setCalculationMethod(context, method)
                                            onSettingsChanged()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                        .testTag("calc_method_${method.name.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = method.localizedFullTitle(isBangla),
                                            color = themeColors.primaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = method.localizedDescription(isBangla),
                                            color = themeColors.secondaryText,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = if (isBangla) "নির্বাচিত" else "Selected",
                                            tint = themeColors.emeraldStrong,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. LOCAL MOSQUE MINUTE CALIBRATION (FINE-TUNE OFFSETS)
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.CALIBRATION) {
                item {
                    SettingsSectionCard(
                        sectionNumber = if (isBangla) "০৩" else "03",
                        title = if (isBangla) "স্থানীয় মসজিদের মিনিট সমন্বয়" else "Local Mosque Minute Calibration",
                        subtitle = if (isBangla) "স্থানীয় মসজিদের জামাতের সাথে মিল রাখতে সময় সূক্ষ্ম সমন্বয় (±৩০ মিনিট)" else "Fine-tune individual prayer times (±30 min) to match your local Jamaat",
                        icon = Icons.Default.Tune,
                        accentColor = Color(0xFFFBBF24),
                        isBangla = isBangla
                    ) {
                        val offsetItems = listOf(
                            Triple(PrayerType.FAJR, if (isBangla) "ফজর / সেহরির শেষ" else "Fazr / Sahri Ends", minuteOffsets.fajr),
                            Triple(PrayerType.SUNRISE, if (isBangla) "সূর্যোদয় (শুরুক)" else "Sunrise (Shuruk)", minuteOffsets.sunrise),
                            Triple(PrayerType.DHUHR, if (isBangla) "যোহর / জুমুআহ" else "Duhr / Jumu'ah", minuteOffsets.dhuhr),
                            Triple(PrayerType.ASR, if (isBangla) "আসর" else "Asr", minuteOffsets.asr),
                            Triple(PrayerType.MAGHRIB, if (isBangla) "মাগরিব / ইফতার" else "Maghrib / Iftar", minuteOffsets.maghrib),
                            Triple(PrayerType.ISHA, if (isBangla) "ইশা" else "Isha", minuteOffsets.isha)
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            offsetItems.forEach { (prayerType, label, currentOffset) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(themeColors.subtleSurface)
                                        .border(1.dp, themeColors.subtleBorder, RoundedCornerShape(11.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = label,
                                        color = themeColors.primaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(themeColors.insetPodSurface)
                                                .border(0.8.dp, themeColors.subtleBorder, CircleShape)
                                                .clickable {
                                                    val next = (currentOffset - 1).coerceIn(-30, 30)
                                                    PrayerSettingsRepository.setPrayerMinuteOffset(context, prayerType, next)
                                                    minuteOffsets = PrayerSettingsRepository.getMinuteOffsets(context)
                                                    onSettingsChanged()
                                                }
                                                .testTag("offset_minus_${prayerType.name.lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Decrease ${prayerType.displayName} offset",
                                                tint = themeColors.primaryText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        val rawOff = if (currentOffset > 0) "+$currentOffset" else "$currentOffset"
                                        val offDisplay = if (isBangla) {
                                            "${DateUtils.toBengaliDigits(rawOff)}মি"
                                        } else {
                                            "${rawOff}m"
                                        }
                                        Text(
                                            text = offDisplay,
                                            color = if (currentOffset != 0) themeColors.goldAccent else themeColors.secondaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )

                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(themeColors.insetPodSurface)
                                                .border(0.8.dp, themeColors.subtleBorder, CircleShape)
                                                .clickable {
                                                    val next = (currentOffset + 1).coerceIn(-30, 30)
                                                    PrayerSettingsRepository.setPrayerMinuteOffset(context, prayerType, next)
                                                    minuteOffsets = PrayerSettingsRepository.getMinuteOffsets(context)
                                                    onSettingsChanged()
                                                }
                                                .testTag("offset_plus_${prayerType.name.lowercase()}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Increase ${prayerType.displayName} offset",
                                                tint = themeColors.primaryText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (minuteOffsets.hasAnyOffset()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    PrayerSettingsRepository.resetAllMinuteOffsets(context)
                                    minuteOffsets = PrayerSettingsRepository.getMinuteOffsets(context)
                                    onSettingsChanged()
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reset_minute_offsets_button")
                            ) {
                                Text(
                                    text = if (isBangla) "সকল মিনিট সমন্বয় ০ মিনিটে রিসেট করুন" else "Reset All Minute Offsets to 0m",
                                    color = themeColors.goldAccent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 4. ALARMS, AUDIO MODE & LOCKSCREEN ALERTS SECTION
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.ALARMS) {
                item {
                    SettingsSectionCard(
                        sectionNumber = if (isBangla) "০৪" else "04",
                        title = if (isBangla) "ওয়েক-আপ এলার্ম ও শব্দ নিয়ন্ত্রণ" else "Wake-Up Alarms & Audio Behavior",
                        subtitle = if (isBangla) "লকস্ক্রিন এলার্ট, ধ্বনি/ভাইব্রেশন মোড ও মাকরূহ সতর্কতা" else "Lock screen wake-up, sound/vibration mode & Makruh alerts",
                        icon = Icons.Default.NotificationsActive,
                        accentColor = Color(0xFF10B981),
                        isBangla = isBangla
                    ) {
                        // Master Salat Alarms Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = themeColors.emeraldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) "৫ ওয়াক্ত নামাজের ওয়েক-আপ এলার্ম" else "5 Daily Salat Wake-Up Alarms",
                                        color = themeColors.primaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBangla) "স্ক্রিন বন্ধ থাকলে ফুল-স্ক্রিন এলার্ট • স্ক্রিন খোলা থাকলে ব্যানার" else "Full-screen lockscreen wake when off • Banner when on",
                                        color = themeColors.secondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = salatAlarmsEnabled,
                                onCheckedChange = { checked ->
                                    salatAlarmsEnabled = checked
                                    PrayerAlarmScheduler.setSalatAlarmsEnabled(context, checked)
                                    onSettingsChanged()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF022C22),
                                    checkedTrackColor = Color(0xFF34D399),
                                    uncheckedThumbColor = themeColors.secondaryText,
                                    uncheckedTrackColor = themeColors.subtleSurface
                                ),
                                modifier = Modifier.testTag("salat_alarms_master_switch")
                            )
                        }

                        if (salatAlarmsEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                PrayerSettingsRepository.fiveDailyPrayers.forEach { prayer ->
                                    val isEnabled = individualPrayerAlarms[prayer] ?: true
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                if (isEnabled) Color(0xFF10B981).copy(alpha = if (isDark) 0.22f else 0.16f)
                                                else themeColors.subtleSurface
                                            )
                                            .border(
                                                width = if (isEnabled) 1.2.dp else 0.8.dp,
                                                color = if (isEnabled) themeColors.emeraldStrong else themeColors.subtleBorder,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable {
                                                val next = !isEnabled
                                                individualPrayerAlarms[prayer] = next
                                                PrayerAlarmScheduler.setPrayerAlarmEnabled(context, prayer, next)
                                                onSettingsChanged()
                                            }
                                            .padding(vertical = 7.dp, horizontal = 4.dp)
                                            .testTag("settings_alarm_pill_${prayer.name.lowercase()}")
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = if (isEnabled) Icons.Default.Alarm else Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = if (isEnabled) themeColors.emeraldAccent else themeColors.mutedText,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = prayer.localizedName(isBangla),
                                                color = if (isEnabled) themeColors.primaryText else themeColors.mutedText,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Forbidden Time Reminders Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = themeColors.goldAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) "মাকরূহ (নিষিদ্ধ সময়) সতর্কতা" else "Forbidden Time (Makruh) Alerts",
                                        color = themeColors.primaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isBangla) "সূর্যোদয়, জাওয়াল ও সূর্যাস্তের নিষিদ্ধ সময়ে সতর্ক করবে" else "Alerts at Sunrise, Zawal & Pre-Sunset prohibited times",
                                        color = themeColors.secondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = forbiddenRemindersEnabled,
                                onCheckedChange = { checked ->
                                    forbiddenRemindersEnabled = checked
                                    PrayerAlarmScheduler.setForbiddenRemindersEnabled(context, checked)
                                    onSettingsChanged()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF022C22),
                                    checkedTrackColor = Color(0xFFFBBF24),
                                    uncheckedThumbColor = themeColors.secondaryText,
                                    uncheckedTrackColor = themeColors.subtleSurface
                                ),
                                modifier = Modifier.testTag("forbidden_reminders_master_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Alarm Audio & Vibration Mode Selector
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = themeColors.emeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "এলার্মের শব্দ ও ভাইব্রেশন মোড" else "ALARM SOUND & VIBRATION MODE",
                                color = themeColors.emeraldAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AlarmToneMode.entries.forEach { mode ->
                                val isSelected = alarmToneMode == mode
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(11.dp))
                                        .background(
                                            if (isSelected) Color(0xFF10B981).copy(alpha = if (isDark) 0.22f else 0.14f)
                                            else themeColors.subtleSurface
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) themeColors.emeraldStrong else themeColors.subtleBorder,
                                            RoundedCornerShape(11.dp)
                                        )
                                        .clickable {
                                            alarmToneMode = mode
                                            PrayerSettingsRepository.setAlarmToneMode(context, mode)
                                            onSettingsChanged()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("alarm_tone_mode_${mode.name.lowercase()}")
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mode.localizedTitle(isBangla),
                                            color = themeColors.primaryText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = mode.localizedSubtitle(isBangla),
                                            color = themeColors.secondaryText,
                                            fontSize = 10.5.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = if (isBangla) "নির্বাচিত" else "Selected",
                                            tint = themeColors.emeraldStrong,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // System Permissions Status Strip
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(themeColors.insetPodSurface)
                                .border(1.dp, themeColors.subtleBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (isBangla) "সিস্টেম এলার্ম প্রস্তুতি" else "SYSTEM ALARM READINESS",
                                    color = themeColors.skyAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.8.sp
                                )
                                PermissionStatusRow(
                                    label = if (isBangla) "ওএস নোটিফিকেশন" else "OS Notifications",
                                    isGranted = notificationsGranted,
                                    isBangla = isBangla,
                                    onFixClick = {
                                        try {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Throwable) {
                                        }
                                    }
                                )
                                PermissionStatusRow(
                                    label = if (isBangla) "সঠিক সময়ে এলার্ম শিডিউলিং" else "Exact Alarm Clock Scheduling",
                                    isGranted = exactAlarmsGranted,
                                    isBangla = isBangla,
                                    onFixClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            try {
                                                context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                )
                                            } catch (_: Throwable) {
                                            }
                                        }
                                    }
                                )
                                PermissionStatusRow(
                                    label = if (isBangla) "লকস্ক্রিন ফুল-স্ক্রিন ওয়েক-আপ" else "Lockscreen Full-Screen Wake-Up",
                                    isGranted = fullScreenGranted,
                                    isBangla = isBangla,
                                    onFixClick = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                            try {
                                                context.startActivity(
                                                    Intent(
                                                        Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                                        Uri.parse("package:${context.packageName}")
                                                    )
                                                )
                                            } catch (_: Throwable) {
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Diagnostic Preview Buttons
                        Button(
                            onClick = {
                                PrayerAlarmScheduler.triggerTestSalatAlarm(
                                    context = context,
                                    prayerName = "Fazr",
                                    forceFullScreenPreview = true
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981).copy(alpha = if (isDark) 0.25f else 0.16f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, themeColors.emeraldStrong.copy(alpha = 0.70f), RoundedCornerShape(12.dp))
                                .testTag("test_fullscreen_alarm_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fullscreen,
                                contentDescription = null,
                                tint = themeColors.emeraldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "ফুল-স্ক্রিন লকস্ক্রিন এলার্ট প্রিভিউ" else "Preview Full-Screen Lockscreen Alert",
                                color = themeColors.emeraldAccent,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    PrayerAlarmScheduler.triggerTestSalatAlarm(
                                        context = context,
                                        prayerName = "Fazr",
                                        forceFullScreenPreview = false
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.emeraldAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_banner_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = themeColors.emeraldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "টেস্ট নামাজ ব্যানার" else "Test Salat Banner",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    PrayerAlarmScheduler.triggerTestForbiddenNotification(
                                        context,
                                        ForbiddenType.POST_SUNRISE,
                                        forceFullScreenPreview = false
                                    )
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.goldAccent),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("test_forbidden_notification_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = themeColors.goldAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isBangla) "টেস্ট মাকরূহ এলার্ট" else "Test Makruh Alert",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 5. LANGUAGE, APPEARANCE, THEME, PRAYER CARDS & CALENDAR SECTION
            if (selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.DISPLAY) {
                item {
                    SettingsSectionCard(
                        sectionNumber = if (isBangla) "০৫" else "05",
                        title = if (isBangla) "ভাষা, থিম, নামাজ কার্ড ও বর্ষপঞ্জি" else "Language, Theme, Prayer Cards & Calendar",
                        subtitle = if (isBangla) "বাংলা/ইংরেজি ভাষা, লাইট/ডার্ক থিম, কার্ডের বিস্তারিত ও হিজরি তারিখ" else "App language (English/Bangla), Light/Dark theme, expandable prayer card options & Hijri date",
                        icon = Icons.Default.Language,
                        accentColor = Color(0xFF818CF8),
                        isBangla = isBangla
                    ) {
                        // A0. App Language Selector (English / বাংলা)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = themeColors.emeraldAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "অ্যাপের ভাষা (APP LANGUAGE)" else "APP LANGUAGE (অ্যাপের ভাষা)",
                                color = themeColors.emeraldAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val isSelected = appLanguage == lang
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFF10B981).copy(alpha = if (isDark) 0.25f else 0.18f)
                                            else themeColors.subtleSurface
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) themeColors.emeraldStrong else themeColors.subtleBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            appLanguage = lang
                                            PrayerSettingsRepository.setAppLanguage(context, lang)
                                            onSettingsChanged()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 9.dp)
                                        .testTag("language_mode_${lang.name.lowercase()}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = if (lang == AppLanguage.BANGLA) "বাংলা (Bangla)" else "English",
                                                color = themeColors.primaryText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = lang.subtitle,
                                                color = if (isSelected) themeColors.emeraldAccent else themeColors.secondaryText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = if (isBangla) "নির্বাচিত" else "Selected",
                                                tint = themeColors.emeraldStrong,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // A. Theme Mode Selector (Dark Night / Light Daylight / System Auto)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null,
                                tint = themeColors.indigoAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "অ্যাপের রঙ ও থিম" else "APPEARANCE & COLOR THEME",
                                color = themeColors.indigoAccent,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AppThemeMode.entries.forEach { mode ->
                                val isSelected = appThemeMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Color(0xFF10B981).copy(alpha = if (isDark) 0.25f else 0.18f)
                                            else themeColors.subtleSurface
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) themeColors.emeraldStrong else themeColors.subtleBorder,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            appThemeMode = mode
                                            PrayerSettingsRepository.setAppThemeMode(context, mode)
                                            onSettingsChanged()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 9.dp)
                                        .testTag("theme_mode_${mode.name.lowercase()}")
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = mode.localizedTitle(isBangla),
                                            color = themeColors.primaryText,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = when (mode) {
                                                AppThemeMode.DARK -> if (isBangla) "রাতের আবহ" else "Celestial"
                                                AppThemeMode.LIGHT -> if (isBangla) "দিনের আলো" else "Alabaster"
                                                AppThemeMode.SYSTEM -> if (isBangla) "ডিভাইস" else "Device"
                                            },
                                            color = if (isSelected) themeColors.emeraldAccent else themeColors.secondaryText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // B. Expandable Prayer Cards Option (OFF by default) + Sub-content toggles
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(themeColors.insetPodSurface)
                                .border(1.dp, themeColors.subtleBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.UnfoldMore,
                                            contentDescription = null,
                                            tint = themeColors.emeraldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = if (isBangla) "প্রসারণযোগ্য নামাজ কার্ড" else "Expandable Prayer Cards",
                                                color = themeColors.primaryText,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = if (isBangla) {
                                                    "হোম স্ক্রিনে নামাজ কার্ডে ট্যাপ করে রাকাত, মাকরূহ ও নফল বিবরণ দেখুন (ডিফল্টভাবে বন্ধ)"
                                                } else {
                                                    "Tap any prayer card on home screen to view Rakats, Makruh & Nafl details (Off by default)"
                                                },
                                                color = themeColors.secondaryText,
                                                fontSize = 11.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }

                                    Switch(
                                        checked = expandedCardOptions.enableExpansion,
                                        onCheckedChange = { checked ->
                                            val updated = expandedCardOptions.copy(enableExpansion = checked)
                                            expandedCardOptions = updated
                                            PrayerSettingsRepository.setExpandedCardOptions(context, updated)
                                            onSettingsChanged()
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color(0xFF022C22),
                                            checkedTrackColor = Color(0xFF34D399),
                                            uncheckedThumbColor = themeColors.secondaryText,
                                            uncheckedTrackColor = themeColors.subtleSurface
                                        ),
                                        modifier = Modifier.testTag("settings_expand_cards_switch")
                                    )
                                }

                                AnimatedVisibility(visible = expandedCardOptions.enableExpansion) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 10.dp)
                                    ) {
                                        Text(
                                            text = if (isBangla) "প্রসারিত কার্ডের বিষয়বস্তু ও অপশন" else "EXPANDED CARD CONTENTS & OPTIONS",
                                            color = themeColors.emeraldAccent,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.8.sp
                                        )

                                        ExpandedContentOptionRow(
                                            title = if (isBangla) "সুন্নত, ফরজ ও বিতর রাকাত বিবরণ" else "Sunnah, Farz & Witr Rakat Breakdown",
                                            checked = expandedCardOptions.showRakatBreakdown,
                                            onCheckedChange = {
                                                val updated = expandedCardOptions.copy(showRakatBreakdown = it)
                                                expandedCardOptions = updated
                                                PrayerSettingsRepository.setExpandedCardOptions(context, updated)
                                                onSettingsChanged()
                                            },
                                            testTag = "expand_opt_rakats"
                                        )
                                        ExpandedContentOptionRow(
                                            title = if (isBangla) "সূর্যোদয়, সূর্যাস্ত ও মধ্যরাত সময়সীমা" else "Sunrise, Sunset & Islamic Midnight Cutoffs",
                                            checked = expandedCardOptions.showSolarMilestones,
                                            onCheckedChange = {
                                                val updated = expandedCardOptions.copy(showSolarMilestones = it)
                                                expandedCardOptions = updated
                                                PrayerSettingsRepository.setExpandedCardOptions(context, updated)
                                                onSettingsChanged()
                                            },
                                            testTag = "expand_opt_milestones"
                                        )
                                        ExpandedContentOptionRow(
                                            title = if (isBangla) "ফিকহী মাকরূহ সময়সীমা সতর্কতা নোট" else "Fiqh Makruh Boundary Warning Notes",
                                            checked = expandedCardOptions.showMakruhBoundaryNote,
                                            onCheckedChange = {
                                                val updated = expandedCardOptions.copy(showMakruhBoundaryNote = it)
                                                expandedCardOptions = updated
                                                PrayerSettingsRepository.setExpandedCardOptions(context, updated)
                                                onSettingsChanged()
                                            },
                                            testTag = "expand_opt_makruh"
                                        )
                                        ExpandedContentOptionRow(
                                            title = if (isBangla) "নফল ও মুস্তাহাব নামাজ (তাহাজ্জুদ, ইশরাক, আওয়াবীন)" else "Voluntary & Nafl Prayers (Tahajjud, Ishraq, Awwabin)",
                                            checked = expandedCardOptions.showVoluntaryPrayers,
                                            onCheckedChange = {
                                                val updated = expandedCardOptions.copy(showVoluntaryPrayers = it)
                                                expandedCardOptions = updated
                                                PrayerSettingsRepository.setExpandedCardOptions(context, updated)
                                                onSettingsChanged()
                                            },
                                            testTag = "expand_opt_voluntary"
                                        )
                                        ExpandedContentOptionRow(
                                            title = if (isBangla) "কার্ডের ভেতর মসজিদের মিনিট সমন্বয় (±১ মিনিট)" else "Inline Mosque Minute Calibration (±1m Stepper)",
                                            checked = expandedCardOptions.showQuickOffsetAdjuster,
                                            onCheckedChange = {
                                                val updated = expandedCardOptions.copy(showQuickOffsetAdjuster = it)
                                                expandedCardOptions = updated
                                                PrayerSettingsRepository.setExpandedCardOptions(context, updated)
                                                onSettingsChanged()
                                            },
                                            testTag = "expand_opt_offset"
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // C. 24-Hour Format Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = themeColors.indigoAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (isBangla) "২৪-ঘণ্টা সময় ফরম্যাট (১৪:৩০)" else "24-Hour Time Format (14:30)",
                                        color = themeColors.primaryText,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (use24HourFormat) {
                                            if (isBangla) "সকল সময় ২৪-ঘণ্টা ফরম্যাটে দেখানো হচ্ছে" else "Showing 24-hour military clock across app"
                                        } else {
                                            if (isBangla) "সকল সময় ১২-ঘণ্টা (AM/PM) ফরম্যাটে দেখানো হচ্ছে" else "Showing standard 12-hour AM/PM clock"
                                        },
                                        color = themeColors.secondaryText,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Switch(
                                checked = use24HourFormat,
                                onCheckedChange = { checked ->
                                    use24HourFormat = checked
                                    PrayerSettingsRepository.set24HourFormat(context, checked)
                                    onSettingsChanged()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF022C22),
                                    checkedTrackColor = Color(0xFF818CF8),
                                    uncheckedThumbColor = themeColors.secondaryText,
                                    uncheckedTrackColor = themeColors.subtleSurface
                                ),
                                modifier = Modifier.testTag("use_24_hour_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // D. Automatic Friday Jumu'ah Mode Switch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isBangla) "স্বয়ংক্রিয় শুক্রবার জুমুআহ মোড" else "Automatic Friday Jumu'ah Mode",
                                    color = themeColors.primaryText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isBangla) "শুক্রবারে যোহরের পরিবর্তে স্বয়ংক্রিয়ভাবে জুমুআহ ও রাকাত দেখাবে" else "Displays Jumu'ah & Friday Sunnah/Farz Rakats automatically on Fridays",
                                    color = themeColors.secondaryText,
                                    fontSize = 11.sp
                                )
                            }

                            Switch(
                                checked = autoJumuahFriday,
                                onCheckedChange = { checked ->
                                    autoJumuahFriday = checked
                                    PrayerSettingsRepository.setAutoJumuahEnabled(context, checked)
                                    onSettingsChanged()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF022C22),
                                    checkedTrackColor = Color(0xFF34D399),
                                    uncheckedThumbColor = themeColors.secondaryText,
                                    uncheckedTrackColor = themeColors.subtleSurface
                                ),
                                modifier = Modifier.testTag("auto_jumuah_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // E. Hijri Moon-Sighting Offset
                        Text(
                            text = if (isBangla) "হিজরি তারিখ সমন্বয় (চাঁদ দেখা)" else "HIJRI DATE OFFSET (MOON SIGHTING)",
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
                                        .clickable {
                                            hijriOffset = offset
                                            DateUtils.setHijriOffset(context, offset)
                                            onSettingsChanged()
                                        }
                                        .padding(vertical = 8.dp)
                                        .testTag("hijri_offset_${offset}")
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
                    }
                }

                // 6. COMMERCIAL SPECIFICATIONS & SAFETY CARD
                item {
                    SettingsSectionCard(
                        sectionNumber = if (isBangla) "০৬" else "06",
                        title = if (isBangla) "অ্যাপের নির্ভুলতা ও ব্যাটারি সুরক্ষা তথ্য" else "Commercial Build & Reliability Info",
                        subtitle = if (isBangla) "জ্যোতির্বিজ্ঞানভিত্তিক সূক্ষ্ম হিসাব ও লকস্ক্রিন পাওয়ার সুরক্ষা" else "Astronomical precision & lockscreen power protection",
                        icon = Icons.Default.Info,
                        accentColor = Color(0xFF38BDF8),
                        isBangla = isBangla
                    ) {
                        Text(
                            text = if (isBangla) {
                                "• জ্যোতির্বিজ্ঞান ইঞ্জিন: স্থানীয় জিপিএস টাইমজোনের সাথে সমন্বিত উচ্চ-নির্ভুল সৌর ডেক্লিনেশন ও ইকুয়েশন-অব-টাইম ক্যালকুলেটর।\n" +
                                    "• মাকরূহমুক্ত ওয়াক্ত সীমা: আসরের বৈধ সময় স্বয়ংক্রিয়ভাবে সূর্যাস্তের পূর্বের ১৫ মিনিটের মাকরূহ সময় বাদ দিয়ে দেখায়; ইশার উত্তম সময় শারঈ মধ্যরাত (নিসফুল লাইল) পর্যন্ত নির্ধারিত।\n" +
                                    "• লকস্ক্রিন ব্যাটারি সুরক্ষা: ফুল-স্ক্রিন এলার্মে স্পর্শ না করলে ৬০ সেকেন্ড পর স্বয়ংক্রিয়ভাবে স্ক্রিন বন্ধ হয়ে নোটিফিকেশন ট্রেতে জমা থাকে এবং ২০ সেকেন্ড পর এলার্মের শব্দ থেমে যায়।"
                            } else {
                                "• Astronomical Engine: High-precision solar declination & equation-of-time solver synchronized to local GPS timezone.\n" +
                                    "• Non-Makruh Waqt Boundaries: Asr permitted window automatically excludes the 15-minute Pre-Sunset Makruh window; Isha preferred window ends at true Islamic Midnight (Nisf al-Layl).\n" +
                                    "• Lockscreen Battery Protection: Unattended full-screen alarms automatically close after 60 seconds while keeping the notification in the shade; chime auto-silences after 20 seconds."
                            },
                            color = themeColors.secondaryText,
                            fontSize = 11.5.sp,
                            lineHeight = 16.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandedContentOptionRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    val themeColors = currentPrayerThemeColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
    ) {
        Text(
            text = title,
            color = themeColors.primaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF022C22),
                checkedTrackColor = Color(0xFF6EE7B7),
                uncheckedThumbColor = themeColors.mutedText,
                uncheckedTrackColor = themeColors.subtleSurface
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}

@Composable
private fun SettingsSectionCard(
    sectionNumber: String,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isBangla: Boolean = false,
    content: @Composable () -> Unit
) {
    val themeColors = currentPrayerThemeColors()
    AppleLiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tintColor = accentColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.20f))
                        .border(1.dp, accentColor.copy(alpha = 0.65f), CircleShape)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isBangla) "বিভাগ $sectionNumber" else "SECTION $sectionNumber",
                            color = accentColor,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.9.sp
                        )
                    }
                    Text(
                        text = title,
                        color = themeColors.primaryText,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = subtitle,
                        color = themeColors.secondaryText,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            content()
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    isGranted: Boolean,
    isBangla: Boolean = false,
    onFixClick: () -> Unit
) {
    val themeColors = currentPrayerThemeColors()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = themeColors.primaryText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (isGranted) Color(0xFF10B981).copy(alpha = 0.18f)
                    else Color(0xFFF59E0B).copy(alpha = 0.22f)
                )
                .clickable(enabled = !isGranted) { onFixClick() }
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(
                text = if (isGranted) {
                    if (isBangla) "সক্রিয়" else "ACTIVE"
                } else {
                    if (isBangla) "চালু করুন →" else "ENABLE →"
                },
                color = if (isGranted) themeColors.emeraldAccent else themeColors.goldAccent,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
