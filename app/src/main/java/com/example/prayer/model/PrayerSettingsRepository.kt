package com.example.prayer.model

import android.content.Context
import com.example.prayer.alarm.PrayerAlarmScheduler
import com.example.prayer.util.DateUtils

enum class AppLanguage(
    val code: String,
    val title: String,
    val nativeTitle: String,
    val subtitle: String
) {
    ENGLISH(
        code = "en",
        title = "English",
        nativeTitle = "English",
        subtitle = "Standard English interface & numerals"
    ),
    BANGLA(
        code = "bn",
        title = "Bangla",
        nativeTitle = "বাংলা",
        subtitle = "সম্পূর্ণ বাংলা ভাষা, তারিখ ও সংখ্যা"
    )
}

enum class CalculationMethod(
    val shortName: String,
    val fullTitle: String,
    val description: String,
    val banglaShortName: String,
    val banglaFullTitle: String,
    val banglaDescription: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaFixedMinutesAfterMaghrib: Int? = null
) {
    KARACHI(
        shortName = "Karachi",
        fullTitle = "University of Islamic Sciences, Karachi",
        description = "Fajr 18.0° • Isha 18.0° (Standard for Bangladesh, Pakistan, India)",
        banglaShortName = "করাচি",
        banglaFullTitle = "ইসলামিক সায়েন্সেস বিশ্ববিদ্যালয়, করাচি",
        banglaDescription = "ফজর ১৮.০° • ইশা ১৮.০° (বাংলাদেশ, ভারত ও পাকিস্তানের মানদণ্ড)",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    ISLAMIC_FOUNDATION_BD(
        shortName = "IFB (BD)",
        fullTitle = "Islamic Foundation Bangladesh",
        description = "Fajr 18.0° • Isha 18.0° (National Bangladesh Calibration)",
        banglaShortName = "ইফা (বাংলাদেশ)",
        banglaFullTitle = "ইসলামিক ফাউন্ডেশন বাংলাদেশ",
        banglaDescription = "ফজর ১৮.০° • ইশা ১৮.০° (বাংলাদেশ জাতীয় সময়সূচি মানদণ্ড)",
        fajrAngle = 18.0,
        ishaAngle = 18.0
    ),
    MWL(
        shortName = "MWL",
        fullTitle = "Muslim World League",
        description = "Fajr 18.0° • Isha 17.0° (Europe, Far East & Global)",
        banglaShortName = "এমডব্লিউএল",
        banglaFullTitle = "মুসলিম ওয়ার্ল্ড লিগ (MWL)",
        banglaDescription = "ফজর ১৮.০° • ইশা ১৭.০° (ইউরোপ, দূরপ্রাচ্য ও আন্তর্জাতিক)",
        fajrAngle = 18.0,
        ishaAngle = 17.0
    ),
    ISNA(
        shortName = "ISNA",
        fullTitle = "Islamic Society of North America",
        description = "Fajr 15.0° • Isha 15.0° (USA & Canada)",
        banglaShortName = "ইসনা (ISNA)",
        banglaFullTitle = "ইসলামিক সোসাইটি অব নর্থ আমেরিকা",
        banglaDescription = "ফজর ১৫.০° • ইশা ১৫.০° (যুক্তরাষ্ট্র ও কানাডা)",
        fajrAngle = 15.0,
        ishaAngle = 15.0
    ),
    UMM_AL_QURA(
        shortName = "Makkah",
        fullTitle = "Umm al-Qura University, Makkah",
        description = "Fajr 18.5° • Isha 90m after Maghrib (Arabian Peninsula)",
        banglaShortName = "মক্কা",
        banglaFullTitle = "উম্মুল কুরা বিশ্ববিদ্যালয়, মক্কা",
        banglaDescription = "ফজর ১৮.৫° • ইশা মাগরিবের ৯০ মিনিট পর (সৌদি আরব ও উপসাগরীয় অঞ্চল)",
        fajrAngle = 18.5,
        ishaAngle = 18.0,
        ishaFixedMinutesAfterMaghrib = 90
    ),
    EGYPTIAN(
        shortName = "Egyptian",
        fullTitle = "Egyptian General Authority of Survey",
        description = "Fajr 19.5° • Isha 17.5° (Africa & Middle East)",
        banglaShortName = "মিশরীয়",
        banglaFullTitle = "ইজিপশিয়ান জেনারেল অথরিটি অব সার্ভে",
        banglaDescription = "ফজর ১৯.৫° • ইশা ১৭.৫° (আফ্রিকা ও মধ্যপ্রাচ্য)",
        fajrAngle = 19.5,
        ishaAngle = 17.5
    );

    fun localizedShortName(isBangla: Boolean): String = if (isBangla) banglaShortName else shortName
    fun localizedFullTitle(isBangla: Boolean): String = if (isBangla) banglaFullTitle else fullTitle
    fun localizedDescription(isBangla: Boolean): String = if (isBangla) banglaDescription else description
}

enum class AsrJuristicMethod(
    val shortName: String,
    val fullTitle: String,
    val description: String,
    val banglaShortName: String,
    val banglaFullTitle: String,
    val banglaDescription: String,
    val shadowFactor: Double
) {
    HANAFI(
        shortName = "Hanafi",
        fullTitle = "Hanafi Fiqh (2x Shadow)",
        description = "Asr begins when shadow equals twice the object length plus noon shadow",
        banglaShortName = "হানাফী",
        banglaFullTitle = "হানাফী মাযহাব (২ গুণ ছায়া)",
        banglaDescription = "বস্তুর মূল ছায়ার সাথে দ্বিগুণ ছায়া হলে আসরের ওয়াক্ত শুরু হয়",
        shadowFactor = 2.0
    ),
    SHAFII(
        shortName = "Standard (1x)",
        fullTitle = "Standard / Shafi'i, Maliki, Hanbali (1x Shadow)",
        description = "Asr begins when shadow equals the object length plus noon shadow",
        banglaShortName = "শাফেয়ী / স্ট্যান্ডার্ড (১ গুণ)",
        banglaFullTitle = "শাফেয়ী, মালেকী ও হাম্বলী (১ গুণ ছায়া)",
        banglaDescription = "বস্তুর মূল ছায়ার সাথে সমপরিমাণ ছায়া হলে আসরের ওয়াক্ত শুরু হয়",
        shadowFactor = 1.0
    );

    fun localizedShortName(isBangla: Boolean): String = if (isBangla) banglaShortName else shortName
    fun localizedDescription(isBangla: Boolean): String = if (isBangla) banglaDescription else description
}

enum class AlarmToneMode(
    val title: String,
    val subtitle: String,
    val banglaTitle: String,
    val banglaSubtitle: String
) {
    SOUND_AND_VIBRATION(
        title = "Chime + Vibration",
        subtitle = "Plays gentle notification chime & vibration (auto-silences after 20s)",
        banglaTitle = "ধ্বনি ও ভাইব্রেশন",
        banglaSubtitle = "মৃদু এলার্ম ধ্বনি ও ভাইব্রেশন বাজবে (২০ সেকেন্ড পর স্বয়ংক্রিয়ভাবে থামবে)"
    ),
    VIBRATION_ONLY(
        title = "Vibration Only",
        subtitle = "Muted audio chime; vibrates gently when Waqt starts",
        banglaTitle = "শুধুমাত্র ভাইব্রেশন",
        banglaSubtitle = "শব্দ বন্ধ থাকবে; ওয়াক্ত শুরু হলে শুধু ভাইব্রেট করবে"
    ),
    SILENT_VISUAL(
        title = "Silent Visual Alert",
        subtitle = "Wakes lock screen & shows banner with zero sound or vibration",
        banglaTitle = "নীরব ভিজ্যুয়াল এলার্ট",
        banglaSubtitle = "কোনো শব্দ বা ভাইব্রেশন ছাড়াই স্ক্রিন জাগাবে ও নোটিফিকেশন দেখাবে"
    );

    fun localizedTitle(isBangla: Boolean): String = if (isBangla) banglaTitle else title
    fun localizedSubtitle(isBangla: Boolean): String = if (isBangla) banglaSubtitle else subtitle
}

enum class AppThemeMode(
    val title: String,
    val subtitle: String,
    val banglaTitle: String,
    val banglaSubtitle: String
) {
    DARK(
        title = "Dark Night",
        subtitle = "Deep celestial emerald & midnight glass",
        banglaTitle = "ডার্ক নাইট",
        banglaSubtitle = "গাঢ় পান্না ও রাতের গ্লাস থিম"
    ),
    LIGHT(
        title = "Light Daylight",
        subtitle = "High-contrast warm alabaster & forest emerald",
        banglaTitle = "লাইট ডে-লাইট",
        banglaSubtitle = "উজ্জ্বল দিনের আলো ও স্পষ্ট পান্না থিম"
    ),
    SYSTEM(
        title = "System Auto",
        subtitle = "Follows your Android device dark/light setting",
        banglaTitle = "সিস্টেম অটো",
        banglaSubtitle = "ফোনের ডার্ক/লাইট সেটিং অনুসরণ করবে"
    );

    fun localizedTitle(isBangla: Boolean): String = if (isBangla) banglaTitle else title
}

data class ExpandedCardOptions(
    val enableExpansion: Boolean = false, // OFF by default per user requirement
    val showRakatBreakdown: Boolean = true,
    val showSolarMilestones: Boolean = true,
    val showMakruhBoundaryNote: Boolean = true,
    val showVoluntaryPrayers: Boolean = true,
    val showQuickOffsetAdjuster: Boolean = true
)

data class PrayerMinuteOffsets(
    val fajr: Int = 0,
    val sunrise: Int = 0,
    val dhuhr: Int = 0,
    val asr: Int = 0,
    val maghrib: Int = 0,
    val isha: Int = 0
) {
    fun hasAnyOffset(): Boolean =
        fajr != 0 || sunrise != 0 || dhuhr != 0 || asr != 0 || maghrib != 0 || isha != 0

    fun getOffsetFor(type: PrayerType): Int = when (type) {
        PrayerType.FAJR -> fajr
        PrayerType.SUNRISE -> sunrise
        PrayerType.ISHRAQ -> sunrise
        PrayerType.DHUHR -> dhuhr
        PrayerType.ASR -> asr
        PrayerType.MAGHRIB -> maghrib
        PrayerType.ISHA -> isha
    }
}

data class CityPreset(
    val name: String,
    val region: String,
    val latitude: Double,
    val longitude: Double,
    val banglaName: String = name,
    val banglaRegion: String = region
) {
    fun localizedName(isBangla: Boolean): String = if (isBangla) banglaName else name
    fun localizedRegion(isBangla: Boolean): String = if (isBangla) banglaRegion else region
}

object PrayerSettingsRepository {
    private const val PREFS_NAME = "prayer_commercial_settings_prefs"

    private const val KEY_CALC_METHOD = "calc_method"
    private const val KEY_ASR_JURISTIC = "asr_juristic"
    private const val KEY_ALARM_TONE_MODE = "alarm_tone_mode"
    private const val KEY_APP_THEME_MODE = "app_theme_mode"
    private const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_USE_24_HOUR = "use_24_hour_format"
    private const val KEY_AUTO_JUMUAH = "auto_jumuah_friday"

    // Expandable Prayer Card Settings (Disabled by default)
    private const val KEY_EXPAND_CARDS_ENABLED = "expand_prayer_cards_enabled"
    private const val KEY_EXPAND_SHOW_RAKATS = "expand_show_rakats"
    private const val KEY_EXPAND_SHOW_MILESTONES = "expand_show_milestones"
    private const val KEY_EXPAND_SHOW_MAKRUH = "expand_show_makruh"
    private const val KEY_EXPAND_SHOW_VOLUNTARY = "expand_show_voluntary"
    private const val KEY_EXPAND_SHOW_OFFSET_ADJUSTER = "expand_show_offset_adjuster"

    // Per-date completed prayer tracking prefix
    private const val KEY_PREFIX_COMPLETED = "completed_salat_"

    private const val KEY_OFFSET_FAJR = "offset_min_fajr"
    private const val KEY_OFFSET_SUNRISE = "offset_min_sunrise"
    private const val KEY_OFFSET_DHUHR = "offset_min_dhuhr"
    private const val KEY_OFFSET_ASR = "offset_min_asr"
    private const val KEY_OFFSET_MAGHRIB = "offset_min_maghrib"
    private const val KEY_OFFSET_ISHA = "offset_min_isha"

    val fiveDailyPrayers: List<PrayerType> = listOf(
        PrayerType.FAJR,
        PrayerType.DHUHR,
        PrayerType.ASR,
        PrayerType.MAGHRIB,
        PrayerType.ISHA
    )

    val presetCities: List<CityPreset> = listOf(
        CityPreset("Bhurungamari, BD", "Kurigram, Bangladesh (UTC+6)", 26.1250, 89.6833, "ভূরুঙ্গামারী, বাংলাদেশ", "কুড়িগ্রাম, বাংলাদেশ (UTC+6)"),
        CityPreset("Dhaka, BD", "Capital, Bangladesh (UTC+6)", 23.8103, 90.4125, "ঢাকা, বাংলাদেশ", "রাজধানী, বাংলাদেশ (UTC+6)"),
        CityPreset("Chittagong, BD", "Port City, Bangladesh (UTC+6)", 22.3569, 91.7832, "চট্টগ্রাম, বাংলাদেশ", "বন্দর নগরী, বাংলাদেশ (UTC+6)"),
        CityPreset("Sylhet, BD", "North-East, Bangladesh (UTC+6)", 24.8949, 91.8687, "সিলেট, বাংলাদেশ", "উত্তর-পূর্বাঞ্চল, বাংলাদেশ (UTC+6)"),
        CityPreset("Rajshahi, BD", "North-West, Bangladesh (UTC+6)", 24.3745, 88.6042, "রাজশাহী, বাংলাদেশ", "উত্তর-পশ্চিমাঞ্চল, বাংলাদেশ (UTC+6)"),
        CityPreset("Rangpur, BD", "Northern Division, Bangladesh (UTC+6)", 25.7439, 89.2752, "রংপুর, বাংলাদেশ", "উত্তরাঞ্চল বিভাগ, বাংলাদেশ (UTC+6)"),
        CityPreset("Khulna, BD", "South-West, Bangladesh (UTC+6)", 22.8456, 89.5403, "খুলনা, বাংলাদেশ", "দক্ষিণ-পশ্চিমাঞ্চল, বাংলাদেশ (UTC+6)"),
        CityPreset("Barishal, BD", "Southern Division, Bangladesh (UTC+6)", 22.7010, 90.3535, "বরিশাল, বাংলাদেশ", "দক্ষিণাঞ্চল বিভাগ, বাংলাদেশ (UTC+6)"),
        CityPreset("Makkah, SA", "Masjid al-Haram, Saudi Arabia (UTC+3)", 21.4225, 39.8262, "মক্কা, সৌদি আরব", "মসজিদুল হারাম, সৌদি আরব (UTC+3)"),
        CityPreset("Madinah, SA", "Masjid an-Nabawi, Saudi Arabia (UTC+3)", 24.4672, 39.6112, "মদিনা, সৌদি আরব", "মসজিদে নববী, সৌদি আরব (UTC+3)"),
        CityPreset("London, UK", "United Kingdom (GMT/BST)", 51.5074, -0.1278, "লন্ডন, যুক্তরাজ্য", "যুক্তরাজ্য (GMT/BST)"),
        CityPreset("New York, US", "United States (EST/EDT)", 40.7128, -74.0060, "নিউ ইয়র্ক, যুক্তরাষ্ট্র", "যুক্তরাষ্ট্র (EST/EDT)"),
        CityPreset("Dubai, AE", "United Arab Emirates (UTC+4)", 25.2048, 55.2708, "দুবাই, আরব আমিরাত", "সংযুক্ত আরব আমিরাত (UTC+4)"),
        CityPreset("Kuala Lumpur, MY", "Malaysia (UTC+8)", 3.1390, 101.6869, "কুয়ালালামপুর, মালয়েশিয়া", "মালয়েশিয়া (UTC+8)"),
        CityPreset("Istanbul, TR", "Turkey (UTC+3)", 41.0082, 28.9784, "ইস্তাম্বুল, তুরস্ক", "তুরস্ক (UTC+3)"),
        CityPreset("Toronto, CA", "Canada (EST/EDT)", 43.6532, -79.3832, "টরন্টো, কানাডা", "কানাডা (EST/EDT)")
    )

    fun getCalculationMethod(context: Context): CalculationMethod {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CALC_METHOD, CalculationMethod.KARACHI.name)
        return CalculationMethod.entries.firstOrNull { it.name == raw } ?: CalculationMethod.KARACHI
    }

    fun setCalculationMethod(context: Context, method: CalculationMethod) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CALC_METHOD, method.name)
            .apply()
    }

    fun getAsrJuristicMethod(context: Context): AsrJuristicMethod {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ASR_JURISTIC, AsrJuristicMethod.HANAFI.name)
        return AsrJuristicMethod.entries.firstOrNull { it.name == raw } ?: AsrJuristicMethod.HANAFI
    }

    fun setAsrJuristicMethod(context: Context, method: AsrJuristicMethod) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ASR_JURISTIC, method.name)
            .apply()
    }

    fun getAlarmToneMode(context: Context): AlarmToneMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ALARM_TONE_MODE, AlarmToneMode.SOUND_AND_VIBRATION.name)
        return AlarmToneMode.entries.firstOrNull { it.name == raw } ?: AlarmToneMode.SOUND_AND_VIBRATION
    }

    fun setAlarmToneMode(context: Context, mode: AlarmToneMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ALARM_TONE_MODE, mode.name)
            .apply()
    }

    fun getAppThemeMode(context: Context): AppThemeMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_THEME_MODE, AppThemeMode.DARK.name)
        return AppThemeMode.entries.firstOrNull { it.name == raw } ?: AppThemeMode.DARK
    }

    fun setAppThemeMode(context: Context, mode: AppThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_THEME_MODE, mode.name)
            .apply()
    }

    fun getAppLanguage(context: Context): AppLanguage {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_APP_LANGUAGE, AppLanguage.ENGLISH.name)
        return AppLanguage.entries.firstOrNull { it.name == raw } ?: AppLanguage.ENGLISH
    }

    fun setAppLanguage(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_APP_LANGUAGE, language.name)
            .apply()
    }

    fun is24HourFormat(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_USE_24_HOUR, false)
    }

    fun set24HourFormat(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_USE_24_HOUR, enabled)
            .apply()
    }

    fun isAutoJumuahEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_JUMUAH, true)
    }

    fun setAutoJumuahEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_JUMUAH, enabled)
            .apply()
    }

    fun getExpandedCardOptions(context: Context): ExpandedCardOptions {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ExpandedCardOptions(
            enableExpansion = prefs.getBoolean(KEY_EXPAND_CARDS_ENABLED, false),
            showRakatBreakdown = prefs.getBoolean(KEY_EXPAND_SHOW_RAKATS, true),
            showSolarMilestones = prefs.getBoolean(KEY_EXPAND_SHOW_MILESTONES, true),
            showMakruhBoundaryNote = prefs.getBoolean(KEY_EXPAND_SHOW_MAKRUH, true),
            showVoluntaryPrayers = prefs.getBoolean(KEY_EXPAND_SHOW_VOLUNTARY, true),
            showQuickOffsetAdjuster = prefs.getBoolean(KEY_EXPAND_SHOW_OFFSET_ADJUSTER, true)
        )
    }

    fun setPrayerCardExpansionEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EXPAND_CARDS_ENABLED, enabled)
            .apply()
    }

    fun setExpandedCardOptions(context: Context, options: ExpandedCardOptions) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_EXPAND_CARDS_ENABLED, options.enableExpansion)
            .putBoolean(KEY_EXPAND_SHOW_RAKATS, options.showRakatBreakdown)
            .putBoolean(KEY_EXPAND_SHOW_MILESTONES, options.showSolarMilestones)
            .putBoolean(KEY_EXPAND_SHOW_MAKRUH, options.showMakruhBoundaryNote)
            .putBoolean(KEY_EXPAND_SHOW_VOLUNTARY, options.showVoluntaryPrayers)
            .putBoolean(KEY_EXPAND_SHOW_OFFSET_ADJUSTER, options.showQuickOffsetAdjuster)
            .apply()
    }

    private fun completionKey(date: java.time.LocalDate, prayerType: PrayerType): String =
        "${KEY_PREFIX_COMPLETED}${date}_${prayerType.name}"

    fun isPrayerCompleted(context: Context, date: java.time.LocalDate, prayerType: PrayerType): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(completionKey(date, prayerType), false)
    }

    fun setPrayerCompleted(
        context: Context,
        date: java.time.LocalDate,
        prayerType: PrayerType,
        completed: Boolean
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(completionKey(date, prayerType), completed)
            .apply()
    }

    fun togglePrayerCompleted(context: Context, date: java.time.LocalDate, prayerType: PrayerType): Boolean {
        val next = !isPrayerCompleted(context, date, prayerType)
        setPrayerCompleted(context, date, prayerType, next)
        return next
    }

    fun getCompletedPrayersForDate(context: Context, date: java.time.LocalDate): Set<PrayerType> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return fiveDailyPrayers.filter { prayer ->
            prefs.getBoolean(completionKey(date, prayer), false)
        }.toSet()
    }

    fun clearCompletedPrayersForDate(context: Context, date: java.time.LocalDate) {
        val editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        fiveDailyPrayers.forEach { prayer ->
            editor.remove(completionKey(date, prayer))
        }
        editor.apply()
    }

    fun getMinuteOffsets(context: Context): PrayerMinuteOffsets {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return PrayerMinuteOffsets(
            fajr = prefs.getInt(KEY_OFFSET_FAJR, 0).coerceIn(-30, 30),
            sunrise = prefs.getInt(KEY_OFFSET_SUNRISE, 0).coerceIn(-30, 30),
            dhuhr = prefs.getInt(KEY_OFFSET_DHUHR, 0).coerceIn(-30, 30),
            asr = prefs.getInt(KEY_OFFSET_ASR, 0).coerceIn(-30, 30),
            maghrib = prefs.getInt(KEY_OFFSET_MAGHRIB, 0).coerceIn(-30, 30),
            isha = prefs.getInt(KEY_OFFSET_ISHA, 0).coerceIn(-30, 30)
        )
    }

    fun setPrayerMinuteOffset(context: Context, prayerType: PrayerType, minutes: Int) {
        val clamped = minutes.coerceIn(-30, 30)
        val key = when (prayerType) {
            PrayerType.FAJR -> KEY_OFFSET_FAJR
            PrayerType.SUNRISE, PrayerType.ISHRAQ -> KEY_OFFSET_SUNRISE
            PrayerType.DHUHR -> KEY_OFFSET_DHUHR
            PrayerType.ASR -> KEY_OFFSET_ASR
            PrayerType.MAGHRIB -> KEY_OFFSET_MAGHRIB
            PrayerType.ISHA -> KEY_OFFSET_ISHA
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(key, clamped)
            .apply()
    }

    fun resetAllMinuteOffsets(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_OFFSET_FAJR, 0)
            .putInt(KEY_OFFSET_SUNRISE, 0)
            .putInt(KEY_OFFSET_DHUHR, 0)
            .putInt(KEY_OFFSET_ASR, 0)
            .putInt(KEY_OFFSET_MAGHRIB, 0)
            .putInt(KEY_OFFSET_ISHA, 0)
            .apply()
    }

    fun resetAllSettingsToDefaults(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        DateUtils.setHijriOffset(context, 0)
        PrayerAlarmScheduler.setSalatAlarmsEnabled(context, true)
        PrayerAlarmScheduler.setForbiddenRemindersEnabled(context, true)
        fiveDailyPrayers.forEach {
            PrayerAlarmScheduler.setPrayerAlarmEnabled(context, it, true)
        }
    }
}
