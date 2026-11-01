package com.example.prayer.alarm

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.prayer.model.AppLanguage
import com.example.prayer.model.ForbiddenType
import com.example.prayer.model.PrayerSettingsRepository
import com.example.prayer.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CustomBannerNotification(
    val id: Int,
    val badgeText: String,
    val title: String,
    val subtitle: String,
    val timeWindow: String,
    val isForbidden: Boolean,
    val prayerName: String = "Salat",
    val startTimeStr: String = "",
    val endTimeStr: String = ""
)

object PrayerNotificationHelper {
    const val CHANNEL_ID_PRAYER_ALARM = "prayer_fullscreen_salat_alarm_v6"
    const val CHANNEL_ID_FORBIDDEN = "prayer_fullscreen_forbidden_alarm_v6"

    const val NOTIFICATION_ID_SALAT_ALARM = 1001
    const val NOTIFICATION_ID_FORBIDDEN = 1002

    const val ACTION_DISMISS = "com.example.prayer.ACTION_DISMISS_ALARM"
    const val ACTION_SNOOZE = "com.example.prayer.ACTION_SNOOZE_ALARM"

    private val _activeBanner = MutableStateFlow<CustomBannerNotification?>(null)
    val activeBanner: StateFlow<CustomBannerNotification?> = _activeBanner.asStateFlow()

    fun areSystemNotificationsEnabled(context: Context): Boolean {
        return try {
            val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            permissionGranted && NotificationManagerCompat.from(context).areNotificationsEnabled()
        } catch (_: Throwable) {
            false
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    fun isScreenOffOrLocked(context: Context): Boolean {
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val isInteractive = powerManager?.isInteractive ?: true
            val isLocked = keyguardManager?.isKeyguardLocked ?: false
            !isInteractive || isLocked
        } catch (_: Throwable) {
            false
        }
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

                // Clean up legacy channels so only v6 channels exist
                listOf(
                    "prayer_alarm_channel",
                    "forbidden_reminder_channel",
                    "salat_wakeup_alarm_v2",
                    "makrooh_forbidden_alert_v2",
                    "salat_wakeup_alarm_v3",
                    "makrooh_forbidden_alert_v3",
                    "salat_wakeup_alarm_v4",
                    "makrooh_forbidden_alert_v4",
                    "prayer_fullscreen_salat_alarm_v5",
                    "prayer_fullscreen_forbidden_alarm_v5"
                ).forEach { oldId ->
                    try {
                        notificationManager.deleteNotificationChannel(oldId)
                    } catch (_: Throwable) {
                    }
                }

                // 1. Salat Full-Screen + Custom Notification Channel
                // Sound & vibration are set to null/false here because AlarmSoundController
                // plays the chime & vibration directly (allowing Mute, Volume keys, and 20s auto-stop to work cleanly without double audio).
                val alarmChannel = NotificationChannel(
                    CHANNEL_ID_PRAYER_ALARM,
                    "Salat Wake-Up & Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Wakes screen with full-screen alert when screen is off, or shows custom banner when screen is on"
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(true)
                    lightColor = Color.CYAN
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setBypassDnd(true)
                }
                notificationManager.createNotificationChannel(alarmChannel)

                // 2. Forbidden Time (Makrooh) Notification Channel
                val forbiddenChannel = NotificationChannel(
                    CHANNEL_ID_FORBIDDEN,
                    "Forbidden Time (Makrooh) Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when forbidden prayer times (Sunrise, Zawal, Sunset) begin"
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(true)
                    lightColor = Color.RED
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(forbiddenChannel)
            } catch (_: Throwable) {
                // Safe ignore
            }
        }
    }

