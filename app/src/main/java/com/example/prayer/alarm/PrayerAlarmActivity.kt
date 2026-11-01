package com.example.prayer.alarm

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainActivity
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.location.LocationHelper
import com.example.prayer.model.AppLanguage
import com.example.prayer.model.PrayerSettingsRepository
import com.example.prayer.util.DateUtils
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Muslim's Day style Full-Screen Lockscreen Wake-Up Alert Activity.
 * Wakes the device when the screen is off or locked and displays a rich, high-contrast
 * full-screen Salat / Makrooh alert over the lock screen with effortless 1-tap Dismiss & Snooze.
 * Automatically closes after 60 seconds if untouched so the screen never stays on for hours
 * while keeping the notification in the system tray.
 */
class PrayerAlarmActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
        const val EXTRA_IS_FORBIDDEN = "extra_is_forbidden"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val AUTO_CLOSE_TIMEOUT_MS = 60_000L
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoCloseRunnable = Runnable {
        // After 60 seconds of no interaction, silence audio, release screen-on lock, and close full-screen overlay
        // while preserving the standard notification in the status bar shade for when the user wakes up.
        AlarmSoundController.setMuted(this, true)
        clearKeepScreenOnFlag()
        finishAndRemoveTaskSafely()
    }

    private var prayerNameState by mutableStateOf("Fazr")
    private var startTimeState by mutableStateOf("")
    private var endTimeState by mutableStateOf("")
    private var isForbiddenState by mutableStateOf(false)
    private var customSubtitleState by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureLockScreenWakeUp()
        scheduleAutoCloseTimeout()
        AlarmSoundController.registerActivityDismissListener {
            runOnUiThread {
                finishAndRemoveTaskSafely()
            }
        }
        enableEdgeToEdge()
        readExtrasFromIntent(intent)

        setContent {
            MyApplicationTheme {
                FullScreenLockscreenAlarmUI(
                    prayerName = prayerNameState,
                    startTimeStr = startTimeState,
                    endTimeStr = endTimeState,
                    isForbidden = isForbiddenState,
                    customSubtitle = customSubtitleState,
                    onDismiss = {
                        PrayerNotificationHelper.cancelSalatNotification(this)
                        finishAndRemoveTaskSafely()
                    },
                    onSnooze = {
                        PrayerNotificationHelper.cancelSalatNotification(this)
                        PrayerAlarmScheduler.scheduleSnooze(
                            context = this,
                            prayerName = prayerNameState,
                            startTime = startTimeState,
                            endTime = endTimeState,
                            minutes = 5
                        )
                        finishAndRemoveTaskSafely()
                    },
                    onOpenSchedule = {
                        PrayerNotificationHelper.cancelSalatNotification(this)
                        unlockAndOpenMainActivity()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureLockScreenWakeUp()
        scheduleAutoCloseTimeout()
        readExtrasFromIntent(intent)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AlarmSoundController.setMuted(this, true)
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun scheduleAutoCloseTimeout() {
        mainHandler.removeCallbacks(autoCloseRunnable)
        mainHandler.postDelayed(autoCloseRunnable, AUTO_CLOSE_TIMEOUT_MS)
    }

    private fun clearKeepScreenOnFlag() {
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } catch (_: Throwable) {
        }
    }

    private fun readExtrasFromIntent(source: Intent?) {
        prayerNameState = source?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Fazr"
        startTimeState = source?.getStringExtra(EXTRA_START_TIME) ?: ""
        endTimeState = source?.getStringExtra(EXTRA_END_TIME) ?: ""
        isForbiddenState = source?.getBooleanExtra(EXTRA_IS_FORBIDDEN, false) ?: false
        customSubtitleState = source?.getStringExtra(EXTRA_SUBTITLE) ?: ""
    }

    private fun configureLockScreenWakeUp() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }

            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )

            // Acquire a 60s WakeLock if screen is currently off so display lights up immediately
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null && !powerManager.isInteractive) {
                releaseWakeLockSafely()
                @Suppress("DEPRECATION")
                val wl = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                    "PrayerTimes:FullScreenAlarmWakeLock"
                )
                wl.acquire(AUTO_CLOSE_TIMEOUT_MS)
                wakeLock = wl
            }
        } catch (_: Throwable) {
            // Never crash on OEM window/power manager differences
        }
    }

    private fun unlockAndOpenMainActivity() {
        try {
            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                if (keyguardManager?.isKeyguardLocked == true) {
                    keyguardManager.requestDismissKeyguard(
                        this,
                        object : KeyguardManager.KeyguardDismissCallback() {
                            override fun onDismissSucceeded() {
                                startActivity(openIntent)
                                finishAndRemoveTaskSafely()
                            }

                            override fun onDismissCancelled() {
                                finishAndRemoveTaskSafely()
                            }

                            override fun onDismissError() {
                                startActivity(openIntent)
                                finishAndRemoveTaskSafely()
                            }
                        }
                    )
                    return
                }
            }
            startActivity(openIntent)
        } catch (_: Throwable) {
        }
        finishAndRemoveTaskSafely()
    }

    private fun finishAndRemoveTaskSafely() {
        mainHandler.removeCallbacks(autoCloseRunnable)
        clearKeepScreenOnFlag()
        releaseWakeLockSafely()
        try {
            finishAndRemoveTask()
        } catch (_: Throwable) {
            finish()
        }
    }

    private fun releaseWakeLockSafely() {
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
        } catch (_: Throwable) {
        } finally {
            wakeLock = null
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(autoCloseRunnable)
        AlarmSoundController.registerActivityDismissListener(null)
        releaseWakeLockSafely()
        super.onDestroy()
    }
}

