package com.example.prayer.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.location.LocationHelper
import com.example.prayer.model.ForbiddenType
import java.time.LocalDate

class PrayerAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PRAYER_ALARM = "com.example.prayer.ACTION_PRAYER_ALARM"
        const val ACTION_FORBIDDEN_REMINDER = "com.example.prayer.ACTION_FORBIDDEN_REMINDER"

        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_FORBIDDEN_TYPE = "extra_forbidden_type"
        const val EXTRA_START_TIME = "extra_start_time"
        const val EXTRA_END_TIME = "extra_end_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        try {
            when (intent.action) {
                ACTION_PRAYER_ALARM -> {
                    val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Salat"
                    val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                    val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""

                    // Post simple, easily dismissible custom notification + in-app banner
                    PrayerNotificationHelper.showPrayerAlarmNotification(
                        context = context,
                        prayerName = prayerName,
                        startTimeStr = startTime,
                        endTimeStr = endTime
                    )

                    // Reschedule upcoming alarms so tomorrow's cycle continues automatically
                    rescheduleNextAlarms(context)
                }

                ACTION_FORBIDDEN_REMINDER -> {
                    val typeName = intent.getStringExtra(EXTRA_FORBIDDEN_TYPE) ?: ForbiddenType.POST_SUNRISE.name
                    val type = try {
                        ForbiddenType.valueOf(typeName)
                    } catch (_: Exception) {
                        ForbiddenType.POST_SUNRISE
                    }
                    val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                    val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""

                    PrayerNotificationHelper.showForbiddenReminderNotification(
                        context = context,
                        forbiddenType = type,
                        startTimeStr = startTime,
                        endTimeStr = endTime
                    )
                    rescheduleNextAlarms(context)
                }

                PrayerNotificationHelper.ACTION_DISMISS -> {
                    PrayerNotificationHelper.cancelSalatNotification(context)
                }

                PrayerNotificationHelper.ACTION_SNOOZE -> {
                    val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Salat"
                    val startTime = intent.getStringExtra(EXTRA_START_TIME) ?: ""
                    val endTime = intent.getStringExtra(EXTRA_END_TIME) ?: ""
                    PrayerNotificationHelper.cancelSalatNotification(context)
                    PrayerAlarmScheduler.scheduleSnooze(context, prayerName, startTime, endTime, 5)
                }
            }
        } catch (_: Throwable) {
            // Never crash in BroadcastReceiver
        }
    }

    private fun rescheduleNextAlarms(context: Context) {
        try {
            val loc = LocationHelper.getLastSavedLocation(context) ?: return
            val zoneId = PrayerCalculator.resolveZoneId(loc.latitude, loc.longitude)
            val today = LocalDate.now(zoneId)
            val todaySchedule = PrayerCalculator.calculateSchedule(
                date = today,
                lat = loc.latitude,
                lng = loc.longitude
            )
            val tomorrowSchedule = PrayerCalculator.calculateSchedule(
                date = today.plusDays(1),
                lat = loc.latitude,
                lng = loc.longitude
            )
            PrayerAlarmScheduler.scheduleAll(context, todaySchedule, tomorrowSchedule)
        } catch (_: Throwable) {
            // Ignore
        }
    }
}
