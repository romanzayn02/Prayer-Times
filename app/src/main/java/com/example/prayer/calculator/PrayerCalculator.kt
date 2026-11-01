package com.example.prayer.calculator

import com.example.prayer.model.ActiveWaqtInfo
import com.example.prayer.model.AsrJuristicMethod
import com.example.prayer.model.CalculationMethod
import com.example.prayer.model.ForbiddenStatus
import com.example.prayer.model.ForbiddenType
import com.example.prayer.model.NextPrayerInfo
import com.example.prayer.model.PrayerMinuteOffsets
import com.example.prayer.model.PrayerSchedule
import com.example.prayer.model.PrayerType
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

/**
 * Astronomical Islamic Prayer Time Calculator:
 * - Default calibration: Bhurungamari, Bangladesh (Lat: 26.1250° N, Lng: 89.6833° E)
 * - Default Calculation Method: University of Islamic Sciences, Karachi (Fajr: 18°, Isha: 18°)
 * - Default Juristic Method: Hanafi (Asr shadow factor: 2.0)
 * - Supports multiple Calculation Authorities, Hanafi/Standard Asr Fiqh, and per-prayer minute offsets
 * - Dynamic Timezone resolution for any GPS coordinate worldwide
 * - Excludes Forbidden / Makrooh times from displayed permitted prayer windows:
 *   - Asr permitted window ends 1 minute before Pre-Sunset Makrooh (15m before Maghrib)
 *   - Isha preferred window ends at true Islamic Midnight (Nisf al-Layl: midpoint between Maghrib & Fazr)
 * - Makrooh Tahrimi Forbidden windows:
 *   1. 15 minutes after sunrise
 *   2. 10 minutes before Dhuhr (Zawal)
 *   3. 15 minutes before Maghrib (Ghuroob)
 */
object PrayerCalculator {
    const val BHURUNGAMARI_LAT = 26.1250
    const val BHURUNGAMARI_LNG = 89.6833
    const val TIME_ZONE_OFFSET = 6.0 // UTC+6 (Bangladesh Standard Time)

    // Karachi Method Twilight angles
    const val FAJR_ANGLE = 18.0
    const val ISHA_ANGLE = 18.0

    // Astronomical standard sun altitude at horizon (accounting for 34' refraction + 16' solar radius)
    const val SUNRISE_SUNSET_ANGLE = -0.8333

    // Hanafi Asr shadow multiplier
    const val HANAFI_SHADOW_FACTOR = 2.0

    private fun degToRad(deg: Double): Double = deg * PI / 180.0
    private fun radToDeg(rad: Double): Double = rad * 180.0 / PI

    private fun fixAngle(deg: Double): Double {
        var a = deg - 360.0 * floor(deg / 360.0)
        if (a < 0.0) a += 360.0
        return a
    }

    private fun fixHour(hours: Double): Double {
        var h = hours - 24.0 * floor(hours / 24.0)
        if (h < 0.0) h += 24.0
        return h
    }

    /**
     * Resolves the accurate ZoneId for the given GPS coordinates so that local clock time
     * and astronomical solar transit are always in the same timezone.
     */
    fun resolveZoneId(lat: Double, lng: Double): ZoneId {
        // Bangladesh coordinates (including Bhurungamari & Dhaka)
        if (lat in 20.5..26.7 && lng in 88.0..92.7) {
            return ZoneId.of("Asia/Dhaka")
        }
        // India coordinates
        if (lat in 6.0..37.0 && lng in 68.0..97.5) {
            return ZoneId.of("Asia/Kolkata")
        }
        // Pakistan coordinates
        if (lat in 23.5..37.1 && lng in 60.8..77.0) {
            return ZoneId.of("Asia/Karachi")
        }

        val sysZone = ZoneId.systemDefault()
        val sysOffsetHours = sysZone.rules.getOffset(LocalDate.now().atTime(12, 0)).totalSeconds / 3600.0
        val solarOffsetHours = lng / 15.0

        return if (abs(sysOffsetHours - solarOffsetHours) <= 2.5) {
            sysZone
        } else {
            val offsetSeconds = (solarOffsetHours.roundToInt() * 3600).coerceIn(-43200, 50400)
            ZoneOffset.ofTotalSeconds(offsetSeconds)
        }
    }