@Composable
fun FullScreenLockscreenAlarmUI(
    prayerName: String,
    startTimeStr: String,
    endTimeStr: String,
    isForbidden: Boolean,
    customSubtitle: String,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
    onOpenSchedule: () -> Unit
) {
    val context = LocalContext.current
    val isBangla = remember { PrayerSettingsRepository.getAppLanguage(context) == AppLanguage.BANGLA }
    val isMuted by AlarmSoundController.isMuted.collectAsState()
    val isRinging by AlarmSoundController.isAlarmRinging.collectAsState()

    val savedLocation = remember { LocationHelper.getLastSavedLocation(context) }
    val zoneId = remember(savedLocation) {
        if (savedLocation != null) {
            PrayerCalculator.resolveZoneId(savedLocation.latitude, savedLocation.longitude)
        } else {
            ZoneId.systemDefault()
        }
    }

    var currentTime by remember { mutableStateOf(LocalTime.now(zoneId)) }
    val todayDate = remember { LocalDate.now(zoneId) }
    val hijriDate = remember(isBangla) { DateUtils.getHijriDate(todayDate, DateUtils.getHijriOffset(context), isBangla) }
    val gregorianDate = remember(isBangla) {
        DateUtils.formatFullGregorianDate(todayDate, isBangla)
    }

    LaunchedEffect(zoneId) {
        while (true) {
            currentTime = LocalTime.now(zoneId)
            delay(1000L)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LockscreenAlarmPulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val primaryAccent = if (isForbidden) Color(0xFFF43F5E) else Color(0xFF10B981)
    val secondaryAccent = if (isForbidden) Color(0xFFF59E0B) else Color(0xFF38BDF8)
    val goldAccent = Color(0xFFFDE047)

    val locStart = DateUtils.localizeTime(startTimeStr, isBangla)
    val locEnd = DateUtils.localizeTime(endTimeStr, isBangla)
    val localizedPrayerTitle = DateUtils.localizePrayerName(prayerName, isBangla)

    val timeWindowText = remember(locStart, locEnd, isBangla) {
        when {
            locStart.isNotEmpty() && locEnd.isNotEmpty() -> "$locStart – $locEnd"
            locStart.isNotEmpty() -> if (isBangla) "শুরু: $locStart" else "Starts at $locStart"
            else -> if (isBangla) "চলমান ওয়াক্তের সময়" else "Active Waqt Window"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isForbidden) {
                        listOf(
                            Color(0xFF1F0812),
                            Color(0xFF2D0A1A),
                            Color(0xFF130912)
                        )
                    } else {
                        listOf(
                            Color(0xFF041C24),
                            Color(0xFF072A30),
                            Color(0xFF061524)
                        )
                    }
                )
            )
            .testTag("fullscreen_alarm_screen")
    ) {
        // Atmospheric Geometric Halo Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.38f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryAccent.copy(alpha = 0.28f * glowAlpha),
                        secondaryAccent.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.width * 0.72f * ringScale
                ),
                center = Offset(cx, cy),
                radius = size.width * 0.72f * ringScale
            )

            // Concentric spiritual rings
            drawCircle(
                color = primaryAccent.copy(alpha = 0.18f * glowAlpha),
                radius = size.width * 0.36f * ringScale,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = goldAccent.copy(alpha = 0.14f),
                radius = size.width * 0.26f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            // Top Section: Live Digital Clock + Hijri & Gregorian Date
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                val locCity = DateUtils.localizeLocationName(
                    savedLocation?.locationName ?: "Bhurungamari",
                    isBangla
                ).substringBefore(",")
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(50))
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$hijriDate  •  $locCity",
                        color = Color(0xFFA7F3D0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = DateUtils.localizeTime(
                        currentTime.format(DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH)),
                        isBangla
                    ),
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = gregorianDate,
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Center Section: Pulsing Mosque/Alarm Emblem + Arabic Call + Prayer Title & Waqt Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pulsing Emblem Orb
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    primaryAccent.copy(alpha = 0.42f),
                                    primaryAccent.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .border(
                            width = 2.dp,
                            brush = Brush.verticalGradient(
                                listOf(goldAccent.copy(alpha = 0.9f), primaryAccent.copy(alpha = 0.7f))
                            ),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (isForbidden) Icons.Default.Warning else Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = goldAccent,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(primaryAccent.copy(alpha = 0.25f))
                        .border(1.dp, primaryAccent.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isForbidden) {
                            if (isBangla) "মাকরূহ তাহরীমী সতর্কতা" else "MAKROOH TAHRIMI ALERT"
                        } else {
                            if (isBangla) "নামাজের ওয়াক্ত শুরু হয়েছে" else "SALAT WAQT STARTED"
                        },
                        color = if (isForbidden) Color(0xFFFECDD3) else Color(0xFF6EE7B7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Arabic Calligraphic Phrase
                Text(
                    text = if (isForbidden) "وقت الكراهة — لا صلاة الآن" else "حَيَّ عَلَى الصَّلَاةِ • حَيَّ عَلَى الْفَلَاحِ",
                    color = goldAccent,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Main Prayer Title
                Text(
                    text = if (isForbidden) {
                        localizedPrayerTitle
                    } else {
                        if (isBangla) "$localizedPrayerTitle নামাজ" else "$prayerName Salat"
                    },
                    color = Color.White,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (customSubtitle.isNotEmpty()) {
                        customSubtitle
                    } else if (isForbidden) {
                        if (isBangla) {
                            "এই সময়ে সকল ফরজ, সুন্নত ও নফল নামাজ পড়া নিষিদ্ধ।"
                        } else {
                            "All obligatory & voluntary prayers are prohibited during this window."
                        }
                    } else {
                        if (isBangla) {
                            "এসো নামাজের দিকে, এসো কল্যাণের দিকে — ওয়াক্ত এখন চলমান।"
                        } else {
                            "Come to Prayer, Come to Success — Waqt is now active."
                        }
                    },
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Permitted Waqt Time Window Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.09f))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    primaryAccent.copy(alpha = 0.65f),
                                    goldAccent.copy(alpha = 0.45f),
                                    primaryAccent.copy(alpha = 0.65f)
                                )
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isForbidden) {
                                if (isBangla) "নিষিদ্ধ সময়সীমা" else "PROHIBITED WINDOW"
                            } else {
                                if (isBangla) "নির্ধারিত ওয়াক্তের সময়সীমা" else "PERMITTED WAQT WINDOW"
                            },
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = timeWindowText,
                            color = goldAccent,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Bottom Section: Mute Chime, Snooze 5m, Open Schedule, and Massive 1-Tap Dismiss Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Quick Audio Mute / Unmute + Open Schedule row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mute / Unmute Chime button
                    OutlinedButton(
                        onClick = {
                            AlarmSoundController.setMuted(context, !isMuted)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("fullscreen_alarm_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted || !isRinging) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isBangla) "শব্দ বন্ধ" else "Mute",
                            tint = if (isMuted) Color(0xFFFDA4AF) else Color(0xFF6EE7B7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isMuted || !isRinging) {
                                if (isBangla) "শব্দ নীরব" else "Sound Muted"
                            } else {
                                if (isBangla) "শব্দ থামান" else "Mute Sound"
                            },
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isForbidden) {
                        // Snooze 5 Minutes button
                        OutlinedButton(
                            onClick = onSnooze,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFDE047)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("fullscreen_alarm_snooze_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Snooze,
                                contentDescription = if (isBangla) "৫ মিনিট স্নুজ" else "Snooze 5 minutes",
                                tint = Color(0xFFFDE047),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBangla) "৫ মিনিট স্নুজ" else "Snooze 5m",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Large 1-Tap Dismiss Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isForbidden) Color(0xFFE11D48) else Color(0xFF10B981)
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("fullscreen_alarm_dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (isBangla) "এলার্ম বন্ধ করুন" else "Dismiss Alarm",
                        tint = if (isForbidden) Color.White else Color(0xFF022C22),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isBangla) "এলার্ম বন্ধ করুন" else "Dismiss Alarm",
                        color = if (isForbidden) Color.White else Color(0xFF022C22),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isBangla) "পূর্ণ নামাজের সময়সূচি খুলুন" else "Open Full Prayer Schedule",
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenSchedule() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
