package com.example.prayer.util

import android.content.Context
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit

object DateUtils {
    private const val PREFS_NAME = "prayer_date_prefs"
    private const val KEY_HIJRI_OFFSET = "hijri_offset_days"

    private val hijriMonthsEn = arrayOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    private val hijriMonthsBn = arrayOf(
        "মহররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
        "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শাবান",
        "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
    )

    private val bengaliMonthsEn = arrayOf(
        "Boishakh", "Jyoishtho", "Asharh", "Shrabon", "Bhadro", "Ashwin",
        "Kartik", "Ogrohayon", "Poush", "Magh", "Falgun", "Choitro"
    )

    private val bengaliMonthsBn = arrayOf(
        "বৈশাখ", "জ্যৈষ্ঠ", "আষাঢ়", "শ্রাবণ", "ভাদ্র", "আশ্বিন",
        "কার্তিক", "অগ্রহায়ণ", "পৌষ", "মাঘ", "ফাল্গুন", "চৈত্র"
    )

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(('০'.code + (ch.code - '0'.code)).toChar())
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun localizeDigits(input: String, isBangla: Boolean): String {
        return if (isBangla) toBengaliDigits(input) else input
    }

    fun localizeTime(input: String, isBangla: Boolean): String {
        if (!isBangla) return input
        val localizedPeriod = input
            .replace("AM", "এএম", ignoreCase = true)
            .replace("PM", "পিএম", ignoreCase = true)
        return toBengaliDigits(localizedPeriod)
    }

    fun localizePrayerName(name: String, isBangla: Boolean): String {
        if (!isBangla) return name
        return when (name.trim()) {
            "Fazr", "Fajr" -> "ফজর"
            "Sunrise" -> "সূর্যোদয়"
            "Ishraq" -> "ইশরাক"
            "Duhr", "Dhuhr" -> "যোহর"
            "Jumu'ah", "Jumuah" -> "জুমুআহ"
            "Asr" -> "আসর"
            "Maghrib" -> "মাগরিব"
            "Isha" -> "ইশা"
            "Tahajjud" -> "তাহাজ্জুদ"
            "Chasht / Duha", "Chasht", "Duha" -> "চাশত / দুহা"
            "Awwabin" -> "আওয়াবীন"
            "Salat" -> "সালাত"
            else -> name
        }
    }

    fun localizeLocationName(locationName: String, isBangla: Boolean): String {
        if (!isBangla) return locationName
        val clean = locationName.trim()
        return when {
            clean.startsWith("Bhurungamari", ignoreCase = true) -> "ভূরুঙ্গামারী, বাংলাদেশ"
            clean.startsWith("Dhaka", ignoreCase = true) -> "ঢাকা, বাংলাদেশ"
            clean.startsWith("Chittagong", ignoreCase = true) -> "চট্টগ্রাম, বাংলাদেশ"
            clean.startsWith("Sylhet", ignoreCase = true) -> "সিলেট, বাংলাদেশ"
            clean.startsWith("Rajshahi", ignoreCase = true) -> "রাজশাহী, বাংলাদেশ"
            clean.startsWith("Rangpur", ignoreCase = true) -> "রংপুর, বাংলাদেশ"
            clean.startsWith("Khulna", ignoreCase = true) -> "খুলনা, বাংলাদেশ"
            clean.startsWith("Barishal", ignoreCase = true) -> "বরিশাল, বাংলাদেশ"
            clean.startsWith("Kurigram", ignoreCase = true) -> "কুড়িগ্রাম, বাংলাদেশ"
            clean.startsWith("Makkah", ignoreCase = true) -> "মক্কা, সৌদি আরব"
            clean.startsWith("Madinah", ignoreCase = true) -> "মদিনা, সৌদি আরব"
            clean.startsWith("London", ignoreCase = true) -> "লন্ডন, যুক্তরাজ্য"
            clean.startsWith("New York", ignoreCase = true) -> "নিউ ইয়র্ক, যুক্তরাষ্ট্র"
            clean.startsWith("Dubai", ignoreCase = true) -> "দুবাই, আরব আমিরাত"
            clean.startsWith("Kuala Lumpur", ignoreCase = true) -> "কুয়ালালামপুর, মালয়েশিয়া"
            clean.startsWith("Istanbul", ignoreCase = true) -> "ইস্তাম্বুল, তুরস্ক"
            clean.startsWith("Toronto", ignoreCase = true) -> "টরন্টো, কানাডা"
            else -> toBengaliDigits(clean)
        }
    }

