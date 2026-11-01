package com.example.prayer.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.prayer.alarm.PrayerAlarmScheduler
import com.example.prayer.alarm.PrayerNotificationHelper
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.location.LocationHelper
import com.example.prayer.model.AppLanguage
import com.example.prayer.model.AppThemeMode
import com.example.prayer.model.PrayerSettingsRepository
import com.example.prayer.model.PrayerType
import com.example.prayer.model.UserLocation
import com.example.prayer.ui.components.AppleLiquidGlassCard
import com.example.prayer.ui.components.AppleLiquidGlassPill
import com.example.prayer.ui.components.CalendarDateDialog
import com.example.prayer.ui.components.CustomNotificationBanner
import com.example.prayer.ui.components.ForbiddenWarningBox
import com.example.prayer.ui.components.LiquidGlassBackground
import com.example.prayer.ui.components.LocalAppLanguage
import com.example.prayer.ui.components.LocalIsDarkTheme
import com.example.prayer.ui.components.LocationPickerDialog
import com.example.prayer.ui.components.MakroohInfoDialog
import com.example.prayer.ui.components.PrayerRowData
import com.example.prayer.ui.components.PrayerRowItem
import com.example.prayer.ui.components.SolarArcGauge
import com.example.prayer.ui.components.VoluntaryPrayerSlot
import com.example.prayer.ui.components.currentPrayerThemeColors
import com.example.prayer.ui.components.isBanglaLanguage
import com.example.prayer.util.DateUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class ActiveScreen {
    DASHBOARD,
    SETTINGS
}

