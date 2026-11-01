package com.example.prayer.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.prayer.model.AlarmToneMode
import com.example.prayer.model.PrayerSettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Simple, crash-safe controller for Prayer & Makruh notification chime and vibration.
 * Stops immediately when the user taps Dismiss or swipes the notification away,
 * and auto-silences after 20 seconds so it never gets stuck ringing.
 */
object AlarmSoundController {

    private var mediaPlayer: MediaPlayer? = null
    private var fallbackRingtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { silenceChimeOnly() }

    private val _isAlarmRinging = MutableStateFlow(false)
    val isAlarmRinging: StateFlow<Boolean> = _isAlarmRinging.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var onAlarmDismissedCallback: (() -> Unit)? = null

    fun registerActivityDismissListener(listener: (() -> Unit)?) {
        onAlarmDismissedCallback = listener
    }

    @Synchronized
    fun startAlarm(context: Context) {
        try {
            val appContext = context.applicationContext ?: context
            mainHandler.removeCallbacks(autoStopRunnable)
            val toneMode = PrayerSettingsRepository.getAlarmToneMode(appContext)
            if (toneMode == AlarmToneMode.SILENT_VISUAL) {
                _isAlarmRinging.value = false
                _isMuted.value = true
                stopSoundOnly()
                stopVibrationOnly()
                return
            }
            _isAlarmRinging.value = true
            _isMuted.value = false
            if (toneMode == AlarmToneMode.SOUND_AND_VIBRATION) {
                startSound(appContext)
            } else {
                stopSoundOnly()
            }
            startVibration(appContext)
            // Auto-stop sound & vibration after 20 seconds so notification remains simple & non-intrusive
            mainHandler.postDelayed(autoStopRunnable, 20_000L)
        } catch (_: Throwable) {
            // Never crash on audio/vibrator differences
        }
    }

    @Synchronized
    fun setMuted(context: Context, muted: Boolean) {
        try {
            val appContext = context.applicationContext ?: context
            _isMuted.value = muted
            if (muted) {
                stopSoundOnly()
                stopVibrationOnly()
            } else if (_isAlarmRinging.value) {
                val toneMode = PrayerSettingsRepository.getAlarmToneMode(appContext)
                if (toneMode == AlarmToneMode.SOUND_AND_VIBRATION) {
                    startSound(appContext)
                }
                if (toneMode != AlarmToneMode.SILENT_VISUAL) {
                    startVibration(appContext)
                }
            }
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    private fun silenceChimeOnly() {
        try {
            mainHandler.removeCallbacks(autoStopRunnable)
            _isAlarmRinging.value = false
            stopSoundOnly()
            stopVibrationOnly()
        } catch (_: Throwable) {
        }
    }

    @Synchronized
    fun stopAlarm() {
        try {
            mainHandler.removeCallbacks(autoStopRunnable)
            _isAlarmRinging.value = false
            _isMuted.value = false
            stopSoundOnly()
            stopVibrationOnly()
            onAlarmDismissedCallback?.invoke()
        } catch (_: Throwable) {
        }
    }

    private fun startSound(context: Context) {
        stopSoundOnly()
        try {
            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                ?: return

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = false
                prepare()
                start()
            }
        } catch (_: Throwable) {
            try {
                val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: return
                val ringtone = RingtoneManager.getRingtone(context, soundUri)
                ringtone?.play()
                fallbackRingtone = ringtone
            } catch (_: Throwable) {
                // Safe no-op in test or restricted audio environments
            }
        }
    }

    private fun stopSoundOnly() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Throwable) {
        } finally {
            mediaPlayer = null
        }

        try {
            fallbackRingtone?.stop()
        } catch (_: Throwable) {
        } finally {
            fallbackRingtone = null
        }
    }

    private fun startVibration(context: Context) {
        stopVibrationOnly()
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 350, 200, 350)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, -1)
            }
        } catch (_: Throwable) {
        }
    }

    private fun stopVibrationOnly() {
        try {
            vibrator?.cancel()
        } catch (_: Throwable) {
        } finally {
            vibrator = null
        }
    }
}