    private fun clearExistingSystemNotifications(context: Context) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID_SALAT_ALARM)
            ForbiddenType.values().forEach { type ->
                notificationManager?.cancel(NOTIFICATION_ID_FORBIDDEN + type.ordinal)
            }
        } catch (_: Throwable) {
        }
    }

    fun showPrayerAlarmNotification(
        context: Context,
        prayerName: String,
        startTimeStr: String,
        endTimeStr: String,
        forceFullScreenPreview: Boolean = false
    ) {
        val isBangla = PrayerSettingsRepository.getAppLanguage(context) == AppLanguage.BANGLA
        val localizedPrayer = DateUtils.localizePrayerName(prayerName, isBangla)
        val locStart = DateUtils.localizeTime(startTimeStr, isBangla)
        val locEnd = DateUtils.localizeTime(endTimeStr, isBangla)

        val timeRange = if (locStart.isNotEmpty() && locEnd.isNotEmpty()) {
            "$locStart - $locEnd"
        } else {
            locStart.ifEmpty { if (isBangla) "এখন চলমান" else "Now Active" }
        }
        val title = if (isBangla) "$localizedPrayer নামাজের ওয়াক্ত শুরু হয়েছে" else "$prayerName Salat Time Has Started"
        val subtitle = if (locEnd.isNotEmpty()) {
            if (isBangla) "ওয়াক্তের সময়: $locStart - $locEnd • বন্ধ করতে ট্যাপ করুন"
            else "Waqt window: $startTimeStr - $endTimeStr • Tap Dismiss to clear"
        } else {
            if (isBangla) "ওয়াক্ত শুরু: $locStart • বন্ধ করতে ট্যাপ করুন"
            else "Waqt started at $startTimeStr • Tap Dismiss to clear"
        }

        // Clear any previous notification so only 1 notification is ever active at a time
        clearExistingSystemNotifications(context)

        // Update fallback banner state (only displayed in-app if OS notifications are blocked/disabled)
        _activeBanner.value = CustomBannerNotification(
            id = NOTIFICATION_ID_SALAT_ALARM,
            badgeText = if (isBangla) "নামাজের ওয়াক্ত" else "SALAT WAQT",
            title = title,
            subtitle = subtitle,
            timeWindow = timeRange,
            isForbidden = false,
            prayerName = prayerName,
            startTimeStr = startTimeStr,
            endTimeStr = endTimeStr
        )

        // Play chime + vibration
        AlarmSoundController.startAlarm(context)

        // If user explicitly tapped "Preview Full-Screen Lockscreen Alert", open the full-screen alert
        // directly without posting a second heads-up banner on top of it
        if (forceFullScreenPreview) {
            launchFullScreenActivitySafely(
                context = context,
                prayerName = prayerName,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr,
                isForbidden = false,
                subtitle = subtitle
            )
            return
        }

        // Post single Android notification with fullScreenIntent (wakes lockscreen automatically when screen is off)
        try {
            val notification = buildPrayerAlarmNotification(
                context = context,
                prayerName = prayerName,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr
            )
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(NOTIFICATION_ID_SALAT_ALARM, notification)
        } catch (_: Throwable) {
            // Fallback in-app custom notification banner is already updated
        }

        // If device screen is currently off or locked, ensure full-screen wake-up activity launches
        if (isScreenOffOrLocked(context)) {
            launchFullScreenActivitySafely(
                context = context,
                prayerName = prayerName,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr,
                isForbidden = false,
                subtitle = subtitle
            )
        }
    }

    fun launchFullScreenActivitySafely(
        context: Context,
        prayerName: String,
        startTimeStr: String,
        endTimeStr: String,
        isForbidden: Boolean,
        subtitle: String = ""
    ) {
        try {
            val fsIntent = Intent(context, PrayerAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra(PrayerAlarmActivity.EXTRA_PRAYER_NAME, prayerName)
                putExtra(PrayerAlarmActivity.EXTRA_START_TIME, startTimeStr)
                putExtra(PrayerAlarmActivity.EXTRA_END_TIME, endTimeStr)
                putExtra(PrayerAlarmActivity.EXTRA_IS_FORBIDDEN, isForbidden)
                putExtra(PrayerAlarmActivity.EXTRA_SUBTITLE, subtitle)
            }
            context.startActivity(fsIntent)
        } catch (_: Throwable) {
            // If OS blocks direct startActivity in background, the notification's setFullScreenIntent handles it
        }
    }

    fun buildPrayerAlarmNotification(
        context: Context,
        prayerName: String,
        startTimeStr: String,
        endTimeStr: String
    ): Notification {
        createNotificationChannels(context)
        val isBangla = PrayerSettingsRepository.getAppLanguage(context) == AppLanguage.BANGLA
        val localizedPrayer = DateUtils.localizePrayerName(prayerName, isBangla)
        val locStart = DateUtils.localizeTime(startTimeStr, isBangla)
        val locEnd = DateUtils.localizeTime(endTimeStr, isBangla)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SALAT_ALARM,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Full-Screen Wake-Up Intent for when screen is off or locked (Muslim's Day style)
        val fullScreenIntent = Intent(context, PrayerAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_USER_ACTION
            putExtra(PrayerAlarmActivity.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmActivity.EXTRA_START_TIME, startTimeStr)
            putExtra(PrayerAlarmActivity.EXTRA_END_TIME, endTimeStr)
            putExtra(PrayerAlarmActivity.EXTRA_IS_FORBIDDEN, false)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID_SALAT_ALARM + 500,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Dismiss Action (stops sound & clears notification immediately)
        val dismissIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_DISMISS
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            201,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze Action (5 mins)
        val snoozeIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_START_TIME, startTimeStr)
            putExtra(PrayerAlarmReceiver.EXTRA_END_TIME, endTimeStr)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            202,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timeRange = if (locStart.isNotEmpty() && locEnd.isNotEmpty()) {
            "$locStart - $locEnd"
        } else {
            locStart
        }
        val title = if (isBangla) "$localizedPrayer নামাজের ওয়াক্ত শুরু হয়েছে" else "$prayerName Salat Time Has Started"
        val subtitle = if (locEnd.isNotEmpty()) {
            if (isBangla) "ওয়াক্তের সময়: $locStart - $locEnd • বন্ধ করতে ট্যাপ করুন"
            else "Waqt window: $startTimeStr - $endTimeStr • Tap Dismiss to clear"
        } else {
            if (isBangla) "ওয়াক্ত শুরু: $locStart • বন্ধ করতে ট্যাপ করুন"
            else "Waqt started at $startTimeStr • Tap Dismiss to clear"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_PRAYER_ALARM)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(contentPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setDeleteIntent(dismissPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                if (isBangla) "বন্ধ করুন" else "Dismiss",
                dismissPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_recent_history,
                if (isBangla) "৫মি স্নুজ" else "Snooze 5m",
                snoozePendingIntent
            )

        // Attach Custom RemoteViews layout with direct inline Dismiss button
        try {
            val customView = RemoteViews(context.packageName, R.layout.custom_prayer_notification).apply {
                setTextViewText(R.id.custom_notif_badge, if (isBangla) "নামাজের ওয়াক্ত" else "SALAT WAQT")
                setTextViewText(R.id.custom_notif_time, timeRange)
                setTextViewText(R.id.custom_notif_title, title)
                setTextViewText(R.id.custom_notif_subtitle, subtitle)
                setOnClickPendingIntent(R.id.custom_notif_dismiss_btn, dismissPendingIntent)
            }
            builder.setCustomContentView(customView)
            builder.setCustomBigContentView(customView)
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        } catch (_: Throwable) {
            // Standard notification fallback
        }

        return builder.build()
    }

    fun showForbiddenReminderNotification(
        context: Context,
        forbiddenType: ForbiddenType,
        startTimeStr: String,
        endTimeStr: String,
        forceFullScreenPreview: Boolean = false
    ) {
        createNotificationChannels(context)
        val isBangla = PrayerSettingsRepository.getAppLanguage(context) == AppLanguage.BANGLA
        val locStart = DateUtils.localizeTime(startTimeStr, isBangla)
        val locEnd = DateUtils.localizeTime(endTimeStr, isBangla)

        val title = if (isBangla) {
            forbiddenType.banglaTitle
        } else {
            when (forbiddenType) {
                ForbiddenType.POST_SUNRISE -> "Sunrise Forbidden Time (Makrooh)"
                ForbiddenType.ZAWAL -> "Midday Zawal Forbidden Time (Makrooh)"
                ForbiddenType.PRE_MAGHRIB -> "Pre-Sunset Forbidden Time (Makrooh)"
            }
        }
        val timeRange = if (locStart.isNotEmpty() && locEnd.isNotEmpty()) {
            "$locStart - $locEnd"
        } else {
            locStart
        }
        val text = if (isBangla) {
            "নিষিদ্ধ সময়: $timeRange • এই সময়ে সকল নামাজ ও সিজদা নিষিদ্ধ"
        } else {
            "Prohibited window: $timeRange • All Salah & Sajdah prohibited"
        }

        clearExistingSystemNotifications(context)

        // Update fallback banner state (only displayed in-app if OS notifications are blocked/disabled)
        _activeBanner.value = CustomBannerNotification(
            id = NOTIFICATION_ID_FORBIDDEN + forbiddenType.ordinal,
            badgeText = if (isBangla) "মাকরূহ সতর্কতা" else "MAKROOH ALERT",
            title = title,
            subtitle = text,
            timeWindow = timeRange,
            isForbidden = true
        )

        AlarmSoundController.startAlarm(context)

        if (forceFullScreenPreview) {
            launchFullScreenActivitySafely(
                context = context,
                prayerName = title,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr,
                isForbidden = true,
                subtitle = text
            )
            return
        }

        try {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                301 + forbiddenType.ordinal,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val fullScreenIntent = Intent(context, PrayerAlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION
                putExtra(PrayerAlarmActivity.EXTRA_PRAYER_NAME, title)
                putExtra(PrayerAlarmActivity.EXTRA_START_TIME, startTimeStr)
                putExtra(PrayerAlarmActivity.EXTRA_END_TIME, endTimeStr)
                putExtra(PrayerAlarmActivity.EXTRA_IS_FORBIDDEN, true)
                putExtra(PrayerAlarmActivity.EXTRA_SUBTITLE, text)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                401 + forbiddenType.ordinal,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val dismissIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                350 + forbiddenType.ordinal,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID_FORBIDDEN)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setOngoing(false)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setDeleteIntent(dismissPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

            try {
                val customView = RemoteViews(context.packageName, R.layout.custom_prayer_notification).apply {
                    setTextViewText(R.id.custom_notif_badge, "MAKROOH ALERT")
                    setInt(R.id.custom_notif_badge, "setBackgroundResource", R.drawable.bg_custom_notif_badge_makrooh)
                    setTextColor(R.id.custom_notif_badge, Color.WHITE)
                    setTextViewText(R.id.custom_notif_time, timeRange)
                    setTextColor(R.id.custom_notif_time, Color.parseColor("#FDA4AF"))
                    setTextViewText(R.id.custom_notif_title, title)
                    setTextViewText(R.id.custom_notif_subtitle, text)
                    setOnClickPendingIntent(R.id.custom_notif_dismiss_btn, dismissPendingIntent)
                }
                builder.setCustomContentView(customView)
                builder.setCustomBigContentView(customView)
                builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            } catch (_: Throwable) {
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.notify(NOTIFICATION_ID_FORBIDDEN + forbiddenType.ordinal, builder.build())
        } catch (_: Throwable) {
        }

        if (isScreenOffOrLocked(context)) {
            launchFullScreenActivitySafely(
                context = context,
                prayerName = title,
                startTimeStr = startTimeStr,
                endTimeStr = endTimeStr,
                isForbidden = true,
                subtitle = text
            )
        }
    }

    fun cancelSalatNotification(context: Context) {
        AlarmSoundController.stopAlarm()
        _activeBanner.value = null
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.cancel(NOTIFICATION_ID_SALAT_ALARM)
            ForbiddenType.values().forEach { type ->
                notificationManager?.cancel(NOTIFICATION_ID_FORBIDDEN + type.ordinal)
            }
        } catch (_: Throwable) {
        }
    }
}
