package com.example.prayer.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.location.LocationHelper
import com.example.prayer.model.PrayerSettingsRepository
import java.time.LocalDate

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            val userLocation = LocationHelper.getLastSavedLocation(context) ?: return
            val zoneId = PrayerCalculator.resolveZoneId(userLocation.latitude, userLocation.longitude)
            val today = LocalDate.now(zoneId)
            val calcMethod = PrayerSettingsRepository.getCalculationMethod(context)
            val asrMethod = PrayerSettingsRepository.getAsrJuristicMethod(context)
            val offsets = PrayerSettingsRepository.getMinuteOffsets(context)
            val todaySchedule = PrayerCalculator.calculateSchedule(
                date = today,
                lat = userLocation.latitude,
                lng = userLocation.longitude,
                calculationMethod = calcMethod,
                asrJuristicMethod = asrMethod,
                minuteOffsets = offsets
            )
            val tomorrowSchedule = PrayerCalculator.calculateSchedule(
                date = today.plusDays(1),
                lat = userLocation.latitude,
                lng = userLocation.longitude,
                calculationMethod = calcMethod,
                asrJuristicMethod = asrMethod,
                minuteOffsets = offsets
            )
            PrayerAlarmScheduler.scheduleAll(context, todaySchedule, tomorrowSchedule)
        }
    }
}
