package com.example

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.prayer.alarm.AlarmSoundController
import com.example.prayer.alarm.PrayerAlarmActivity
import com.example.prayer.alarm.PrayerAlarmReceiver
import com.example.prayer.alarm.PrayerAlarmScheduler
import com.example.prayer.alarm.PrayerNotificationHelper
import com.example.prayer.calculator.PrayerCalculator
import com.example.prayer.location.LocationHelper
import com.example.prayer.model.AlarmToneMode
import com.example.prayer.model.AsrJuristicMethod
import com.example.prayer.model.CalculationMethod
import com.example.prayer.model.ForbiddenType
import com.example.prayer.model.PrayerMinuteOffsets
import com.example.prayer.model.PrayerSettingsRepository
import com.example.prayer.model.PrayerType
import com.example.prayer.model.UserLocation
import com.example.prayer.ui.PrayerScreen
import com.example.prayer.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Prayer Times", appName)
    }

    @Test
    fun `calculate prayer schedule and verify non-Makrooh boundaries`() {
        val date = LocalDate.of(2026, 9, 21)
        val schedule = PrayerCalculator.calculateSchedule(date)

        // Fajr is before sunrise
        assertTrue(schedule.fajr.isBefore(schedule.sunrise))
        // Sunrise is before Ishraq
        assertTrue(schedule.sunrise.isBefore(schedule.ishraq))
        // Ishraq is 15 minutes after sunrise
        assertEquals(schedule.sunrise.plusMinutes(15), schedule.ishraq)
        // Dhuhr is after sunrise
        assertTrue(schedule.dhuhr.isAfter(schedule.sunrise))
        // Asr (Hanafi) is after Dhuhr
        assertTrue(schedule.asr.isAfter(schedule.dhuhr))
        // Maghrib is after Asr
        assertTrue(schedule.maghrib.isAfter(schedule.asr))
        // Isha is after Maghrib
        assertTrue(schedule.isha.isAfter(schedule.maghrib))

        // Verify all prayer times are aligned to :00 seconds
        assertEquals(0, schedule.fajr.second)
        assertEquals(0, schedule.dhuhr.second)
        assertEquals(0, schedule.asr.second)
        assertEquals(0, schedule.maghrib.second)
        assertEquals(0, schedule.isha.second)
        assertEquals(0, schedule.midnight.second)

        // Verify forbidden windows definitions
        assertEquals(schedule.sunrise, schedule.sunriseMakroohStart)
        assertEquals(schedule.sunrise.plusMinutes(15), schedule.sunriseMakroohEnd)
        assertEquals(schedule.dhuhr.minusMinutes(10), schedule.zawalMakroohStart)
        assertEquals(schedule.dhuhr, schedule.zawalMakroohEnd)
        assertEquals(schedule.maghrib.minusMinutes(15), schedule.sunsetMakroohStart)
        assertEquals(schedule.maghrib, schedule.sunsetMakroohEnd)

        // Verify Asr permitted end time excludes the 15-min Pre-Sunset Forbidden window
        assertEquals(schedule.sunsetMakroohStart.minusMinutes(1), schedule.asrEnd)
        assertEquals(schedule.maghrib.minusMinutes(1), schedule.asrMaxEnd)

        // Verify Isha preferred end time is Islamic Midnight (Nisf al-Layl), not Fajr
        assertEquals(schedule.midnight, schedule.ishaEnd)
        assertEquals(schedule.fajr.minusMinutes(1), schedule.ishaMaxEnd)
    }

    @Test
    fun `verify calibrated times and Islamic Midnight for September 21 at Bhurungamari`() {
        val date = LocalDate.of(2026, 9, 21)
        val schedule = PrayerCalculator.calculateSchedule(date)

        // Fazr: 04:32 AM - 05:48 AM
        assertEquals(LocalTime.of(4, 32), schedule.fajr)
        assertEquals(LocalTime.of(5, 48), schedule.fajrEnd)

        // Sunrise: 05:49 AM
        assertEquals(LocalTime.of(5, 49), schedule.sunrise)

        // Duhr: 11:58 AM - 04:17 PM
        assertEquals(LocalTime.of(11, 58), schedule.dhuhr)
        assertEquals(LocalTime.of(16, 17), schedule.dhuhrEnd)

        // Asr Permitted (Ikhtiyari): 04:18 PM - 05:44 PM (excludes 05:45 - 06:00 PM Sunset Makrooh)
        assertEquals(LocalTime.of(16, 18), schedule.asr)
        assertEquals(LocalTime.of(17, 44), schedule.asrEnd)
        assertEquals(LocalTime.of(17, 59), schedule.asrMaxEnd)

        // Maghrib: 06:00 PM - 07:16 PM
        assertEquals(LocalTime.of(18, 0), schedule.maghrib)
        assertEquals(LocalTime.of(19, 16), schedule.maghribEnd)

        // Isha Preferred (Ikhtiyari): 07:17 PM - 11:16 PM (Islamic Midnight: midpoint between 18:00 and 04:32)
        assertEquals(LocalTime.of(19, 17), schedule.isha)
        assertEquals(LocalTime.of(23, 16), schedule.midnight)
        assertEquals(LocalTime.of(23, 16), schedule.ishaEnd)
        assertEquals(LocalTime.of(4, 31), schedule.ishaMaxEnd)
    }

    @Test
    fun `test CalculationMethod, AsrJuristicMethod, and Local Mosque Minute Offsets`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PrayerSettingsRepository.resetAllSettingsToDefaults(context)

        val date = LocalDate.of(2026, 9, 21)
        val defaultKarachiHanafi = PrayerCalculator.calculateSchedule(date)

        // 1. Standard / Shafi'i Asr (1x shadow) starts earlier than Hanafi Asr (2x shadow)
        val shafiiSchedule = PrayerCalculator.calculateSchedule(
            date = date,
            asrJuristicMethod = AsrJuristicMethod.SHAFII
        )
        assertTrue(shafiiSchedule.asr.isBefore(defaultKarachiHanafi.asr))

        // 2. ISNA (15° Fajr/Isha) has later Fajr and earlier Isha than Karachi (18° Fajr/Isha)
        val isnaSchedule = PrayerCalculator.calculateSchedule(
            date = date,
            calculationMethod = CalculationMethod.ISNA
        )
        assertTrue(isnaSchedule.fajr.isAfter(defaultKarachiHanafi.fajr))
        assertTrue(isnaSchedule.isha.isBefore(defaultKarachiHanafi.isha))

        // 3. Umm al-Qura sets Isha to 90 minutes after Maghrib
        val makkahSchedule = PrayerCalculator.calculateSchedule(
            date = date,
            calculationMethod = CalculationMethod.UMM_AL_QURA
        )
        assertEquals(makkahSchedule.maghrib.plusMinutes(90), makkahSchedule.isha)

        // 4. Per-prayer minute calibration offsets shift individual times accurately
        val offsets = PrayerMinuteOffsets(fajr = 5, dhuhr = -3, maghrib = 2)
        val calibratedSchedule = PrayerCalculator.calculateSchedule(
            date = date,
            minuteOffsets = offsets
        )
        assertEquals(defaultKarachiHanafi.fajr.plusMinutes(5), calibratedSchedule.fajr)
        assertEquals(defaultKarachiHanafi.dhuhr.minusMinutes(3), calibratedSchedule.dhuhr)
        assertEquals(defaultKarachiHanafi.maghrib.plusMinutes(2), calibratedSchedule.maghrib)

        // 5. Verify persistence in PrayerSettingsRepository
        PrayerSettingsRepository.setCalculationMethod(context, CalculationMethod.MWL)
        PrayerSettingsRepository.setAsrJuristicMethod(context, AsrJuristicMethod.SHAFII)
        PrayerSettingsRepository.setAlarmToneMode(context, AlarmToneMode.VIBRATION_ONLY)
        PrayerSettingsRepository.set24HourFormat(context, true)
        PrayerSettingsRepository.setPrayerMinuteOffset(context, PrayerType.FAJR, 4)

        assertEquals(CalculationMethod.MWL, PrayerSettingsRepository.getCalculationMethod(context))
        assertEquals(AsrJuristicMethod.SHAFII, PrayerSettingsRepository.getAsrJuristicMethod(context))
        assertEquals(AlarmToneMode.VIBRATION_ONLY, PrayerSettingsRepository.getAlarmToneMode(context))
        assertTrue(PrayerSettingsRepository.is24HourFormat(context))
        assertEquals(4, PrayerSettingsRepository.getMinuteOffsets(context).fajr)

        PrayerSettingsRepository.resetAllSettingsToDefaults(context)
        assertEquals(CalculationMethod.KARACHI, PrayerSettingsRepository.getCalculationMethod(context))
        assertEquals(AsrJuristicMethod.HANAFI, PrayerSettingsRepository.getAsrJuristicMethod(context))
        assertFalse(PrayerSettingsRepository.is24HourFormat(context))
        assertEquals(0, PrayerSettingsRepository.getMinuteOffsets(context).fajr)
    }

    @Test
    fun `test active waqt and active prayer exclude Sunset Makrooh and Post-Midnight Makrooh`() {
        val date = LocalDate.of(2026, 9, 21)
        val today = PrayerCalculator.calculateSchedule(date)
        val tomorrow = PrayerCalculator.calculateSchedule(date.plusDays(1))

        // 1. Inside permitted Asr (04:45 PM) -> ASR is active
        val asrPermitted = LocalTime.of(16, 45)
        assertEquals(PrayerType.ASR, PrayerCalculator.determineCurrentActivePrayer(asrPermitted, today))
        val waqtAsr = PrayerCalculator.determineActiveWaqt(asrPermitted, today, tomorrow)
        assertEquals("Asr", waqtAsr.displayName)
        assertEquals(LocalTime.of(17, 44), waqtAsr.endTime)

        // 2. Inside Pre-Sunset Makrooh (05:50 PM) -> ASR is NOT marked active "NOW", Waqt shows Sunset (Makruh)
        val preSunsetMakrooh = LocalTime.of(17, 50)
        assertNotEquals(PrayerType.ASR, PrayerCalculator.determineCurrentActivePrayer(preSunsetMakrooh, today))
        val waqtSunsetMakrooh = PrayerCalculator.determineActiveWaqt(preSunsetMakrooh, today, tomorrow)
        assertEquals("Sunset (Makruh)", waqtSunsetMakrooh.displayName)

        // 3. Inside preferred Isha before Islamic Midnight (09:00 PM) -> ISHA is active
        val ishaPreferred = LocalTime.of(21, 0)
        assertEquals(PrayerType.ISHA, PrayerCalculator.determineCurrentActivePrayer(ishaPreferred, today))
        val waqtIsha = PrayerCalculator.determineActiveWaqt(ishaPreferred, today, tomorrow)
        assertEquals("Isha", waqtIsha.displayName)
        assertEquals(LocalTime.of(23, 16), waqtIsha.endTime)

        // 4. After Islamic Midnight (11:30 PM) -> Isha is in Makrooh delay / Tahajjud window
        val postMidnight = LocalTime.of(23, 30)
        assertNotEquals(PrayerType.ISHA, PrayerCalculator.determineCurrentActivePrayer(postMidnight, today))
        val waqtLateIsha = PrayerCalculator.determineActiveWaqt(postMidnight, today, tomorrow)
        assertEquals("Tahajjud / Late Isha (Makruh)", waqtLateIsha.displayName)
        assertTrue(waqtLateIsha.remainingSeconds > 0)
    }

    @Test
    fun `test Islamic Midnight wrapping past 00_00 clock midnight in summer high-latitude or western longitudes`() {
        val date = LocalDate.of(2026, 6, 21)
        val baseSchedule = PrayerCalculator.calculateSchedule(date, lat = 31.5204, lng = 74.3587, tz = 5.0)
        val scheduleWithAfterMidnightWrap = baseSchedule.copy(
            isha = LocalTime.of(21, 0),
            midnight = LocalTime.of(0, 15),
            ishaEnd = LocalTime.of(0, 15),
            fajr = LocalTime.of(4, 15)
        )

        val tenThirtyPm = LocalTime.of(22, 30)
        assertEquals(
            PrayerType.ISHA,
            PrayerCalculator.determineCurrentActivePrayer(tenThirtyPm, scheduleWithAfterMidnightWrap)
        )
        val waqtAtTenThirty = PrayerCalculator.determineActiveWaqt(
            tenThirtyPm,
            scheduleWithAfterMidnightWrap,
            scheduleWithAfterMidnightWrap
        )
        assertEquals("Isha", waqtAtTenThirty.displayName)
        assertEquals(6300L, waqtAtTenThirty.remainingSeconds)

        val twelveOhFiveAm = LocalTime.of(0, 5)
        assertEquals(
            PrayerType.ISHA,
            PrayerCalculator.determineCurrentActivePrayer(twelveOhFiveAm, scheduleWithAfterMidnightWrap)
        )

        val twelveThirtyAm = LocalTime.of(0, 30)
        assertNotEquals(
            PrayerType.ISHA,
            PrayerCalculator.determineCurrentActivePrayer(twelveThirtyAm, scheduleWithAfterMidnightWrap)
        )
        val waqtPostMid = PrayerCalculator.determineActiveWaqt(
            twelveThirtyAm,
            scheduleWithAfterMidnightWrap,
            scheduleWithAfterMidnightWrap
        )
        assertEquals("Tahajjud / Late Isha (Makruh)", waqtPostMid.displayName)
        assertEquals(3 * 3600L + 45 * 60L, waqtPostMid.remainingSeconds)
    }

    @Test
    fun `test forbidden windows detection`() {
        val date = LocalDate.of(2026, 9, 21)
        val schedule = PrayerCalculator.calculateSchedule(date)

        val postSunriseTime = schedule.sunrise.plusMinutes(5)
        val status1 = PrayerCalculator.checkForbiddenStatus(postSunriseTime, schedule)
        assertTrue(status1.isForbidden)
        assertEquals(ForbiddenType.POST_SUNRISE, status1.activeType)

        val zawalTime = schedule.dhuhr.minusMinutes(5)
        val status2 = PrayerCalculator.checkForbiddenStatus(zawalTime, schedule)
        assertTrue(status2.isForbidden)
        assertEquals(ForbiddenType.ZAWAL, status2.activeType)

        val sunsetTime = schedule.maghrib.minusMinutes(5)
        val status3 = PrayerCalculator.checkForbiddenStatus(sunsetTime, schedule)
        assertTrue(status3.isForbidden)
        assertEquals(ForbiddenType.PRE_MAGHRIB, status3.activeType)

        assertFalse(PrayerCalculator.checkForbiddenStatus(schedule.dhuhr, schedule).isForbidden)
        assertFalse(PrayerCalculator.checkForbiddenStatus(schedule.maghrib, schedule).isForbidden)
    }

    @Test
    fun `verify location isGps persistence and Hijri offset adjustment`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val defaultLoc = UserLocation(26.1250, 89.6833, "Bhurungamari, BD", isGps = false)
        LocationHelper.saveLocation(context, defaultLoc)
        val loadedLoc = LocationHelper.getLastSavedLocation(context)
        assertTrue(loadedLoc != null)
        assertFalse(loadedLoc!!.isGps)

        val date = LocalDate.of(2026, 9, 21)
        DateUtils.setHijriOffset(context, -1)
        assertEquals(-1, DateUtils.getHijriOffset(context))
        val hijriZero = DateUtils.getHijriDate(date, 0)
        val hijriMinusOne = DateUtils.getHijriDate(date, -1)
        assertNotEquals(hijriZero, hijriMinusOne)
        DateUtils.setHijriOffset(context, 0)
    }

    @Test
    fun `test single notification guarantee and fullScreenIntent lockscreen wake-up`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PrayerSettingsRepository.resetAllSettingsToDefaults(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = shadowOf(notificationManager)

        PrayerNotificationHelper.createNotificationChannels(context)
        PrayerNotificationHelper.cancelSalatNotification(context)

        val notification = PrayerNotificationHelper.buildPrayerAlarmNotification(
            context = context,
            prayerName = "Fazr",
            startTimeStr = "04:32 AM",
            endTimeStr = "05:48 AM"
        )

        assertTrue(notification.actions.isNotEmpty())
        assertEquals("Dismiss", notification.actions[0].title.toString())
        assertTrue(notification.fullScreenIntent != null)

        PrayerAlarmScheduler.triggerTestSalatAlarm(context, "Fazr", forceFullScreenPreview = false)
        assertEquals(1, shadowNm.allNotifications.size)
        val salatBanner = PrayerNotificationHelper.activeBanner.value
        assertTrue(salatBanner != null)
        assertEquals("SALAT WAQT", salatBanner?.badgeText)

        PrayerAlarmScheduler.triggerTestForbiddenNotification(
            context,
            ForbiddenType.POST_SUNRISE,
            forceFullScreenPreview = false
        )
        assertEquals(1, shadowNm.allNotifications.size)
        val forbiddenBanner = PrayerNotificationHelper.activeBanner.value
        assertEquals("MAKROOH ALERT", forbiddenBanner?.badgeText)

        PrayerAlarmScheduler.triggerTestSalatAlarm(context, "Fazr", forceFullScreenPreview = true)
        assertEquals(0, shadowNm.allNotifications.size)

        val receiver = PrayerAlarmReceiver()
        receiver.onReceive(context, Intent(PrayerNotificationHelper.ACTION_DISMISS))
        assertTrue(PrayerNotificationHelper.activeBanner.value == null)
        assertFalse(AlarmSoundController.isAlarmRinging.value)
        assertEquals(0, shadowNm.allNotifications.size)
    }

    @Test
    fun `test PrayerAlarmActivity full-screen lockscreen UI, volume key mute, and 60s auto-close timeout`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PrayerSettingsRepository.resetAllSettingsToDefaults(context)
        AlarmSoundController.startAlarm(context)
        assertFalse(AlarmSoundController.isMuted.value)

        val intent = Intent(context, PrayerAlarmActivity::class.java).apply {
            putExtra(PrayerAlarmActivity.EXTRA_PRAYER_NAME, "Fazr")
            putExtra(PrayerAlarmActivity.EXTRA_START_TIME, "04:32 AM")
            putExtra(PrayerAlarmActivity.EXTRA_END_TIME, "05:48 AM")
            putExtra(PrayerAlarmActivity.EXTRA_IS_FORBIDDEN, false)
        }

        val controller = Robolectric.buildActivity(PrayerAlarmActivity::class.java, intent).setup()
        val activity = controller.get()
        assertTrue(activity != null)
        assertFalse(activity.isFinishing)

        val handled = activity.onKeyDown(
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN,
            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_VOLUME_DOWN)
        )
        assertTrue(handled)
        assertTrue(AlarmSoundController.isMuted.value)

        val updateIntent = Intent(context, PrayerAlarmActivity::class.java).apply {
            putExtra(PrayerAlarmActivity.EXTRA_PRAYER_NAME, "Maghrib")
            putExtra(PrayerAlarmActivity.EXTRA_START_TIME, "06:00 PM")
            putExtra(PrayerAlarmActivity.EXTRA_END_TIME, "07:16 PM")
            putExtra(PrayerAlarmActivity.EXTRA_IS_FORBIDDEN, false)
        }
        controller.newIntent(updateIntent)
        assertFalse(activity.isFinishing)

        org.robolectric.shadows.ShadowLooper.idleMainLooper(61, java.util.concurrent.TimeUnit.SECONDS)
        assertTrue(activity.isFinishing)

        controller.pause().stop().destroy()
    }

    @Test
    fun `test Friday Jumuah automatic mode and worldwide Sahri Iftar milestone countdown rollover`() {
        val fridayDate = LocalDate.of(2026, 9, 25)
        val fridaySchedule = PrayerCalculator.calculateSchedule(fridayDate)
        val saturdaySchedule = PrayerCalculator.calculateSchedule(fridayDate.plusDays(1))

        val duringJumuah = fridaySchedule.dhuhr.plusMinutes(30)
        val fridayWaqt = PrayerCalculator.determineActiveWaqt(duringJumuah, fridaySchedule, saturdaySchedule)
        assertEquals("Jumu'ah", fridayWaqt.displayName)

        val saturdayWaqt = PrayerCalculator.determineActiveWaqt(duringJumuah, saturdaySchedule, fridaySchedule)
        assertEquals("Duhr", saturdayWaqt.displayName)

        val beforeFajr = LocalTime.of(3, 32, 0)
        val secBefore = com.example.prayer.ui.components.computeSolarMilestoneCountdownSeconds(
            now = beforeFajr,
            todayTarget = LocalTime.of(4, 32, 0),
            tomorrowTarget = LocalTime.of(4, 33, 0)
        )
        assertEquals(3600L, secBefore)

        val afterFajr = LocalTime.of(5, 32, 0)
        val secAfter = com.example.prayer.ui.components.computeSolarMilestoneCountdownSeconds(
            now = afterFajr,
            todayTarget = LocalTime.of(4, 32, 0),
            tomorrowTarget = LocalTime.of(4, 33, 0)
        )
        assertEquals(23 * 3600L + 60L, secAfter)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `test PrayerScreen UI, distinct dialogs, and Dedicated Settings Page on standard phone`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PrayerSettingsRepository.resetAllSettingsToDefaults(context)
        LocationHelper.saveLocation(
            context,
            UserLocation(
                latitude = PrayerCalculator.BHURUNGAMARI_LAT,
                longitude = PrayerCalculator.BHURUNGAMARI_LNG,
                locationName = "Bhurungamari, BD",
                isGps = false
            )
        )

        composeTestRule.setContent {
            PrayerScreen()
        }
        composeTestRule.waitForIdle()

        // 1. Verify Hero Dashboard & 4-Column Quick Timings Strip
        composeTestRule.onNodeWithTag("solar_arc_gauge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("live_clock_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sahri Ends").assertIsDisplayed()
        composeTestRule.onNodeWithText("Sunrise").assertIsDisplayed()
        composeTestRule.onNodeWithText("Iftar").assertIsDisplayed()
        composeTestRule.onNodeWithText("Midnight").assertIsDisplayed()

        // Tap Sahri Ends and Iftar cells to toggle live countdowns while preserving time display
        composeTestRule.onNodeWithTag("quick_timing_sahri").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Sahri Ends").assertIsDisplayed()
        composeTestRule.onNodeWithTag("quick_timing_iftar").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Iftar").assertIsDisplayed()

        // 2. Verify all 5 Salat cards are rendered without scrolling
        composeTestRule.onNodeWithTag("prayer_row_fajr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_dhuhr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_asr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_maghrib").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_isha").assertIsDisplayed()

        // 3. Verify distinct tap targets:
        // (a) Tapping top calendar header opens CalendarDateDialog (NOT MakroohInfoDialog)
        composeTestRule.onNodeWithTag("top_calendar_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("calendar_date_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Done").performClick()
        composeTestRule.waitForIdle()

        // (b) Tapping Location Chip opens LocationPickerDialog
        composeTestRule.onNodeWithTag("location_chip").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("location_picker_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("city_preset_dhaka").performClick()
        composeTestRule.waitForIdle()

        // (c) Tapping Master Alarm Toggle pill directly toggles Wake-Up ON/OFF without opening duplicate window
        composeTestRule.onNodeWithTag("salat_master_alarm_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Wake-Up: OFF").assertIsDisplayed()
        composeTestRule.onNodeWithTag("salat_master_alarm_toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Wake-Up: ON").assertIsDisplayed()

        // (d) Tapping Settings Button navigates to the Dedicated Full-Screen Settings Page
        composeTestRule.onNodeWithTag("open_settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Settings & Calibration").assertIsDisplayed()

        // Filter Settings by category tabs and verify organized controls
        composeTestRule.onNodeWithTag("settings_tab_alarms").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("test_fullscreen_alarm_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("test_banner_notification_button").assertIsDisplayed()

        composeTestRule.onNodeWithTag("settings_tab_calibration").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("offset_plus_fajr").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("+1m").assertIsDisplayed()

        // Navigate back from Settings to Dashboard
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("solar_arc_gauge").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w360dp-h740dp-xxhdpi")
    fun `test all 5 Salat rows fit without scrolling on compact 360x740dp high-DPI display`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PrayerSettingsRepository.resetAllSettingsToDefaults(context)
        LocationHelper.saveLocation(
            context,
            UserLocation(
                latitude = PrayerCalculator.BHURUNGAMARI_LAT,
                longitude = PrayerCalculator.BHURUNGAMARI_LNG,
                locationName = "Bhurungamari, BD",
                isGps = false
            )
        )

        composeTestRule.setContent {
            PrayerScreen()
        }
        composeTestRule.waitForIdle()

        // Even on a compact 360x740dp screen, all 5 Salat rows must be visible before expansion
        composeTestRule.onNodeWithTag("prayer_row_fajr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_dhuhr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_asr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_maghrib").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_isha").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `test Prayer Completion Checkmarks, Expandable Cards Disabled by Default, and Light Theme`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PrayerSettingsRepository.resetAllSettingsToDefaults(context)
        LocationHelper.saveLocation(
            context,
            UserLocation(
                latitude = PrayerCalculator.BHURUNGAMARI_LAT,
                longitude = PrayerCalculator.BHURUNGAMARI_LNG,
                locationName = "Bhurungamari, BD",
                isGps = false
            )
        )

        // Verify expansion is disabled by default in repository
        assertFalse(PrayerSettingsRepository.getExpandedCardOptions(context).enableExpansion)

        composeTestRule.setContent {
            PrayerScreen()
        }
        composeTestRule.waitForIdle()

        // 1. Verify initial completion badge is "0/5 Done" and checkmarks are displayed
        composeTestRule.onNodeWithTag("salat_completion_badge").assertIsDisplayed()
        composeTestRule.onNodeWithText("0/5 Done").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_done_toggle_fajr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_done_toggle_dhuhr").assertIsDisplayed()

        // Mark Fazr and Duhr as Done via the checkmark button
        composeTestRule.onNodeWithTag("prayer_done_toggle_fajr").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("1/5 Done").assertIsDisplayed()

        composeTestRule.onNodeWithTag("prayer_done_toggle_dhuhr").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("2/5 Done").assertIsDisplayed()

        // 2. Verify tapping Fazr row does NOT expand it by default
        composeTestRule.onNodeWithTag("prayer_row_fajr").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("prayer_expanded_fajr").assertDoesNotExist()

        // 3. Toggle Light Theme from top header bar quick toggle button
        composeTestRule.onNodeWithTag("theme_toggle_button").performClick()
        composeTestRule.waitForIdle()
        assertEquals(com.example.prayer.model.AppThemeMode.LIGHT, PrayerSettingsRepository.getAppThemeMode(context))

        // 4. Open Settings -> Display & Calendar tab -> Enable Expandable Prayer Cards
        composeTestRule.onNodeWithTag("open_settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_tab_display").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_expand_cards_switch").performClick()
        composeTestRule.waitForIdle()
        assertTrue(PrayerSettingsRepository.getExpandedCardOptions(context).enableExpansion)

        // Navigate back and verify tapping Fazr card now expands it with proper contents
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("prayer_row_fajr").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("prayer_expanded_fajr").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
    fun `test full Bangla language support in Settings and across Dashboard`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PrayerSettingsRepository.resetAllSettingsToDefaults(context)
        LocationHelper.saveLocation(
            context,
            UserLocation(
                latitude = PrayerCalculator.BHURUNGAMARI_LAT,
                longitude = PrayerCalculator.BHURUNGAMARI_LNG,
                locationName = "Bhurungamari, BD",
                isGps = false
            )
        )

        // Default language is English
        assertEquals(com.example.prayer.model.AppLanguage.ENGLISH, PrayerSettingsRepository.getAppLanguage(context))

        composeTestRule.setContent {
            PrayerScreen()
        }
        composeTestRule.waitForIdle()

        // Open Settings -> Display & Calendar tab -> Select বাংলা (Bangla)
        composeTestRule.onNodeWithTag("open_settings_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_tab_display").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("language_mode_bangla").performClick()
        composeTestRule.waitForIdle()
        assertEquals(com.example.prayer.model.AppLanguage.BANGLA, PrayerSettingsRepository.getAppLanguage(context))
        composeTestRule.onNodeWithText("সেটিংস ও সময় সমন্বয়").assertIsDisplayed()

        // Navigate back to Dashboard and verify Bangla prayer names, numerals, and labels
        composeTestRule.onNodeWithTag("settings_back_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("সেহরির শেষ").assertIsDisplayed()
        composeTestRule.onNodeWithText("সূর্যোদয়").assertIsDisplayed()
        composeTestRule.onNodeWithText("ইফতার").assertIsDisplayed()
        composeTestRule.onNodeWithText("মধ্যরাত").assertIsDisplayed()
        composeTestRule.onNodeWithText("০/৫ সম্পন্ন").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_fajr").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_maghrib").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prayer_row_isha").assertIsDisplayed()
    }
}