    /**
     * Resolves the timezone offset in hours (e.g. +6.0 for Bangladesh) for the given date and coordinates.
     */
    fun resolveTimeZoneOffset(date: LocalDate, lat: Double, lng: Double): Double {
        if (lat in 20.5..26.7 && lng in 88.0..92.7) {
            return TIME_ZONE_OFFSET
        }
        val zoneId = resolveZoneId(lat, lng)
        return zoneId.rules.getOffset(date.atTime(12, 0)).totalSeconds / 3600.0
    }

    private fun julianDate(year: Int, month: Int, day: Int, hourOfDay: Double = 12.0): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + (hourOfDay / 24.0) + b - 1524.5
    }

    /**
     * Computes Sun coordinates: Declination (degrees) and Equation of Time (hours)
     */
    private data class SunCoordinates(val declination: Double, val equationOfTime: Double)

    private fun computeSunCoordinates(jd: Double): SunCoordinates {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * sin(degToRad(g)) + 0.020 * sin(degToRad(2.0 * g)))
        val e = 23.439 - 0.00000036 * d

        val sinDelta = sin(degToRad(e)) * sin(degToRad(l))
        val delta = radToDeg(asin(sinDelta))

        val raDeg = radToDeg(atan2(cos(degToRad(e)) * sin(degToRad(l)), cos(degToRad(l))))
        val raHours = fixAngle(raDeg) / 15.0

        var eqtHours = (q / 15.0) - raHours
        while (eqtHours > 12.0) eqtHours -= 24.0
        while (eqtHours < -12.0) eqtHours += 24.0

        return SunCoordinates(declination = delta, equationOfTime = eqtHours)
    }

    /**
     * Hour angle H in hours for a given sun altitude angle
     */
    private fun computeHourAngle(altitudeAngle: Double, lat: Double, declination: Double): Double {
        val latRad = degToRad(lat)
        val declRad = degToRad(declination)
        val altRad = degToRad(altitudeAngle)

        val cosH = (sin(altRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
        val clampedCosH = cosH.coerceIn(-1.0, 1.0)
        val hRad = acos(clampedCosH)
        return radToDeg(hRad) / 15.0
    }

    /**
     * Converts fractional hours to LocalTime aligned cleanly to the minute (:00 seconds)
     * so displayed times, countdowns, and alarm triggers match to the exact second.
     */
    private fun hoursToLocalTime(hours: Double): LocalTime {
        val fixed = fixHour(hours)
        val totalSeconds = (fixed * 3600.0).roundToInt() % 86400
        val totalMinutes = totalSeconds / 60
        val min = totalMinutes % 60
        val hr = (totalMinutes / 60) % 24
        return LocalTime.of(hr, min, 0)
    }

    fun calculateSchedule(
        date: LocalDate,
        lat: Double = BHURUNGAMARI_LAT,
        lng: Double = BHURUNGAMARI_LNG,
        tz: Double = resolveTimeZoneOffset(date, lat, lng),
        calculationMethod: CalculationMethod = CalculationMethod.KARACHI,
        asrJuristicMethod: AsrJuristicMethod = AsrJuristicMethod.HANAFI,
        minuteOffsets: PrayerMinuteOffsets = PrayerMinuteOffsets()
    ): PrayerSchedule {
        // First pass at 12:00 UTC
        val jdNoon = julianDate(date.year, date.monthValue, date.dayOfMonth, 12.0)
        val sunCoords = computeSunCoordinates(jdNoon)

        // Midday transit (Solar Noon)
        val solarTransit = 12.0 + tz - (lng / 15.0) - sunCoords.equationOfTime

        // Dhuhr: Standard practice in Bangladesh Islamic Foundation & Karachi method adds 4 minutes
        // safety buffer past solar transit so the strictly forbidden Zawal (zenith) period has passed.
        val dhuhrHours = solarTransit + (4.0 / 60.0)

        // Sunrise & Sunset (alpha = -0.8333)
        val hSun = computeHourAngle(SUNRISE_SUNSET_ANGLE, lat, sunCoords.declination)
        val sunriseHours = solarTransit - hSun
        // Maghrib / Sunset: adds 1 minute buffer ensuring complete dip beneath the horizon for Iftar
        val sunsetHours = solarTransit + hSun + (1.0 / 60.0)

        // Fajr (using selected CalculationMethod's Fajr angle)
        val hFajr = computeHourAngle(-calculationMethod.fajrAngle, lat, sunCoords.declination)
        val fajrHours = solarTransit - hFajr

        // Isha (using selected CalculationMethod's Isha angle or fixed 90m for Umm al-Qura)
        val hIsha = computeHourAngle(-calculationMethod.ishaAngle, lat, sunCoords.declination)
        val ishaHours = solarTransit + hIsha + (1.0 / 60.0)

        // Asr (using selected AsrJuristicMethod shadow factor: 2.0 for Hanafi, 1.0 for Standard/Shafi'i)
        val asrAlt = radToDeg(atan(1.0 / (asrJuristicMethod.shadowFactor + tan(degToRad(abs(lat - sunCoords.declination))))))
        val hAsr = computeHourAngle(asrAlt, lat, sunCoords.declination)
        val asrHours = solarTransit + hAsr + (1.0 / 60.0)

        val fajrTime = hoursToLocalTime(fajrHours).plusMinutes(minuteOffsets.fajr.toLong())
        val sunriseTime = hoursToLocalTime(sunriseHours).plusMinutes(minuteOffsets.sunrise.toLong())
        val dhuhrTime = hoursToLocalTime(dhuhrHours).plusMinutes(minuteOffsets.dhuhr.toLong())
        val asrTime = hoursToLocalTime(asrHours).plusMinutes(minuteOffsets.asr.toLong())
        val maghribTime = hoursToLocalTime(sunsetHours).plusMinutes(minuteOffsets.maghrib.toLong())
        val baseIshaTime = if (calculationMethod.ishaFixedMinutesAfterMaghrib != null) {
            hoursToLocalTime(sunsetHours).plusMinutes(calculationMethod.ishaFixedMinutesAfterMaghrib.toLong())
        } else {
            hoursToLocalTime(ishaHours)
        }
        val ishaTime = baseIshaTime.plusMinutes(minuteOffsets.isha.toLong())

        // True Islamic Midnight (Nisf al-Layl): exact midpoint between Maghrib (Sunset) and Fazr (Dawn)
        val maghribMinutes = maghribTime.hour * 60 + maghribTime.minute
        val fajrMinutes = fajrTime.hour * 60 + fajrTime.minute
        val nightDurationMinutes = ((24 * 60 - maghribMinutes) + fajrMinutes) % (24 * 60)
        val midnightTime = maghribTime.plusMinutes((nightDurationMinutes / 2).toLong())

        // Makrooh Tahrimi forbidden times:
        // 1. 15 minutes after sunrise
        val sunriseMakroohStart = sunriseTime
        val sunriseMakroohEnd = sunriseTime.plusMinutes(15)

        // 2. 10 minutes before Dhuhr
        val zawalMakroohStart = dhuhrTime.minusMinutes(10)
        val zawalMakroohEnd = dhuhrTime

        // 3. 15 minutes before Maghrib
        val sunsetMakroohStart = maghribTime.minusMinutes(15)
        val sunsetMakroohEnd = maghribTime

        // Permitted (non-Makrooh / Ikhtiyari) Waqt end times:
        // - Fazr ends 1 min before Sunrise
        val fajrEnd = sunriseTime.minusMinutes(1)
        // - Duhr ends 1 min before Asr
        val dhuhrEnd = asrTime.minusMinutes(1)
        // - Asr permitted (non-Makrooh) time ends 1 min before Pre-Sunset Makrooh starts (excludes the 15m forbidden sunset window!)
        val asrEnd = sunsetMakroohStart.minusMinutes(1)
        val asrMaxEnd = maghribTime.minusMinutes(1)
        // - Maghrib ends 1 min before Isha
        val maghribEnd = ishaTime.minusMinutes(1)
        // - Isha preferred (non-Makrooh) time ends at Islamic Midnight (Nisf al-Layl, ~11:16 PM); delaying past Midnight is Makrooh
        val ishaEnd = midnightTime
        val ishaMaxEnd = fajrTime.minusMinutes(1)

        // Ishraq begins right when sunrise Makrooh ends
        val ishraqTime = sunriseMakroohEnd

        return PrayerSchedule(
            date = date,
            fajr = fajrTime,
            sunrise = sunriseTime,
            ishraq = ishraqTime,
            dhuhr = dhuhrTime,
            asr = asrTime,
            maghrib = maghribTime,
            isha = ishaTime,
            midnight = midnightTime,
            fajrEnd = fajrEnd,
            dhuhrEnd = dhuhrEnd,
            asrEnd = asrEnd,
            maghribEnd = maghribEnd,
            ishaEnd = ishaEnd,
            asrMaxEnd = asrMaxEnd,
            ishaMaxEnd = ishaMaxEnd,
            sunriseMakroohStart = sunriseMakroohStart,
            sunriseMakroohEnd = sunriseMakroohEnd,
            zawalMakroohStart = zawalMakroohStart,
            zawalMakroohEnd = zawalMakroohEnd,
            sunsetMakroohStart = sunsetMakroohStart,
            sunsetMakroohEnd = sunsetMakroohEnd
        )
    }

    /**
     * Checks whether the given time falls in any of the 3 Makrooh Tahrimi forbidden windows.
     * Uses strict `isBefore` on end boundaries so that the exact start minute of Ishraq, Dhuhr, or Maghrib is permitted.
     */
    fun checkForbiddenStatus(now: LocalTime, schedule: PrayerSchedule): ForbiddenStatus {
        // Window 1: Sunrise to Sunrise + 15 min
        if (!now.isBefore(schedule.sunriseMakroohStart) && now.isBefore(schedule.sunriseMakroohEnd)) {
            val remaining = Duration.between(now, schedule.sunriseMakroohEnd).seconds.coerceAtLeast(0)
            return ForbiddenStatus(
                isForbidden = true,
                activeType = ForbiddenType.POST_SUNRISE,
                startTime = schedule.sunriseMakroohStart,
                endTime = schedule.sunriseMakroohEnd,
                remainingSeconds = remaining,
                nextType = ForbiddenType.ZAWAL,
                nextStartTime = schedule.zawalMakroohStart
            )
        }

        // Window 2: Dhuhr - 10 min to Dhuhr
        if (!now.isBefore(schedule.zawalMakroohStart) && now.isBefore(schedule.zawalMakroohEnd)) {
            val remaining = Duration.between(now, schedule.zawalMakroohEnd).seconds.coerceAtLeast(0)
            return ForbiddenStatus(
                isForbidden = true,
                activeType = ForbiddenType.ZAWAL,
                startTime = schedule.zawalMakroohStart,
                endTime = schedule.zawalMakroohEnd,
                remainingSeconds = remaining,
                nextType = ForbiddenType.PRE_MAGHRIB,
                nextStartTime = schedule.sunsetMakroohStart
            )
        }

        // Window 3: Maghrib - 15 min to Maghrib
        if (!now.isBefore(schedule.sunsetMakroohStart) && now.isBefore(schedule.sunsetMakroohEnd)) {
            val remaining = Duration.between(now, schedule.sunsetMakroohEnd).seconds.coerceAtLeast(0)
            return ForbiddenStatus(
                isForbidden = true,
                activeType = ForbiddenType.PRE_MAGHRIB,
                startTime = schedule.sunsetMakroohStart,
                endTime = schedule.sunsetMakroohEnd,
                remainingSeconds = remaining,
                nextType = null,
                nextStartTime = null
            )
        }

        // Not forbidden: find next upcoming forbidden window
        val nextType: ForbiddenType
        val nextStart: LocalTime
        if (now.isBefore(schedule.sunriseMakroohStart)) {
            nextType = ForbiddenType.POST_SUNRISE
            nextStart = schedule.sunriseMakroohStart
        } else if (now.isBefore(schedule.zawalMakroohStart)) {
            nextType = ForbiddenType.ZAWAL
            nextStart = schedule.zawalMakroohStart
        } else if (now.isBefore(schedule.sunsetMakroohStart)) {
            nextType = ForbiddenType.PRE_MAGHRIB
            nextStart = schedule.sunsetMakroohStart
        } else {
            nextType = ForbiddenType.POST_SUNRISE
            nextStart = schedule.sunriseMakroohStart
        }

        return ForbiddenStatus(
            isForbidden = false,
            activeType = null,
            startTime = null,
            endTime = null,
            remainingSeconds = 0,
            nextType = nextType,
            nextStartTime = nextStart
        )
    }

    /**
     * Determines which prayer is next and the countdown remaining
     */
    fun determineNextPrayer(
        now: LocalTime,
        todaySchedule: PrayerSchedule,
        tomorrowSchedule: PrayerSchedule
    ): NextPrayerInfo {
        val prayerList = listOf(
            PrayerType.FAJR to todaySchedule.fajr,
            PrayerType.SUNRISE to todaySchedule.sunrise,
            PrayerType.DHUHR to todaySchedule.dhuhr,
            PrayerType.ASR to todaySchedule.asr,
            PrayerType.MAGHRIB to todaySchedule.maghrib,
            PrayerType.ISHA to todaySchedule.isha
        )

        for ((type, time) in prayerList) {
            if (now.isBefore(time)) {
                val sec = Duration.between(now, time).seconds
                return NextPrayerInfo(
                    prayerType = type,
                    prayerTime = time,
                    remainingSeconds = sec,
                    isTomorrow = false
                )
            }
        }

        // If after Isha, next prayer is Tomorrow Fajr
        val secondsUntilMidnight = Duration.between(now, LocalTime.MAX).seconds + 1
        val secondsFromMidnightToFajr = Duration.between(LocalTime.MIN, tomorrowSchedule.fajr).seconds
        val totalSec = secondsUntilMidnight + secondsFromMidnightToFajr

        return NextPrayerInfo(
            prayerType = PrayerType.FAJR,
            prayerTime = tomorrowSchedule.fajr,
            remainingSeconds = totalSec,
            isTomorrow = true
        )
    }

    private fun secondsBetweenCrossingMidnight(start: LocalTime, end: LocalTime): Long {
        return if (!end.isBefore(start)) {
            Duration.between(start, end).seconds
        } else {
            (Duration.between(start, LocalTime.MAX).seconds + 1) + Duration.between(LocalTime.MIN, end).seconds
        }
    }

    private fun isInPreferredIshaWindow(now: LocalTime, schedule: PrayerSchedule): Boolean {
        return if (!schedule.midnight.isBefore(schedule.isha)) {
            !now.isBefore(schedule.isha) && !now.isAfter(schedule.midnight)
        } else {
            !now.isBefore(schedule.isha) || (!now.isAfter(schedule.midnight) && now.isBefore(schedule.fajr))
        }
    }

    /**
     * Determines which prayer card is currently in its active non-Makrooh window.
     * Note: During the 3 forbidden Makrooh Tahrimi windows (Sunrise, Zawal, Pre-Maghrib)
     * and after Islamic Midnight (when Isha becomes Makrooh), no card shows the green "NOW" badge.
     */
    fun determineCurrentActivePrayer(now: LocalTime, schedule: PrayerSchedule): PrayerType {
        return when {
            isInPreferredIshaWindow(now, schedule) -> PrayerType.ISHA
            now.isBefore(schedule.fajr) -> PrayerType.SUNRISE // Post-midnight Makrooh/Tahajjud window
            now.isBefore(schedule.sunrise) -> PrayerType.FAJR
            now.isBefore(schedule.dhuhr) -> PrayerType.SUNRISE
            now.isBefore(schedule.asr) -> PrayerType.DHUHR
            now.isBefore(schedule.sunsetMakroohStart) -> PrayerType.ASR // Excludes the 15-min pre-sunset forbidden window
            now.isBefore(schedule.maghrib) -> PrayerType.SUNRISE // Pre-sunset forbidden window
            now.isBefore(schedule.isha) -> PrayerType.MAGHRIB
            else -> PrayerType.SUNRISE // Post-midnight Makrooh window
        }
    }

    /**
     * Determines active waqt details including progress across the waqt and time remaining until waqt ends
     */
    fun determineActiveWaqt(
        now: LocalTime,
        todaySchedule: PrayerSchedule,
        tomorrowSchedule: PrayerSchedule,
        autoJumuahOnFriday: Boolean = true
    ): ActiveWaqtInfo {
        return when {
            !now.isBefore(todaySchedule.fajr) && now.isBefore(todaySchedule.sunrise) -> {
                val totalSec = Duration.between(todaySchedule.fajr, todaySchedule.sunrise).seconds.coerceAtLeast(1)
                val elapsedSec = Duration.between(todaySchedule.fajr, now).seconds.coerceAtLeast(0)
                val remain = Duration.between(now, todaySchedule.sunrise).seconds.coerceAtLeast(0)
                val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                ActiveWaqtInfo(
                    prayerType = PrayerType.FAJR,
                    displayName = "Fazr",
                    startTime = todaySchedule.fajr,
                    endTime = todaySchedule.fajrEnd,
                    remainingSeconds = remain,
                    progress = progress
                )
            }
            !now.isBefore(todaySchedule.sunrise) && now.isBefore(todaySchedule.dhuhr) -> {
                when {
                    now.isBefore(todaySchedule.sunriseMakroohEnd) -> {
                        val totalSec = Duration.between(todaySchedule.sunrise, todaySchedule.sunriseMakroohEnd).seconds.coerceAtLeast(1)
                        val elapsedSec = Duration.between(todaySchedule.sunrise, now).seconds.coerceAtLeast(0)
                        val remain = Duration.between(now, todaySchedule.sunriseMakroohEnd).seconds.coerceAtLeast(0)
                        val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                        ActiveWaqtInfo(
                            prayerType = PrayerType.SUNRISE,
                            displayName = "Sunrise (Makruh)",
                            startTime = todaySchedule.sunrise,
                            endTime = todaySchedule.sunriseMakroohEnd,
                            remainingSeconds = remain,
                            progress = progress
                        )
                    }
                    now.isBefore(todaySchedule.zawalMakroohStart) -> {
                        val totalSec = Duration.between(todaySchedule.ishraq, todaySchedule.zawalMakroohStart).seconds.coerceAtLeast(1)
                        val elapsedSec = Duration.between(todaySchedule.ishraq, now).seconds.coerceAtLeast(0)
                        val remain = Duration.between(now, todaySchedule.zawalMakroohStart).seconds.coerceAtLeast(0)
                        val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                        ActiveWaqtInfo(
                            prayerType = PrayerType.ISHRAQ,
                            displayName = "Ishraq / Duha",
                            startTime = todaySchedule.ishraq,
                            endTime = todaySchedule.zawalMakroohStart,
                            remainingSeconds = remain,
                            progress = progress
                        )
                    }
                    else -> {
                        val totalSec = Duration.between(todaySchedule.zawalMakroohStart, todaySchedule.dhuhr).seconds.coerceAtLeast(1)
                        val elapsedSec = Duration.between(todaySchedule.zawalMakroohStart, now).seconds.coerceAtLeast(0)
                        val remain = Duration.between(now, todaySchedule.dhuhr).seconds.coerceAtLeast(0)
                        val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                        ActiveWaqtInfo(
                            prayerType = PrayerType.SUNRISE,
                            displayName = "Zawal (Makruh)",
                            startTime = todaySchedule.zawalMakroohStart,
                            endTime = todaySchedule.dhuhr,
                            remainingSeconds = remain,
                            progress = progress
                        )
                    }
                }
            }
            !now.isBefore(todaySchedule.dhuhr) && now.isBefore(todaySchedule.asr) -> {
                val totalSec = Duration.between(todaySchedule.dhuhr, todaySchedule.asr).seconds.coerceAtLeast(1)
                val elapsedSec = Duration.between(todaySchedule.dhuhr, now).seconds.coerceAtLeast(0)
                val remain = Duration.between(now, todaySchedule.asr).seconds.coerceAtLeast(0)
                val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                val isFriday = autoJumuahOnFriday && todaySchedule.date.dayOfWeek == java.time.DayOfWeek.FRIDAY
                ActiveWaqtInfo(
                    prayerType = PrayerType.DHUHR,
                    displayName = if (isFriday) "Jumu'ah" else "Duhr",
                    startTime = todaySchedule.dhuhr,
                    endTime = todaySchedule.dhuhrEnd,
                    remainingSeconds = remain,
                    progress = progress
                )
            }
            !now.isBefore(todaySchedule.asr) && now.isBefore(todaySchedule.maghrib) -> {
                if (now.isBefore(todaySchedule.sunsetMakroohStart)) {
                    // Permitted Asr window (excludes the 15-min pre-sunset forbidden period)
                    val totalSec = Duration.between(todaySchedule.asr, todaySchedule.sunsetMakroohStart).seconds.coerceAtLeast(1)
                    val elapsedSec = Duration.between(todaySchedule.asr, now).seconds.coerceAtLeast(0)
                    val remain = Duration.between(now, todaySchedule.sunsetMakroohStart).seconds.coerceAtLeast(0)
                    val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                    ActiveWaqtInfo(
                        prayerType = PrayerType.ASR,
                        displayName = "Asr",
                        startTime = todaySchedule.asr,
                        endTime = todaySchedule.asrEnd,
                        remainingSeconds = remain,
                        progress = progress
                    )
                } else {
                    // 15-minute Pre-Sunset Forbidden (Makruh Tahrimi) window
                    val totalSec = Duration.between(todaySchedule.sunsetMakroohStart, todaySchedule.maghrib).seconds.coerceAtLeast(1)
                    val elapsedSec = Duration.between(todaySchedule.sunsetMakroohStart, now).seconds.coerceAtLeast(0)
                    val remain = Duration.between(now, todaySchedule.maghrib).seconds.coerceAtLeast(0)
                    val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                    ActiveWaqtInfo(
                        prayerType = PrayerType.SUNRISE,
                        displayName = "Sunset (Makruh)",
                        startTime = todaySchedule.sunsetMakroohStart,
                        endTime = todaySchedule.maghrib,
                        remainingSeconds = remain,
                        progress = progress
                    )
                }
            }
            !now.isBefore(todaySchedule.maghrib) && now.isBefore(todaySchedule.isha) -> {
                val totalSec = Duration.between(todaySchedule.maghrib, todaySchedule.isha).seconds.coerceAtLeast(1)
                val elapsedSec = Duration.between(todaySchedule.maghrib, now).seconds.coerceAtLeast(0)
                val remain = Duration.between(now, todaySchedule.isha).seconds.coerceAtLeast(0)
                val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                ActiveWaqtInfo(
                    prayerType = PrayerType.MAGHRIB,
                    displayName = "Maghrib",
                    startTime = todaySchedule.maghrib,
                    endTime = todaySchedule.maghribEnd,
                    remainingSeconds = remain,
                    progress = progress
                )
            }
            isInPreferredIshaWindow(now, todaySchedule) -> {
                // Preferred (Non-Makrooh) Isha window: from Isha start until Islamic Midnight (Nisf al-Layl)
                val totalSec = secondsBetweenCrossingMidnight(todaySchedule.isha, todaySchedule.midnight).coerceAtLeast(1)
                val elapsedSec = secondsBetweenCrossingMidnight(todaySchedule.isha, now).coerceAtLeast(0)
                val remain = secondsBetweenCrossingMidnight(now, todaySchedule.midnight).coerceAtLeast(0)
                val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
                ActiveWaqtInfo(
                    prayerType = PrayerType.ISHA,
                    displayName = "Isha",
                    startTime = todaySchedule.isha,
                    endTime = todaySchedule.ishaEnd,
                    remainingSeconds = remain,
                    progress = progress
                )
            }
            else -> {
                // Post-Midnight (Makruh for delaying Isha / Optimal for Tahajjud) until Fazr
                val targetFajr = if (!now.isBefore(todaySchedule.isha)) {
                    tomorrowSchedule.fajr
                } else {
                    todaySchedule.fajr
                }
                val endTimeDisplay = if (!now.isBefore(todaySchedule.isha)) {
                    tomorrowSchedule.fajr.minusMinutes(1)
                } else {
                    todaySchedule.ishaMaxEnd
                }
                val totalSec = secondsBetweenCrossingMidnight(todaySchedule.midnight, targetFajr).coerceAtLeast(1)
                val elapsedSec = secondsBetweenCrossingMidnight(todaySchedule.midnight, now).coerceAtLeast(0)
                val remainSec = secondsBetweenCrossingMidnight(now, targetFajr).coerceAtLeast(0)
                val progress = (elapsedSec.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)

                ActiveWaqtInfo(
                    prayerType = PrayerType.ISHA,
                    displayName = "Tahajjud / Late Isha (Makruh)",
                    startTime = todaySchedule.midnight,
                    endTime = endTimeDisplay,
                    remainingSeconds = remainSec,
                    progress = progress
                )
            }
        }
    }
}
