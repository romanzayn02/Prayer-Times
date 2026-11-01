package com.example.prayer.model

import java.time.LocalDate
import java.time.LocalTime

enum class PrayerType(
    val displayName: String,
    val description: String,
    val banglaName: String,
    val banglaDescription: String
) {
    FAJR("Fazr", "Dawn Prayer (Karachi 18°)", "ফজর", "ভোরের নামাজ (সুবহে সাদিক)"),
    SUNRISE("Sunrise", "Tulu-e-Aftab (Sunrise)", "সূর্যোদয়", "তুলুয়ে আফতাব (সূর্যোদয়)"),
    ISHRAQ("Ishraq", "15m post sunrise (Forbidden ends)", "ইশরাক", "সূর্যোদয়ের ১৫ মিনিট পর"),
    DHUHR("Duhr", "Noon Prayer", "যোহর", "দুপুরের নামাজ"),
    ASR("Asr", "Afternoon Prayer (Hanafi 2x shadow)", "আসর", "বিকেলের নামাজ (হানাফী ২ গুণ ছায়া)"),
    MAGHRIB("Maghrib", "Sunset Prayer (Iftar)", "মাগরিব", "সূর্যাস্তের নামাজ (ইফতার)"),
    ISHA("Isha", "Night Prayer (Karachi 18°)", "ইশা", "রাতের নামাজ");

    fun localizedName(isBangla: Boolean): String = if (isBangla) banglaName else displayName
    fun localizedDescription(isBangla: Boolean): String = if (isBangla) banglaDescription else description
}

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val isGps: Boolean = false
)

data class PrayerSchedule(
    val date: LocalDate,
    val fajr: LocalTime,
    val sunrise: LocalTime,
    val ishraq: LocalTime,
    val dhuhr: LocalTime,
    val asr: LocalTime,
    val maghrib: LocalTime,
    val isha: LocalTime,
    val midnight: LocalTime, // True Islamic Midnight (Nisf al-Layl: midpoint between Maghrib & Fazr)
    // Permitted (non-Makrooh / Ikhtiyari) Waqt end boundaries for display
    val fajrEnd: LocalTime,
    val dhuhrEnd: LocalTime,
    val asrEnd: LocalTime,       // Ends 1 min before Pre-Sunset Makrooh starts (excludes forbidden sunset time)
    val maghribEnd: LocalTime,
    val ishaEnd: LocalTime,      // Ends at Islamic Midnight (Nisf al-Layl); Makrooh to delay past this
    // Absolute final Waqt boundaries (including Makrooh necessity time)
    val asrMaxEnd: LocalTime = asrEnd,
    val ishaMaxEnd: LocalTime = ishaEnd,
    // Makrooh Tahrimi (Forbidden times)
    val sunriseMakroohStart: LocalTime, // Sunrise
    val sunriseMakroohEnd: LocalTime,   // Sunrise + 15 min
    val zawalMakroohStart: LocalTime,   // 10 min before Dhuhr
    val zawalMakroohEnd: LocalTime,     // Dhuhr
    val sunsetMakroohStart: LocalTime,  // 15 min before Maghrib
    val sunsetMakroohEnd: LocalTime     // Maghrib
)

enum class ForbiddenType(
    val title: String,
    val shortReason: String,
    val detailedRule: String,
    val banglaTitle: String,
    val banglaShortReason: String,
    val banglaDetailedRule: String
) {
    POST_SUNRISE(
        title = "Sunrise Forbidden Time (Tulu)",
        shortReason = "The sun is rising above the horizon",
        detailedRule = "Strictly forbidden (Makrooh Tahrimi) from sunrise until the sun has risen the height of a spear (~15 minutes after sunrise). After this window, Ishraq Salah begins.",
        banglaTitle = "সূর্যোদয়ের নিষিদ্ধ সময় (তুলু)",
        banglaShortReason = "সূর্য পূর্ব দিগন্তে উদিত হচ্ছে",
        banglaDetailedRule = "সূর্যোদয়ের শুরু থেকে প্রায় ১৫ মিনিট পর্যন্ত সকল প্রকার নামাজ ও সিজদা সম্পূর্ণ নিষিদ্ধ (মাকরূহ তাহরীমী)। এরপর ইশরাকের সময় শুরু হয়।"
    ),
    ZAWAL(
        title = "Midday Zenith Forbidden Time (Zawal)",
        shortReason = "The sun is at its absolute meridian zenith",
        detailedRule = "Strictly forbidden (Makrooh Tahrimi) during the zenith window (10 minutes before Duhr time starts until Duhr begins).",
        banglaTitle = "দ্বিপ্রহরের নিষিদ্ধ সময় (জাওয়াল)",
        banglaShortReason = "সূর্য ঠিক মাথার উপরে মধ্যগগনে অবস্থান করছে",
        banglaDetailedRule = "যোহরের ওয়াক্ত শুরু হওয়ার পূর্বের ১০ মিনিট (জাওয়ালের সময়) সকল নামাজ নিষিদ্ধ (মাকরূহ তাহরীমী)।"
    ),
    PRE_MAGHRIB(
        title = "Sunset Forbidden Time (Ghuroob)",
        shortReason = "The sun is setting / rays turning yellow",
        detailedRule = "Strictly forbidden (Makrooh Tahrimi) from 15 minutes before Maghrib until sunset (except that day's Asr if accidentally delayed, though intentional delay is disliked).",
        banglaTitle = "সূর্যাস্তের নিষিদ্ধ সময় (গুরুব)",
        banglaShortReason = "সূর্য অস্ত যাচ্ছে ও কিরণ হলুদ বর্ণ ধারণ করেছে",
        banglaDetailedRule = "মাগরিবের ১৫ মিনিট পূর্ব থেকে সূর্যাস্ত পর্যন্ত সকল নামাজ নিষিদ্ধ (তবে ওই দিনের আসরের ফরজ অনাকাঙ্ক্ষিতভাবে বাকি থাকলে আদায় করা যাবে)।"
    );

    fun localizedTitle(isBangla: Boolean): String = if (isBangla) banglaTitle else title
    fun localizedShortReason(isBangla: Boolean): String = if (isBangla) banglaShortReason else shortReason
    fun localizedDetailedRule(isBangla: Boolean): String = if (isBangla) banglaDetailedRule else detailedRule
}

data class ForbiddenStatus(
    val isForbidden: Boolean,
    val activeType: ForbiddenType? = null,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val remainingSeconds: Long = 0,
    val nextType: ForbiddenType? = null,
    val nextStartTime: LocalTime? = null
)

data class NextPrayerInfo(
    val prayerType: PrayerType,
    val prayerTime: LocalTime,
    val remainingSeconds: Long,
    val isTomorrow: Boolean = false
)

data class ActiveWaqtInfo(
    val prayerType: PrayerType,
    val displayName: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val remainingSeconds: Long,
    val progress: Float // 0f to 1f
)
