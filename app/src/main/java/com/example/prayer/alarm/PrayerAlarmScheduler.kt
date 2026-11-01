package com.example.prayer.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.MainActivity
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.location.LocationHelper
import com.example.prayer.model.ForbiddenType
import com.example.prayer.model.PrayerSchedule
import com.example.prayer.model.PrayerType
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object PrayerAlarmScheduler {

    private const val PREFS_NAME = "prayer_alarm_prefs"
    private const val KEY_SALAT_ALARMS_ENABLED = "key_salat_alarms_enabled"
    private const val KEY_FORBIDDEN_REMINDERS_ENABLED = "key_forbidden_reminders_enabled"
    private const val KEY_PREFIX_PRAYER = "key_prayer_"

    private const val REQUEST_CODE_SALAT_BASE = 1000
    private const val REQUEST_CODE_FORBIDDEN_BASE = 2000
    private const val REQUEST_CODE_SNOOZE = 9999

    fun isSalatAlarmsEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SALAT_ALARMS_ENABLED, true)
    }

    fun setSalatAlarmsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SALAT_ALARMS_ENABLED, enabled)
            .apply()
    }

    fun isPrayerAlarmEnabled(context: Context, prayerType: PrayerType): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREFIX_PRAYER + prayerType.name, true)
    }

    fun setPrayerAlarmEnabled(context: Context, prayerType: PrayerType, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PREFIX_PRAYER + prayerType.name, enabled)
            .apply()
    }

    fun isForbiddenRemindersEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_FORBIDDEN_REMINDERS_ENABLED, true)
    }

    fun setForbiddenRemindersEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FORBIDDEN_REMINDERS_ENABLED, enabled)
            .apply()
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAll(
        context: Context,
        todaySchedule: PrayerSchedule,
        tomorrowSchedule: PrayerSchedule? = null
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val savedLocation = LocationHelper.getLastSavedLocation(context)
        val zoneId = if (savedLocation != null) {
            PrayerCalculator.resolveZoneId(savedLocation.latitude, savedLocation.longitude)
        } else {
            ZoneId.systemDefault()
        }

        val now = LocalDateTime.now(zoneId)
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

        // 1. Schedule 5 Daily Salat Fullscreen Alarms
        if (isSalatAlarmsEnabled(context)) {
            val prayers = listOf(
                Pair(PrayerType.FAJR, todaySchedule.fajr to todaySchedule.fajrEnd),
                Pair(PrayerType.DHUHR, todaySchedule.dhuhr to todaySchedule.dhuhrEnd),
                Pair(PrayerType.ASR, todaySchedule.asr to todaySchedule.asrEnd),
                Pair(PrayerType.MAGHRIB, todaySchedule.maghrib to todaySchedule.maghribEnd),
                Pair(PrayerType.ISHA, todaySchedule.isha to todaySchedule.ishaEnd)
            )

            val tomorrowPrayers = tomorrowSchedule?.let {
                mapOf(
                    PrayerType.FAJR to (it.fajr to it.fajrEnd),
                    PrayerType.DHUHR to (it.dhuhr to it.dhuhrEnd),
                    PrayerType.ASR to (it.asr to it.asrEnd),
                    PrayerType.MAGHRIB to (it.maghrib to it.maghribEnd),
                    PrayerType.ISHA to (it.isha to it.ishaEnd)
                )
            }

            for ((prayerType, times) in prayers) {
                if (!isPrayerAlarmEnabled(context, prayerType)) {
                    cancelSalatAlarm(context, prayerType)
                    continue
                }

                var (startTime, endTime) = times
                var targetDateTime = LocalDateTime.of(todaySchedule.date, startTime)

                if (!targetDateTime.isAfter(now)) {
                    // Schedule for tomorrow if already passed today
                    val tomorrowTimes = tomorrowPrayers?.get(prayerType)
                    if (tomorrowTimes != null && tomorrowSchedule != null) {
                        startTime = tomorrowTimes.first
                        endTime = tomorrowTimes.second
                        targetDateTime = LocalDateTime.of(tomorrowSchedule.date, startTime)
                    } else {
                        targetDateTime = targetDateTime.plusDays(1)
                    }
                }

                val triggerMillis = targetDateTime.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleSalatAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    prayerType = prayerType,
                    triggerMillis = triggerMillis,
                    startTimeStr = startTime.format(timeFormatter),
                    endTimeStr = endTime.format(timeFormatter)
                )
            }
        } else {
            // Cancel all salat alarms
            for (p in PrayerType.values()) {
                cancelSalatAlarm(context, p)
            }
        }

        // 2. Schedule Forbidden Time (Makrooh) Reminders
        if (isForbiddenRemindersEnabled(context)) {
            val forbiddenItems = listOf(
                Triple(
                    ForbiddenType.POST_SUNRISE,
                    todaySchedule.sunriseMakroohStart,
                    todaySchedule.sunriseMakroohEnd
                ),
                Triple(
                    ForbiddenType.ZAWAL,
                    todaySchedule.zawalMakroohStart,
                    todaySchedule.zawalMakroohEnd
                ),
                Triple(
                    ForbiddenType.PRE_MAGHRIB,
                    todaySchedule.sunsetMakroohStart,
                    todaySchedule.sunsetMakroohEnd
                )
            )

            val tomorrowForbiddenItems = tomorrowSchedule?.let {
                mapOf(
                    ForbiddenType.POST_SUNRISE to (it.sunriseMakroohStart to it.sunriseMakroohEnd),
                    ForbiddenType.ZAWAL to (it.zawalMakroohStart to it.zawalMakroohEnd),
                    ForbiddenType.PRE_MAGHRIB to (it.sunsetMakroohStart to it.sunsetMakroohEnd)
                )
            }

            for ((type, start, end) in forbiddenItems) {
                var targetDateTime = LocalDateTime.of(todaySchedule.date, start)
                var startFormatted = start.format(timeFormatter)
                var endFormatted = end.format(timeFormatter)

                if (!targetDateTime.isAfter(now)) {
                    val tomTimes = tomorrowForbiddenItems?.get(type)
                    if (tomTimes != null && tomorrowSchedule != null) {
                        targetDateTime = LocalDateTime.of(tomorrowSchedule.date, tomTimes.first)
                        startFormatted = tomTimes.first.format(timeFormatter)
                        endFormatted = tomTimes.second.format(timeFormatter)
                    } else {
                        targetDateTime = targetDateTime.plusDays(1)
                    }
                }

                val triggerMillis = targetDateTime.atZone(zoneId).toInstant().toEpochMilli()
                scheduleSingleForbiddenReminder(
                    context = context,
                    alarmManager = alarmManager,
                    type = type,
                    triggerMillis = triggerMillis,
                    startTimeStr = startFormatted,
                    endTimeStr = endFormatted
                )
            }
        } else {
            for (type in ForbiddenType.values()) {
                cancelForbiddenReminder(context, type)
            }
        }
    }

    private fun scheduleExactOrFallback(
        context: Context,
        alarmManager: AlarmManager,
        triggerMillis: Long,
        requestCode: Int,
        pendingIntent: PendingIntent
    ) {
        val showIntent = Intent(context, MainActivity::class.java)
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canExact) {
            try {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
                return
            } catch (_: SecurityException) {
                // Fallback below
            }
        }

        // Reliable wake-up fallback even if exact alarm permission is pending
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (_: Exception) {
            // Ignore in restricted environments
        }
    }

    private fun scheduleSingleSalatAlarm(
        context: Context,
        alarmManager: AlarmManager,
        prayerType: PrayerType,
        triggerMillis: Long,
        startTimeStr: String,
        endTimeStr: String
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerType.displayName)
            putExtra(PrayerAlarmReceiver.EXTRA_START_TIME, startTimeStr)
            putExtra(PrayerAlarmReceiver.EXTRA_END_TIME, endTimeStr)
        }

        val requestCode = REQUEST_CODE_SALAT_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrFallback(context, alarmManager, triggerMillis, requestCode, pendingIntent)
    }

    private fun cancelSalatAlarm(context: Context, prayerType: PrayerType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
        }
        val requestCode = REQUEST_CODE_SALAT_BASE + prayerType.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun scheduleSingleForbiddenReminder(
        context: Context,
        alarmManager: AlarmManager,
        type: ForbiddenType,
        triggerMillis: Long,
        startTimeStr: String,
        endTimeStr: String
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_FORBIDDEN_REMINDER
            putExtra(PrayerAlarmReceiver.EXTRA_FORBIDDEN_TYPE, type.name)
            putExtra(PrayerAlarmReceiver.EXTRA_START_TIME, startTimeStr)
            putExtra(PrayerAlarmReceiver.EXTRA_END_TIME, endTimeStr)
        }

        val requestCode = REQUEST_CODE_FORBIDDEN_BASE + type.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrFallback(context, alarmManager, triggerMillis, requestCode, pendingIntent)
    }

    private fun cancelForbiddenReminder(context: Context, type: ForbiddenType) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_FORBIDDEN_REMINDER
        }
        val requestCode = REQUEST_CODE_FORBIDDEN_BASE + type.ordinal
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun scheduleSnooze(
        context: Context,
        prayerName: String,
        startTime: String,
        endTime: String,
        minutes: Int = 5
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerMillis = System.currentTimeMillis() + (minutes * 60 * 1000L)

        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmReceiver.ACTION_PRAYER_ALARM
            putExtra(PrayerAlarmReceiver.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerAlarmReceiver.EXTRA_START_TIME, startTime)
            putExtra(PrayerAlarmReceiver.EXTRA_END_TIME, endTime)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SNOOZE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactOrFallback(context, alarmManager, triggerMillis, REQUEST_CODE_SNOOZE, pendingIntent)
    }

    fun triggerTestSalatAlarm(
        context: Context,
        prayerName: String = "Fazr",
        forceFullScreenPreview: Boolean = false
    ) {
        try {
            val savedLocation = LocationHelper.getLastSavedLocation(context)
            val zoneId = if (savedLocation != null) {
                PrayerCalculator.resolveZoneId(savedLocation.latitude, savedLocation.longitude)
            } else {
                ZoneId.systemDefault()
            }
            val now = LocalTime.now(zoneId)
            val timeStr = now.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
            val endStr = now.plusHours(1).plusMinutes(15).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))

            PrayerNotificationHelper.showPrayerAlarmNotification(
                context = context,
                prayerName = prayerName,
                startTimeStr = timeStr,
                endTimeStr = endStr,
                forceFullScreenPreview = forceFullScreenPreview
            )
        } catch (_: Throwable) {
        }
    }

    fun triggerTestForbiddenNotification(
        context: Context,
        forbiddenType: ForbiddenType = ForbiddenType.POST_SUNRISE,
        forceFullScreenPreview: Boolean = false
    ) {
        try {
            val savedLocation = LocationHelper.getLastSavedLocation(context)
            val zoneId = if (savedLocation != null) {
                PrayerCalculator.resolveZoneId(savedLocation.latitude, savedLocation.longitude)
            } else {
                ZoneId.systemDefault()
            }
            val now = LocalTime.now(zoneId)
            val timeStr = now.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
            val endStr = now.plusMinutes(15).format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
            PrayerNotificationHelper.showForbiddenReminderNotification(
                context = context,
                forbiddenType = forbiddenType,
                startTimeStr = timeStr,
                endTimeStr = endStr,
                forceFullScreenPreview = forceFullScreenPreview
            )
        } catch (_: Throwable) {
        }
    }
}