@Composable
fun PrayerScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentLocation by remember {
        mutableStateOf(
            LocationHelper.getLastSavedLocation(context)
                ?: UserLocation(
                    latitude = 26.1250,
                    longitude = 89.6833,
                    locationName = "Bhurungamari, BD",
                    isGps = false
                )
        )
    }

    var isFetchingGpsFirstTime by remember {
        mutableStateOf(LocationHelper.getLastSavedLocation(context) == null)
    }
    var gpsStatusError by remember { mutableStateOf<String?>(null) }

    // Commercial Settings State
    var appLanguage by remember {
        mutableStateOf(PrayerSettingsRepository.getAppLanguage(context))
    }
    var calculationMethod by remember {
        mutableStateOf(PrayerSettingsRepository.getCalculationMethod(context))
    }
    var asrJuristicMethod by remember {
        mutableStateOf(PrayerSettingsRepository.getAsrJuristicMethod(context))
    }
    var minuteOffsets by remember {
        mutableStateOf(PrayerSettingsRepository.getMinuteOffsets(context))
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

    val resolvedZone = remember(currentLocation) {
        PrayerCalculator.resolveZoneId(currentLocation.latitude, currentLocation.longitude)
    }
    var realTodayDate by remember(resolvedZone) { mutableStateOf(LocalDate.now(resolvedZone)) }
    var selectedDate by remember(resolvedZone) { mutableStateOf(LocalDate.now(resolvedZone)) }
    var currentTime by remember(resolvedZone) { mutableStateOf(LocalTime.now(resolvedZone)) }

    var showMakroohDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    var activeScreen by remember { mutableStateOf(ActiveScreen.DASHBOARD) }
    var settingsInitialCategory by remember { mutableStateOf(SettingsCategory.ALL) }

    var isRefreshingGps by remember { mutableStateOf(false) }
    var expandedPrayer by remember { mutableStateOf<PrayerType?>(null) }

    var salatAlarmsEnabled by remember {
        mutableStateOf(PrayerAlarmScheduler.isSalatAlarmsEnabled(context))
    }
    var forbiddenRemindersEnabled by remember {
        mutableStateOf(PrayerAlarmScheduler.isForbiddenRemindersEnabled(context))
    }

    val individualPrayerAlarms = remember {
        mutableStateMapOf<PrayerType, Boolean>().apply {
            PrayerSettingsRepository.fiveDailyPrayers.forEach { prayer ->
                put(prayer, PrayerAlarmScheduler.isPrayerAlarmEnabled(context, prayer))
            }
        }
    }

    // Completed Prayer Checkmark State for the currently selected date
    var completedPrayers by remember(selectedDate) {
        mutableStateOf(PrayerSettingsRepository.getCompletedPrayersForDate(context, selectedDate))
    }

    var systemNotificationsEnabled by remember {
        mutableStateOf(PrayerNotificationHelper.areSystemNotificationsEnabled(context))
    }
    val activeBanner by PrayerNotificationHelper.activeBanner.collectAsState()

    val isBangla = appLanguage == AppLanguage.BANGLA

    fun scheduleAlarmsForLocation(loc: UserLocation) {
        val zone = PrayerCalculator.resolveZoneId(loc.latitude, loc.longitude)
        val today = LocalDate.now(zone)
        val method = PrayerSettingsRepository.getCalculationMethod(context)
        val asr = PrayerSettingsRepository.getAsrJuristicMethod(context)
        val offsets = PrayerSettingsRepository.getMinuteOffsets(context)
        val todaySched = PrayerCalculator.calculateSchedule(
            date = today,
            lat = loc.latitude,
            lng = loc.longitude,
            calculationMethod = method,
            asrJuristicMethod = asr,
            minuteOffsets = offsets
        )
        val tomorrowSched = PrayerCalculator.calculateSchedule(
            date = today.plusDays(1),
            lat = loc.latitude,
            lng = loc.longitude,
            calculationMethod = method,
            asrJuristicMethod = asr,
            minuteOffsets = offsets
        )
        PrayerAlarmScheduler.scheduleAll(context, todaySched, tomorrowSched)
    }

    fun refreshSettings() {
        appLanguage = PrayerSettingsRepository.getAppLanguage(context)
        calculationMethod = PrayerSettingsRepository.getCalculationMethod(context)
        asrJuristicMethod = PrayerSettingsRepository.getAsrJuristicMethod(context)
        minuteOffsets = PrayerSettingsRepository.getMinuteOffsets(context)
        appThemeMode = PrayerSettingsRepository.getAppThemeMode(context)
        expandedCardOptions = PrayerSettingsRepository.getExpandedCardOptions(context)
        if (!expandedCardOptions.enableExpansion) {
            expandedPrayer = null
        }
        use24HourFormat = PrayerSettingsRepository.is24HourFormat(context)
        autoJumuahFriday = PrayerSettingsRepository.isAutoJumuahEnabled(context)
        hijriOffset = DateUtils.getHijriOffset(context)
        salatAlarmsEnabled = PrayerAlarmScheduler.isSalatAlarmsEnabled(context)
        forbiddenRemindersEnabled = PrayerAlarmScheduler.isForbiddenRemindersEnabled(context)
        PrayerSettingsRepository.fiveDailyPrayers.forEach { prayer ->
            individualPrayerAlarms[prayer] = PrayerAlarmScheduler.isPrayerAlarmEnabled(context, prayer)
        }
        completedPrayers = PrayerSettingsRepository.getCompletedPrayersForDate(context, selectedDate)
        scheduleAlarmsForLocation(currentLocation)
    }

    fun triggerLiveGpsFetch() {
        isRefreshingGps = true
        gpsStatusError = null
        coroutineScope.launch {
            val loc = LocationHelper.getDeviceLocation(context)
            if (loc != null) {
                currentLocation = loc
                isFetchingGpsFirstTime = false
                gpsStatusError = null
                scheduleAlarmsForLocation(loc)
            } else {
                val saved = LocationHelper.getLastSavedLocation(context)
                if (saved != null) {
                    currentLocation = saved
                    isFetchingGpsFirstTime = false
                } else {
                    gpsStatusError = if (appLanguage == AppLanguage.BANGLA) {
                        "জিপিএস সিগন্যাল পাওয়া যায়নি। ফোনের লোকেশন (GPS) চালু আছে কিনা যাচাই করুন অথবা শহর নির্বাচন করুন।"
                    } else {
                        "Unable to lock onto GPS satellites. Ensure Location Services (GPS) are enabled on your device."
                    }
                }
            }
            isRefreshingGps = false
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        systemNotificationsEnabled = PrayerNotificationHelper.areSystemNotificationsEnabled(context)
        scheduleAlarmsForLocation(currentLocation)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerLiveGpsFetch()
        } else {
            val saved = LocationHelper.getLastSavedLocation(context)
            if (saved != null) {
                currentLocation = saved
                isFetchingGpsFirstTime = false
            } else {
                gpsStatusError = if (appLanguage == AppLanguage.BANGLA) {
                    "নির্ভুল সূর্যোদয়, সূর্যাস্ত ও নামাজের সময় গণনার জন্য লোকেশন অনুমতি প্রয়োজন।"
                } else {
                    "Location permission is required to calculate exact sunrise, sunset, and prayer times for your coordinates."
                }
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, currentLocation, selectedDate) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                systemNotificationsEnabled = PrayerNotificationHelper.areSystemNotificationsEnabled(context)
                refreshSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        PrayerNotificationHelper.createNotificationChannels(context)
        systemNotificationsEnabled = PrayerNotificationHelper.areSystemNotificationsEnabled(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasNotifPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotifPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val hasFine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            triggerLiveGpsFetch()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        scheduleAlarmsForLocation(currentLocation)
    }

    // Live 1-second ticker anchored to the selected location's timezone
    LaunchedEffect(resolvedZone) {
        while (true) {
            val nowTime = LocalTime.now(resolvedZone)
            val nowDate = LocalDate.now(resolvedZone)
            currentTime = nowTime
            if (nowDate != realTodayDate) {
                if (selectedDate == realTodayDate) {
                    selectedDate = nowDate
                }
                realTodayDate = nowDate
                scheduleAlarmsForLocation(currentLocation)
            }
            delay(1000L)
        }
    }

    val isViewingToday = selectedDate == realTodayDate

    val schedule = remember(
        selectedDate,
        currentLocation,
        calculationMethod,
        asrJuristicMethod,
        minuteOffsets
    ) {
        PrayerCalculator.calculateSchedule(
            date = selectedDate,
            lat = currentLocation.latitude,
            lng = currentLocation.longitude,
            calculationMethod = calculationMethod,
            asrJuristicMethod = asrJuristicMethod,
            minuteOffsets = minuteOffsets
        )
    }

    val tomorrowSchedule = remember(
        selectedDate,
        currentLocation,
        calculationMethod,
        asrJuristicMethod,
        minuteOffsets
    ) {
        PrayerCalculator.calculateSchedule(
            date = selectedDate.plusDays(1),
            lat = currentLocation.latitude,
            lng = currentLocation.longitude,
            calculationMethod = calculationMethod,
            asrJuristicMethod = asrJuristicMethod,
            minuteOffsets = minuteOffsets
        )
    }

    val forbiddenStatus = remember(schedule, currentTime, isViewingToday) {
        if (isViewingToday) {
            PrayerCalculator.checkForbiddenStatus(currentTime, schedule)
        } else {
            PrayerCalculator.checkForbiddenStatus(LocalTime.of(9, 0), schedule)
        }
    }

    val nextPrayer = remember(schedule, tomorrowSchedule, currentTime) {
        PrayerCalculator.determineNextPrayer(currentTime, schedule, tomorrowSchedule)
    }

    val activePrayer = remember(schedule, currentTime, isViewingToday) {
        if (isViewingToday) {
            PrayerCalculator.determineCurrentActivePrayer(currentTime, schedule)
        } else {
            PrayerType.SUNRISE
        }
    }

    val activeWaqt = remember(schedule, tomorrowSchedule, currentTime, autoJumuahFriday) {
        PrayerCalculator.determineActiveWaqt(
            now = currentTime,
            todaySchedule = schedule,
            tomorrowSchedule = tomorrowSchedule,
            autoJumuahOnFriday = autoJumuahFriday
        )
    }

    val isFriday = selectedDate.dayOfWeek == java.time.DayOfWeek.FRIDAY && autoJumuahFriday

    val timeFormatter = remember(use24HourFormat) {
        if (use24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
        } else {
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
        }
    }

    val calculationBadgeText = remember(calculationMethod, asrJuristicMethod, minuteOffsets, isBangla) {
        val base = "${calculationMethod.localizedShortName(isBangla)} • ${asrJuristicMethod.localizedShortName(isBangla)}"
        if (minuteOffsets.hasAnyOffset()) {
            if (isBangla) "$base • সমন্বিত" else "$base • Calibrated"
        } else base
    }

    val tahajjudStart = remember(schedule) {
        val maghribSecs = schedule.maghrib.toSecondOfDay()
        val fajrSecs = schedule.fajr.toSecondOfDay() + 86400
        val nightDuration = fajrSecs - maghribSecs
        val lastThirdStartSecs = (maghribSecs + (nightDuration * 2) / 3) % 86400
        LocalTime.ofSecondOfDay(lastThirdStartSecs.toLong())
    }
    val chashtStart = remember(schedule) { schedule.sunrise.plusMinutes(45) }
    val chashtEnd = remember(schedule) { schedule.zawalMakroohStart.minusMinutes(5) }
    val awwabinStart = remember(schedule) { schedule.maghrib.plusMinutes(15) }
    val awwabinEnd = remember(schedule) { schedule.isha.minusMinutes(5) }

    val prayerRows = remember(schedule, nextPrayer, activePrayer, activeWaqt, isViewingToday, isFriday, minuteOffsets, timeFormatter, isBangla) {
        val sunriseFormatted = DateUtils.localizeTime(schedule.sunrise.format(timeFormatter), isBangla)
        val zawalStartFormatted = DateUtils.localizeTime(schedule.zawalMakroohStart.format(timeFormatter), isBangla)
        val dhuhrFormatted = DateUtils.localizeTime(schedule.dhuhr.format(timeFormatter), isBangla)
        val sunsetMakroohFormatted = DateUtils.localizeTime(schedule.sunsetMakroohStart.format(timeFormatter), isBangla)
        val maghribFormatted = DateUtils.localizeTime(schedule.maghrib.format(timeFormatter), isBangla)
        val midnightFormatted = DateUtils.localizeTime(schedule.midnight.format(timeFormatter), isBangla)

        listOf(
            PrayerRowData(
                type = PrayerType.FAJR,
                startTime = schedule.fajr,
                endTime = schedule.fajrEnd,
                isNext = isViewingToday && nextPrayer.prayerType == PrayerType.FAJR,
                isActive = isViewingToday && activePrayer == PrayerType.FAJR,
                activeProgress = if (isViewingToday && activePrayer == PrayerType.FAJR) activeWaqt.progress else 0f,
                sunnahInfo = if (isBangla) "২ সুন্নত মুয়াক্কাদা + ২ ফরজ" else "2 Sunnah Mu'akkadah + 2 Farz",
                naflNote = if (isBangla) {
                    "সুবহে সাদিক থেকে ফজরের ২ রাকাত সুন্নত ছাড়া অন্য কোনো নফল নামাজ নেই।"
                } else {
                    "No Nafl after Subh Sadiq except the 2 Rakat Sunnah of Fazr"
                },
                makruhNote = if (isBangla) {
                    "সূর্যোদয় ($sunriseFormatted) থেকে ১৫ মিনিট নামাজ সম্পূর্ণ নিষিদ্ধ (মাকরূহ তাহরীমী)।"
                } else {
                    "Sunrise at $sunriseFormatted (15m post-sunrise is strictly prohibited)"
                },
                milestoneLabel = if (isBangla) "সূর্যোদয়" else "Sunrise",
                milestoneTime = schedule.sunrise,
                isMilestoneWarning = false,
                minuteOffset = minuteOffsets.fajr,
                voluntaryPrayers = listOf(
                    VoluntaryPrayerSlot(
                        name = "Ishraq",
                        startTime = schedule.ishraq,
                        endTime = schedule.sunrise.plusMinutes(110),
                        description = if (isBangla) "২–৪ রাকাত নফল (সূর্যোদয়ের ১৫ মিনিট পর)" else "2–4 Rakat Nafl (15m after Sunrise)"
                    ),
                    VoluntaryPrayerSlot(
                        name = "Chasht / Duha",
                        startTime = chashtStart,
                        endTime = chashtEnd,
                        description = if (isBangla) "২–৮ রাকাত সালাতুদ দুহা (জাওয়ালের পূর্ব পর্যন্ত)" else "2–8 Rakat Salat al-Duha (before Zawal)"
                    )
                )
            ),
            PrayerRowData(
                type = PrayerType.DHUHR,
                customTitle = if (isFriday) "Jumu'ah" else "Duhr",
                startTime = schedule.dhuhr,
                endTime = schedule.dhuhrEnd,
                isNext = isViewingToday && nextPrayer.prayerType == PrayerType.DHUHR,
                isActive = isViewingToday && activePrayer == PrayerType.DHUHR,
                activeProgress = if (isViewingToday && activePrayer == PrayerType.DHUHR) activeWaqt.progress else 0f,
                sunnahInfo = if (isFriday) {
                    if (isBangla) "৪ সুন্নত + ২ ফরজ (জুমুআহ) + ৪+২ সুন্নত" else "4 Sunnah + 2 Farz (Jumu'ah) + 4+2 Sunnah"
                } else {
                    if (isBangla) "৪ সুন্নত + ৪ ফরজ + ২ সুন্নত + ২ নফল" else "4 Sunnah + 4 Farz + 2 Sunnah + 2 Nafl"
                },
                naflNote = if (isFriday) {
                    if (isBangla) "জুমুআর পূর্বে ও পরে ৪ রাকাত করে সুন্নতে মুয়াক্কাদা" else "4 Rakat Qabliyah before Khutbah & 4+2 Rakat Ba'diyah after Jumu'ah"
                } else {
                    if (isBangla) "ফরজের পর ২ রাকাত সুন্নত ও ২ রাকাত নফল মুস্তাহাব" else "2 Rakat Nafl after the 2 Rakat post-Duhr Sunnah is Mustahabb"
                },
                makruhNote = if (isBangla) {
                    "জাওয়ালের নিষিদ্ধ সময়: $zawalStartFormatted – $dhuhrFormatted (যোহরের পূর্বের ১০ মিনিট)"
                } else {
                    "Zawal Forbidden Window: $zawalStartFormatted – $dhuhrFormatted (10m before Duhr)"
                },
                milestoneLabel = if (isBangla) "জাওয়াল" else "Zawal",
                milestoneTime = schedule.zawalMakroohStart,
                isMilestoneWarning = true,
                minuteOffset = minuteOffsets.dhuhr
            ),
            PrayerRowData(
                type = PrayerType.ASR,
                startTime = schedule.asr,
                endTime = schedule.asrEnd,
                isNext = isViewingToday && nextPrayer.prayerType == PrayerType.ASR,
                isActive = isViewingToday && activePrayer == PrayerType.ASR,
                activeProgress = if (isViewingToday && activePrayer == PrayerType.ASR) activeWaqt.progress else 0f,
                sunnahInfo = if (isBangla) "৪ সুন্নত (গায়রে মুয়াক্কাদা) + ৪ ফরজ" else "4 Sunnah (Ghayr Mu'akkadah) + 4 Farz",
                naflNote = if (isBangla) "আসরের ফরজ আদায়ের পর সূর্যাস্ত পর্যন্ত কোনো নফল নামাজ পড়া যাবে না।" else "No Nafl prayers permitted after Asr Farz until Maghrib sunset",
                makruhNote = if (isBangla) {
                    "সূর্যাস্তের নিষিদ্ধ সময় $sunsetMakroohFormatted – $maghribFormatted (১৫ মিনিট পূর্বে বৈধ সময় শেষ)"
                } else {
                    "Pre-Sunset Makruh starts at $sunsetMakroohFormatted (15m before Maghrib)"
                },
                milestoneLabel = if (isBangla) "মাকরূহ শুরু" else "Makruh",
                milestoneTime = schedule.sunsetMakroohStart,
                isMilestoneWarning = true,
                minuteOffset = minuteOffsets.asr
            ),
            PrayerRowData(
                type = PrayerType.MAGHRIB,
                startTime = schedule.maghrib,
                endTime = schedule.maghribEnd,
                isNext = isViewingToday && nextPrayer.prayerType == PrayerType.MAGHRIB,
                isActive = isViewingToday && activePrayer == PrayerType.MAGHRIB,
                activeProgress = if (isViewingToday && activePrayer == PrayerType.MAGHRIB) activeWaqt.progress else 0f,
                sunnahInfo = if (isBangla) "৩ ফরজ + ২ সুন্নত + ২ নফল" else "3 Farz + 2 Sunnah + 2 Nafl",
                naflNote = if (isBangla) "মাগরিবের পর ৬ থেকে ২০ রাকাত আওয়াবীন নামাজ অত্যন্ত ফজিলতপূর্ণ" else "6 to 20 Rakat Salat al-Awwabin after Maghrib Sunnah",
                makruhNote = if (isBangla) "ইফতার ও মাগরিবের ওয়াক্ত শুরু: $maghribFormatted" else "Iftar & Maghrib starts at $maghribFormatted; avoid delaying past twilight glow",
                milestoneLabel = if (isBangla) "ইফতার" else "Iftar",
                milestoneTime = schedule.maghrib,
                isMilestoneWarning = false,
                minuteOffset = minuteOffsets.maghrib,
                voluntaryPrayers = listOf(
                    VoluntaryPrayerSlot(
                        name = "Awwabin",
                        startTime = awwabinStart,
                        endTime = awwabinEnd,
                        description = if (isBangla) "৬ রাকাত নফল (মাগরিবের সুন্নতের পর)" else "6 Rakat Nafl after Maghrib Sunnah"
                    )
                )
            ),
            PrayerRowData(
                type = PrayerType.ISHA,
                startTime = schedule.isha,
                endTime = schedule.ishaEnd,
                isNext = isViewingToday && nextPrayer.prayerType == PrayerType.ISHA,
                isActive = isViewingToday && activePrayer == PrayerType.ISHA,
                activeProgress = if (isViewingToday && activePrayer == PrayerType.ISHA) activeWaqt.progress else 0f,
                sunnahInfo = if (isBangla) "৪ সুন্নত + ৪ ফরজ + ২ সুন্নত + ৩ বিতর" else "4 Sunnah + 4 Farz + 2 Sunnah + 3 Witr",
                naflNote = if (isBangla) "৩ রাকাত বিতর ওয়াজিব; শেষ রাতে তাহাজ্জুদের পর বিতর পড়া উত্তম" else "3 Rakat Witr Wajib (best after Tahajjud if waking up is certain)",
                makruhNote = if (isBangla) {
                    "ইশার উত্তম সময় মধ্যরাত ($midnightFormatted) পর্যন্ত; এরপর বিলম্ব করা মাকরূহ।"
                } else {
                    "Preferred Isha ends at Islamic Midnight ($midnightFormatted); Makruh to delay past this"
                },
                milestoneLabel = if (isBangla) "মধ্যরাত" else "Midnight",
                milestoneTime = schedule.midnight,
                isMilestoneWarning = false,
                minuteOffset = minuteOffsets.isha,
                voluntaryPrayers = listOf(
                    VoluntaryPrayerSlot(
                        name = "Tahajjud",
                        startTime = tahajjudStart,
                        endTime = schedule.fajr.minusMinutes(10),
                        description = if (isBangla) "৪–৮ রাকাত কিয়ামুল লাইল (রাতের শেষ তৃতীয়াংশ)" else "4–8 Rakat Qiyam al-Layl (Last third of night)"
                    )
                )
            )
        )
    }

    val systemIsDark = isSystemInDarkTheme()
    val isDarkTheme = when (appThemeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> systemIsDark
    }

    // Update Android OS Status Bar & Navigation Bar icon contrast dynamically for Light/Dark themes
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDarkTheme
                controller.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    // Clamp excessive system fontScale so cards and time pills never overflow on any device setting
    val baseDensity = LocalDensity.current
    val safeFontScale = baseDensity.fontScale.coerceIn(0.85f, 1.05f)
    val clampedDensity = remember(baseDensity.density, safeFontScale) {
        Density(density = baseDensity.density, fontScale = safeFontScale)
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides isDarkTheme,
        LocalAppLanguage provides appLanguage,
        LocalDensity provides clampedDensity
    ) {
        val themeColors = currentPrayerThemeColors()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isCompactScreen = maxHeight < 700.dp

            LiquidGlassBackground(
                modifier = Modifier.fillMaxSize(),
                activePrayerType = activeWaqt.prayerType,
                isForbidden = forbiddenStatus.isForbidden
            )

            if (isFetchingGpsFirstTime) {
                GpsAcquisitionScreen(
                    isRefreshing = isRefreshingGps,
                    errorMessage = gpsStatusError,
                    onRetryGps = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            triggerLiveGpsFetch()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onOpenLocationSettings = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (_: Throwable) {
                        }
                    },
                    onChooseCityPreset = {
                        showLocationDialog = true
                    }
                )
            } else if (activeScreen == ActiveScreen.SETTINGS) {
                SettingsScreen(
                    currentLocation = currentLocation,
                    isRefreshingGps = isRefreshingGps,
                    onRefreshGps = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            triggerLiveGpsFetch()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onOpenLocationPicker = {
                        showLocationDialog = true
                    },
                    onSettingsChanged = {
                        refreshSettings()
                    },
                    onNavigateBack = {
                        refreshSettings()
                        activeScreen = ActiveScreen.DASHBOARD
                    },
                    initialCategory = settingsInitialCategory
                )
            } else {
                // MAIN COMMERCIAL DASHBOARD SCREEN
                val completedCount = completedPrayers.size

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // Prominent Status Header Bar (visually anchors the top status bar area)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(themeColors.topBarSurface)
                    ) {
                        Column {
                            TopDateHeaderBar(
                                date = selectedDate,
                                currentTime = currentTime,
                                hijriOffset = hijriOffset,
                                locationName = currentLocation.locationName,
                                isGpsLocation = currentLocation.isGps,
                                isRefreshingGps = isRefreshingGps,
                                salatAlarmsEnabled = salatAlarmsEnabled,
                                isDarkTheme = isDarkTheme,
                                use24HourFormat = use24HourFormat,
                                onLocationClick = {
                                    showLocationDialog = true
                                },
                                onToggleTheme = {
                                    val nextTheme = if (isDarkTheme) AppThemeMode.LIGHT else AppThemeMode.DARK
                                    PrayerSettingsRepository.setAppThemeMode(context, nextTheme)
                                    refreshSettings()
                                },
                                onToggleSalatAlarms = {
                                    val nextState = !salatAlarmsEnabled
                                    salatAlarmsEnabled = nextState
                                    PrayerAlarmScheduler.setSalatAlarmsEnabled(context, nextState)
                                    scheduleAlarmsForLocation(currentLocation)
                                },
                                onOpenSettings = {
                                    settingsInitialCategory = SettingsCategory.ALL
                                    activeScreen = ActiveScreen.SETTINGS
                                },
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = if (isCompactScreen) 6.dp else 8.dp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(themeColors.topBarBorder)
                            )
                        }
                    }

                    // Fallback in-app notification banner ONLY when OS notifications are blocked
                    if (!systemNotificationsEnabled) {
                        CustomNotificationBanner(
                            notification = activeBanner,
                            onDismiss = {
                                PrayerNotificationHelper.cancelSalatNotification(context)
                            },
                            onSnooze = { notif ->
                                PrayerNotificationHelper.cancelSalatNotification(context)
                                PrayerAlarmScheduler.scheduleSnooze(
                                    context = context,
                                    prayerName = notif.prayerName,
                                    startTime = notif.startTimeStr,
                                    endTime = notif.endTimeStr,
                                    minutes = 5
                                )
                            }
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 12.dp,
                                vertical = if (isCompactScreen) 4.dp else 7.dp
                            )
                    ) {
                        // 1. Date Navigation & Daily Completion Progress Strip
                        DateSelectorRow(
                            selectedDate = selectedDate,
                            realTodayDate = realTodayDate,
                            hijriOffset = hijriOffset,
                            completedCount = completedCount,
                            onPrevDay = { selectedDate = selectedDate.minusDays(1) },
                            onNextDay = { selectedDate = selectedDate.plusDays(1) },
                            onResetToday = { selectedDate = realTodayDate },
                            onOpenCalendarDialog = { showCalendarDialog = true }
                        )

                        Spacer(modifier = Modifier.height(if (isCompactScreen) 4.dp else 6.dp))

                        // 2. Hero Sanctuary Waqt & Solar Arc Dashboard Card
                        SolarArcGauge(
                            schedule = schedule,
                            currentTime = currentTime,
                            nextPrayer = nextPrayer,
                            activeWaqt = activeWaqt,
                            forbiddenStatus = forbiddenStatus,
                            calculationBadgeText = calculationBadgeText,
                            use24HourFormat = use24HourFormat,
                            autoJumuahFriday = autoJumuahFriday,
                            onBadgeClick = {
                                settingsInitialCategory = SettingsCategory.CALCULATION
                                activeScreen = ActiveScreen.SETTINGS
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(if (isCompactScreen) 4.dp else 6.dp))

                        // 3. Forbidden (Makrooh Tahrimi) / Permitted Status Bar
                        ForbiddenWarningBox(
                            forbiddenStatus = forbiddenStatus,
                            use24HourFormat = use24HourFormat,
                            onInfoClick = { showMakroohDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(if (isCompactScreen) 4.dp else 6.dp))

                        // 4. Five Daily Prayer Cards with Completion Checkmarks & Optional Expandable Details
                        Column(
                            verticalArrangement = Arrangement.spacedBy(if (isCompactScreen) 5.dp else 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            prayerRows.forEach { rowData ->
                                val isPrayerAlarmOn = salatAlarmsEnabled && (individualPrayerAlarms[rowData.type] ?: true)
                                val isDone = completedPrayers.contains(rowData.type)
                                val isRowExpanded = expandedCardOptions.enableExpansion && expandedPrayer == rowData.type

                                PrayerRowItem(
                                    data = rowData,
                                    isExpanded = isRowExpanded,
                                    isCompleted = isDone,
                                    isAlarmEnabled = isPrayerAlarmOn,
                                    expandedCardOptions = expandedCardOptions,
                                    use24HourFormat = use24HourFormat,
                                    onToggleExpand = {
                                        if (expandedCardOptions.enableExpansion) {
                                            expandedPrayer = if (expandedPrayer == rowData.type) null else rowData.type
                                        }
                                    },
                                    onToggleCompleted = {
                                        PrayerSettingsRepository.togglePrayerCompleted(context, selectedDate, rowData.type)
                                        completedPrayers = PrayerSettingsRepository.getCompletedPrayersForDate(context, selectedDate)
                                    },
                                    onToggleAlarm = {
                                        val nextEnabled = !(individualPrayerAlarms[rowData.type] ?: true)
                                        individualPrayerAlarms[rowData.type] = nextEnabled
                                        if (nextEnabled && !salatAlarmsEnabled) {
                                            salatAlarmsEnabled = true
                                            PrayerAlarmScheduler.setSalatAlarmsEnabled(context, true)
                                        }
                                        PrayerAlarmScheduler.setPrayerAlarmEnabled(context, rowData.type, nextEnabled)
                                        scheduleAlarmsForLocation(currentLocation)
                                    },
                                    onAdjustOffset = { newOffset ->
                                        PrayerSettingsRepository.setPrayerMinuteOffset(context, rowData.type, newOffset)
                                        refreshSettings()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            if (showMakroohDialog) {
                MakroohInfoDialog(
                    schedule = schedule,
                    onDismiss = { showMakroohDialog = false }
                )
            }

            if (showLocationDialog) {
                LocationPickerDialog(
                    currentLocation = currentLocation,
                    isRefreshingGps = isRefreshingGps,
                    onRefreshGps = {
                        val hasFine = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            triggerLiveGpsFetch()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onSelectLocation = { selectedLoc ->
                        LocationHelper.saveLocation(context, selectedLoc)
                        currentLocation = selectedLoc
                        isFetchingGpsFirstTime = false
                        scheduleAlarmsForLocation(selectedLoc)
                    },
                    onDismiss = { showLocationDialog = false }
                )
            }

            if (showCalendarDialog) {
                CalendarDateDialog(
                    selectedDate = selectedDate,
                    realTodayDate = realTodayDate,
                    hijriOffset = hijriOffset,
                    onSelectDate = { newDate -> selectedDate = newDate },
                    onResetToday = { selectedDate = realTodayDate },
                    onHijriOffsetChange = { newOffset ->
                        hijriOffset = newOffset
                        DateUtils.setHijriOffset(context, newOffset)
                    },
                    onDismiss = { showCalendarDialog = false }
                )
            }
        }
    }
}

@Composable
private fun GpsAcquisitionScreen(
    isRefreshing: Boolean,
    errorMessage: String?,
    onRetryGps: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onChooseCityPreset: () -> Unit
) {
    val themeColors = currentPrayerThemeColors()
    val isBangla = isBanglaLanguage()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        AppleLiquidGlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            tintColor = Color(0xFF38BDF8),
            isActive = true
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8).copy(alpha = 0.35f),
                                    Color(0xFF10B981).copy(alpha = 0.15f)
                                )
                            )
                        )
                        .border(1.5.dp, themeColors.skyAccent.copy(alpha = 0.7f), CircleShape)
                ) {
                    if (isRefreshing || errorMessage == null) {
                        CircularProgressIndicator(
                            color = themeColors.emeraldAccent,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = if (isBangla) "জিপিএস অবস্থান" else "GPS Location",
                            tint = themeColors.goldAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (errorMessage == null) {
                        if (isBangla) "জিপিএস স্থানাঙ্ক নির্ণয় করা হচ্ছে..." else "Acquiring Live GPS Coordinates..."
                    } else {
                        if (isBangla) "জিপিএস অবস্থান প্রয়োজন" else "GPS Location Required"
                    },
                    color = themeColors.primaryText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = errorMessage
                        ?: if (isBangla) {
                            "আপনার অবস্থানের নির্ভুল নামাজ, সেহরি, ইফতার ও মাকরূহ সময়সূচি গণনা করা হচ্ছে।"
                        } else {
                            "Calculating astronomical sunrise, sunset, Makruh windows, and 5 daily prayer times for your exact coordinates."
                        },
                    color = themeColors.secondaryText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMessage != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = onRetryGps,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF022C22),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "পুনরায় চেষ্টা" else "Retry GPS",
                                color = Color(0xFF022C22),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        OutlinedButton(
                            onClick = onOpenLocationSettings,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = themeColors.skyAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isBangla) "জিপিএস চালু করুন" else "Enable GPS",
                                color = themeColors.skyAccent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                OutlinedButton(
                    onClick = onChooseCityPreset,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("choose_city_preset_button")
                ) {
                    Text(
                        text = if (isBangla) "শহর নির্বাচন করুন (ঢাকা, ভূরুঙ্গামারী, মক্কা...)" else "Select City Preset (Dhaka, Bhurungamari, Makkah...)",
                        color = themeColors.emeraldAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopDateHeaderBar(
    date: LocalDate,
    currentTime: LocalTime,
    hijriOffset: Int,
    locationName: String,
    isGpsLocation: Boolean,
    isRefreshingGps: Boolean,
    salatAlarmsEnabled: Boolean,
    isDarkTheme: Boolean,
    use24HourFormat: Boolean,
    onLocationClick: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleSalatAlarms: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = currentPrayerThemeColors()
    val isBangla = isBanglaLanguage()
    val clockFormatter = remember(use24HourFormat) {
        if (use24HourFormat) {
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ENGLISH)
        } else {
            DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH)
        }
    }

    val hijriStr = remember(date, hijriOffset, isBangla) {
        DateUtils.getHijriDate(date, hijriOffset, isBangla)
    }

    val localizedCityName = remember(locationName, isBangla) {
        DateUtils.localizeLocationName(locationName, isBangla).substringBefore(",")
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Row 1: Location on Left, Quick Action Pills on Right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Location Pill -> Opens ONLY LocationPickerDialog
            AppleLiquidGlassPill(
                shape = RoundedCornerShape(50),
                tintColor = if (isGpsLocation) Color(0xFF10B981) else Color(0xFF38BDF8),
                modifier = Modifier
                    .clickable { onLocationClick() }
                    .testTag("location_chip")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    if (isRefreshingGps) {
                        CircularProgressIndicator(
                            color = themeColors.emeraldAccent,
                            strokeWidth = 1.8.dp,
                            modifier = Modifier.size(11.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (isGpsLocation) Icons.Default.MyLocation else Icons.Default.LocationOn,
                            contentDescription = if (isBangla) "অবস্থান পরিবর্তন" else "Change Location",
                            tint = if (isGpsLocation) themeColors.emeraldAccent else themeColors.skyAccent,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(3.5.dp))
                    Text(
                        text = localizedCityName,
                        color = themeColors.primaryText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Right Side Action Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // 1. Quick Light / Dark Theme Toggle Button
                AppleLiquidGlassPill(
                    shape = CircleShape,
                    tintColor = if (isDarkTheme) Color(0xFFFDE047) else Color(0xFF0284C7),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onToggleTheme() }
                        .testTag("theme_toggle_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Switch to Light Theme" else "Switch to Dark Theme",
                            tint = if (isDarkTheme) themeColors.goldAccent else themeColors.skyAccent,
                            modifier = Modifier.size(13.5.dp)
                        )
                    }
                }

                // 2. Master Salat Alarm Quick Toggle
                AppleLiquidGlassPill(
                    shape = RoundedCornerShape(50),
                    tintColor = if (salatAlarmsEnabled) Color(0xFF10B981) else Color(0xFF64748B),
                    modifier = Modifier
                        .clickable { onToggleSalatAlarms() }
                        .testTag("salat_master_alarm_toggle")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (salatAlarmsEnabled) Icons.Default.Alarm else Icons.Default.NotificationsOff,
                            contentDescription = if (salatAlarmsEnabled) "Salat Alarms Active" else "Salat Alarms Muted",
                            tint = if (salatAlarmsEnabled) themeColors.emeraldAccent else themeColors.mutedText,
                            modifier = Modifier.size(11.5.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (isBangla) {
                                if (salatAlarmsEnabled) "এলার্ম: চালু" else "এলার্ম: বন্ধ"
                            } else {
                                if (salatAlarmsEnabled) "Wake-Up: ON" else "Wake-Up: OFF"
                            },
                            color = if (salatAlarmsEnabled) themeColors.emeraldAccent else themeColors.mutedText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 3. Dedicated Settings Page Button
                AppleLiquidGlassPill(
                    shape = CircleShape,
                    tintColor = Color(0xFF38BDF8),
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { onOpenSettings() }
                        .testTag("open_settings_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = if (isBangla) "সেটিংস খুলুন" else "Open Settings",
                            tint = themeColors.skyAccent,
                            modifier = Modifier.size(13.5.dp)
                        )
                    }
                }
            }
        }

        // Row 2: Live Digital Clock on Left, Hijri Date on Right
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Live Clock Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag("live_clock_chip")
            ) {
                Text(
                    text = DateUtils.localizeTime(currentTime.format(clockFormatter), isBangla),
                    color = themeColors.primaryText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(themeColors.emeraldStrong.copy(alpha = 0.16f))
                        .padding(horizontal = 4.5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = if (isBangla) "লাইভ" else "LIVE",
                        color = themeColors.emeraldAccent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        softWrap = false
                    )
                }
            }

            // Hijri Date
            Text(
                text = hijriStr,
                color = themeColors.skyAccent,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DateSelectorRow(
    selectedDate: LocalDate,
    realTodayDate: LocalDate,
    hijriOffset: Int,
    completedCount: Int,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onResetToday: () -> Unit,
    onOpenCalendarDialog: () -> Unit
) {
    val themeColors = currentPrayerThemeColors()
    val isBangla = isBanglaLanguage()
    val isToday = selectedDate == realTodayDate
    val isFriday = selectedDate.dayOfWeek == java.time.DayOfWeek.FRIDAY
    val bengaliDate = remember(selectedDate, isBangla) {
        DateUtils.getBengaliDate(selectedDate, isBangla)
    }
    val gregorianHeaderDate = remember(selectedDate, isBangla) {
        DateUtils.formatGregorianHeaderDate(selectedDate, isBangla)
    }

    AppleLiquidGlassPill(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tintColor = if (isToday) Color(0xFF10B981) else Color(0xFFF59E0B)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            IconButton(
                onClick = onPrevDay,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("prev_day_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = if (isBangla) "আগের দিন" else "Previous Day",
                    tint = themeColors.primaryText,
                    modifier = Modifier.size(19.dp)
                )
            }

            // Center Date Area -> Tapping opens CalendarDateDialog
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenCalendarDialog() }
                    .padding(vertical = 2.dp, horizontal = 4.dp)
                    .testTag("top_calendar_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = if (isBangla) "ক্যালেন্ডার খুলুন" else "Open Calendar",
                    tint = themeColors.skyAccent,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "$gregorianHeaderDate  •  $bengaliDate",
                    color = themeColors.primaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isFriday) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFFDE047).copy(alpha = 0.22f))
                            .border(0.7.dp, themeColors.goldAccent, RoundedCornerShape(5.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isBangla) "জুমুআহ" else "JUMU'AH",
                            color = themeColors.goldAccent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Daily Completed Prayers Counter Badge (e.g., 0/5 Done or ০/৫ সম্পন্ন)
                val countText = if (isBangla) {
                    "${DateUtils.toBengaliDigits(completedCount.toString())}/৫ সম্পন্ন"
                } else {
                    "$completedCount/5 Done"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (completedCount == 5) Color(0xFF10B981)
                            else themeColors.subtleSurface
                        )
                        .border(
                            0.8.dp,
                            if (completedCount > 0) themeColors.emeraldStrong else themeColors.subtleBorder,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                        .testTag("salat_completion_badge")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = if (isBangla) "আদায়কৃত নামাজ" else "Completed Prayers",
                            tint = if (completedCount == 5) Color(0xFF022C22) else themeColors.emeraldAccent,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        Text(
                            text = countText,
                            color = if (completedCount == 5) Color(0xFF022C22) else themeColors.primaryText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                if (!isToday) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF59E0B).copy(alpha = 0.22f))
                            .border(0.8.dp, themeColors.goldAccent, RoundedCornerShape(8.dp))
                            .clickable { onResetToday() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("reset_today_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = if (isBangla) "আজকের তারিখে ফিরুন" else "Return to Today",
                            tint = themeColors.goldAccent,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(2.5.dp))
                        Text(
                            text = if (isBangla) "আজ" else "Today",
                            color = themeColors.goldAccent,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                IconButton(
                    onClick = onNextDay,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("next_day_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (isBangla) "পরের দিন" else "Next Day",
                        tint = themeColors.primaryText,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}