    fun formatGregorianHeaderDate(date: LocalDate, isBangla: Boolean): String {
        if (!isBangla) {
            val dayName = when (date.dayOfWeek) {
                DayOfWeek.MONDAY -> "Mon"
                DayOfWeek.TUESDAY -> "Tue"
                DayOfWeek.WEDNESDAY -> "Wed"
                DayOfWeek.THURSDAY -> "Thu"
                DayOfWeek.FRIDAY -> "Fri"
                DayOfWeek.SATURDAY -> "Sat"
                DayOfWeek.SUNDAY -> "Sun"
            }
            val monthName = when (date.month) {
                Month.JANUARY -> "Jan"
                Month.FEBRUARY -> "Feb"
                Month.MARCH -> "Mar"
                Month.APRIL -> "Apr"
                Month.MAY -> "May"
                Month.JUNE -> "Jun"
                Month.JULY -> "Jul"
                Month.AUGUST -> "Aug"
                Month.SEPTEMBER -> "Sep"
                Month.OCTOBER -> "Oct"
                Month.NOVEMBER -> "Nov"
                Month.DECEMBER -> "Dec"
            }
            return "$dayName, ${date.dayOfMonth} $monthName"
        }
        val dayNameBn = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "সোম"
            DayOfWeek.TUESDAY -> "মঙ্গল"
            DayOfWeek.WEDNESDAY -> "বুধ"
            DayOfWeek.THURSDAY -> "বৃহস্পতি"
            DayOfWeek.FRIDAY -> "শুক্র"
            DayOfWeek.SATURDAY -> "শনি"
            DayOfWeek.SUNDAY -> "রবি"
        }
        val monthNameBn = getBanglaGregorianMonth(date.month)
        return "$dayNameBn, ${toBengaliDigits(date.dayOfMonth.toString())} $monthNameBn"
    }

    fun formatFullGregorianDate(date: LocalDate, isBangla: Boolean): String {
        if (!isBangla) {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", java.util.Locale.ENGLISH)
            return date.format(formatter)
        }
        val dayNameBn = when (date.dayOfWeek) {
            DayOfWeek.MONDAY -> "সোমবার"
            DayOfWeek.TUESDAY -> "মঙ্গলবার"
            DayOfWeek.WEDNESDAY -> "বুধবার"
            DayOfWeek.THURSDAY -> "বৃহস্পতিবার"
            DayOfWeek.FRIDAY -> "শুক্রবার"
            DayOfWeek.SATURDAY -> "শনিবার"
            DayOfWeek.SUNDAY -> "রবিবার"
        }
        val monthNameBn = getBanglaGregorianMonth(date.month)
        return "$dayNameBn, ${toBengaliDigits(date.dayOfMonth.toString())} $monthNameBn ${toBengaliDigits(date.year.toString())}"
    }

    private fun getBanglaGregorianMonth(month: Month): String = when (month) {
        Month.JANUARY -> "জানুয়ারি"
        Month.FEBRUARY -> "ফেব্রুয়ারি"
        Month.MARCH -> "মার্চ"
        Month.APRIL -> "এপ্রিল"
        Month.MAY -> "মে"
        Month.JUNE -> "জুন"
        Month.JULY -> "জুলাই"
        Month.AUGUST -> "আগস্ট"
        Month.SEPTEMBER -> "সেপ্টেম্বর"
        Month.OCTOBER -> "অক্টোবর"
        Month.NOVEMBER -> "নভেম্বর"
        Month.DECEMBER -> "ডিসেম্বর"
    }

    fun getHijriOffset(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_HIJRI_OFFSET, 0) // Default 0; user can adjust -2..+2 for local moon sighting
    }

    fun setHijriOffset(context: Context, offset: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HIJRI_OFFSET, offset.coerceIn(-3, 3))
            .apply()
    }

    fun getHijriDate(date: LocalDate, offsetDays: Int = 0, isBangla: Boolean = false): String {
        return try {
            val adjustedDate = date.plusDays(offsetDays.toLong())
            val hijrahDate = HijrahDate.from(adjustedDate)
            val day = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val month = hijrahDate.get(ChronoField.MONTH_OF_YEAR) // 1-12
            val year = hijrahDate.get(ChronoField.YEAR)
            if (isBangla) {
                val monthName = hijriMonthsBn.getOrElse(month - 1) { "" }
                "${toBengaliDigits(day.toString())} $monthName ${toBengaliDigits(year.toString())} হিজরি"
            } else {
                val monthName = hijriMonthsEn.getOrElse(month - 1) { "" }
                "$day $monthName $year AH"
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Revised Bangladeshi Solar Calendar (Bangla Academy standard):
     * First 6 months (Boishakh - Ashwin): 31 days
     * Next 5 months (Kartik - Falgun): 30 days (Falgun has 31 days in Gregorian leap years)
     * Last month (Choitro): 30 days
     * Pohela Boishakh falls on April 14.
     */
    fun getBengaliDate(date: LocalDate, isBangla: Boolean = false): String {
        val year = date.year
        val isLeap = date.isLeapYear
        val boishakhStart = LocalDate.of(year, 4, 14)

        val banglaYear: Int
        val refStart: LocalDate
        val falgunLength: Int

        if (!date.isBefore(boishakhStart)) {
            banglaYear = year - 593
            refStart = boishakhStart
            val nextYearLeap = LocalDate.of(year + 1, 1, 1).isLeapYear
            falgunLength = if (nextYearLeap) 31 else 30
        } else {
            banglaYear = year - 594
            refStart = LocalDate.of(year - 1, 4, 14)
            falgunLength = if (isLeap) 31 else 30
        }

        val monthLengths = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, falgunLength, 30)

        var daysElapsed = ChronoUnit.DAYS.between(refStart, date).toInt()
        var monthIndex = 0
        while (monthIndex < 12 && daysElapsed >= monthLengths[monthIndex]) {
            daysElapsed -= monthLengths[monthIndex]
            monthIndex++
        }
        val banglaDay = daysElapsed + 1
        return if (isBangla) {
            val monthName = bengaliMonthsBn.getOrElse(monthIndex) { "" }
            "${toBengaliDigits(banglaDay.toString())} $monthName ${toBengaliDigits(banglaYear.toString())} বঙ্গাব্দ"
        } else {
            val monthName = bengaliMonthsEn.getOrElse(monthIndex) { "" }
            "$banglaDay $monthName $banglaYear BS"
        }
    }
}
