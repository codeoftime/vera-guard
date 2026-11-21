package com.vera.guard.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Loud personal alarm (women's safety toolkit). Plays the alarm tone at full alarm volume until stopped. */
@Singleton
class SirenController @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val _on = MutableStateFlow(false)
    val on: StateFlow<Boolean> = _on.asStateFlow()
    private var player: MediaPlayer? = null
    private var previousVolume = -1

    fun toggle() { if (_on.value) stop() else start() }

    fun start() {
        if (_on.value) return
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            previousVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            player = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(ctx, uri); isLooping = true; setVolume(1f, 1f); prepare(); start()
            }
            _on.value = true
        } catch (e: Exception) { stop() }
    }

    fun stop() {
        try { player?.stop(); player?.release() } catch (e: Exception) { }
        player = null
        try {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (previousVolume >= 0) am.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0)
        } catch (e: Exception) { }
        previousVolume = -1
        _on.value = false
    }
}
