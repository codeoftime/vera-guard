package com.vera.guard.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.telecom.TelecomManager
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

object Perm {
    fun has(ctx: Context, p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
}

object Channels {
    const val GUARD = "guard"; const val ALERT = "alert"; const val QUIET = "quiet"
    fun create(ctx: Context, guardName: String, alertName: String, quietName: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(GUARD, guardName, NotificationManager.IMPORTANCE_LOW).apply { setShowBadge(false) })
        nm.createNotificationChannel(NotificationChannel(ALERT, alertName, NotificationManager.IMPORTANCE_HIGH).apply { setSound(null, null); enableVibration(false); lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC })
        nm.createNotificationChannel(NotificationChannel(QUIET, quietName, NotificationManager.IMPORTANCE_MIN).apply { setShowBadge(false) })
    }
}

@Singleton
class LocationHelper @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(ctx) }

    fun hasPermission() = Perm.has(ctx, Manifest.permission.ACCESS_FINE_LOCATION) || Perm.has(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

    /** High-accuracy fix; falls back to last known (fused, then LocationManager). Never throws. */
    @SuppressLint("MissingPermission")
    suspend fun current(timeoutMs: Long = 8000): Location? {
        if (!hasPermission()) return null
        val fresh = withTimeoutOrNull(timeoutMs) {
            try { fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await() } catch (e: Exception) { null }
        }
        if (fresh != null) return fresh
        val last = try { fused.lastLocation.await() } catch (e: Exception) { null }
        return last ?: lastKnownFromManager()
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownFromManager(): Location? = try {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
    } catch (e: Exception) { null }

    fun isGpsEnabled(): Boolean = try {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (e: Exception) { false }

    @SuppressLint("MissingPermission")
    fun updates(intervalMs: Long, priority: Int = Priority.PRIORITY_BALANCED_POWER_ACCURACY): Flow<Location> = callbackFlow {
        val req = LocationRequest.Builder(priority, intervalMs).setMinUpdateIntervalMillis(intervalMs / 2).build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) { r.locations.forEach { trySend(it) } }
        }
        try { fused.requestLocationUpdates(req, cb, Looper.getMainLooper()) } catch (e: Exception) { close(e) }
        awaitClose { fused.removeLocationUpdates(cb) }
    }
}

@Singleton
class SmsSender @Inject constructor(@ApplicationContext private val ctx: Context) {
    fun send(to: String, text: String): Boolean {
        if (!Perm.has(ctx, Manifest.permission.SEND_SMS)) return false
        return try {
            @Suppress("DEPRECATION")
            val sm = if (Build.VERSION.SDK_INT >= 31) ctx.getSystemService(SmsManager::class.java) else SmsManager.getDefault()
            sm.sendMultipartTextMessage(to, null, sm.divideMessage(text), null, null)
            true
        } catch (e: Exception) { false }
    }
}

@Singleton
class CallHelper @Inject constructor(@ApplicationContext private val ctx: Context) {
    /** Direct call via Telecom (allowed from a foreground service with CALL_PHONE). Speaker stays off. */
    fun place(number: String): Boolean {
        if (!Perm.has(ctx, Manifest.permission.CALL_PHONE)) return false
        return try {
            ctx.getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", number, null), Bundle())
            true
        } catch (e: Exception) { false }
    }
    @Suppress("DEPRECATION")
    fun isIdle(): Boolean = try {
        if (!Perm.has(ctx, Manifest.permission.READ_PHONE_STATE)) true
        else ctx.getSystemService(TelephonyManager::class.java).callState == TelephonyManager.CALL_STATE_IDLE
    } catch (e: Exception) { true }

    /** Opens the dialer pre-filled (no permission needed). */
    fun dial(number: String) {
        ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

object Battery {
    fun percent(ctx: Context): Int? = try {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).takeIf { it in 0..100 }
    } catch (e: Exception) { null }
}

/** Alarm sound + vibration + spoken prompt for the loud fall alert. Owned by the service so it works with the UI closed. */
class AlertFeedback(private val ctx: Context) {
    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val handler = Handler(Looper.getMainLooper())
    private var speaking = false
    private var phrase = ""
    private var locale: Locale = Locale("en", "IN")
    private var previousVolume = -1

    fun start(spoken: String, loc: Locale) {
        phrase = spoken; locale = loc; speaking = true
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            previousVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(ctx, uri); isLooping = true; setVolume(1f, 1f); prepare(); start()
            }
        } catch (e: Exception) { /* alarm is best effort */ }
        vibrate(true)
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(locale)
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts?.setLanguage(Locale.ENGLISH)
                ttsReady = true; speakLoop()
            }
        }
    }

    private fun speakLoop() {
        if (!speaking || !ttsReady) return
        val attrs = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
        tts?.setAudioAttributes(attrs)
        tts?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, "vera-alert")
        handler.postDelayed({ speakLoop() }, 8000)
    }

    @Suppress("DEPRECATION")
    fun vibrate(on: Boolean) {
        val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (!on) { v.cancel(); return }
        v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 600, 300), 0))
    }

    fun stop() {
        speaking = false
        handler.removeCallbacksAndMessages(null)
        try { player?.stop(); player?.release() } catch (e: Exception) { }
        player = null
        try { tts?.stop(); tts?.shutdown() } catch (e: Exception) { }
        tts = null; ttsReady = false
        vibrate(false)
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (previousVolume >= 0) am.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
        } catch (e: Exception) { }
    }
}
