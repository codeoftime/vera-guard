package com.vera.guard.ui

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vera.guard.R
import com.vera.guard.data.SecureStore
import com.vera.guard.util.Locales
import kotlinx.coroutines.delay
import org.json.JSONObject

/** Realistic incoming-call screen (name + ringtone) to help the user leave an uncomfortable situation. */
class FakeCallActivity : ComponentActivity() {
    private var ringtone: Ringtone? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Locales.wrap(newBase, Locales.code(SecureStore(newBase))))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        val name = intent.getStringExtra("name")?.ifBlank { null } ?: "Mom"
        try {
            ringtone = RingtoneManager.getRingtone(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)).also { it.isLooping = true; it.play() }
            @Suppress("DEPRECATION") val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
        } catch (e: Exception) { }
        setContent {
            VeraTheme("dark") {
                var answered by remember { mutableStateOf(false) }
                var seconds by remember { mutableLongStateOf(0L) }
                LaunchedEffect(answered) { if (answered) while (true) { delay(1000); seconds++ } }
                Column(Modifier.fillMaxSize().background(Color(0xFF101418)).padding(32.dp), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 64.dp)) {
                        Text(name, color = Color.White, fontSize = 36.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(if (answered) "%02d:%02d".format(seconds / 60, seconds % 60) else stringResource(R.string.incoming_call), color = Color(0xFF9FB0C0), fontSize = 18.sp)
                    }
                    Row(Modifier.padding(bottom = 48.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
                        Button(onClick = { stopRing(); finishAndRemoveTask() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5484D)), modifier = Modifier.size(76.dp)) { Text("✕", fontSize = 24.sp) }
                        if (!answered) Button(onClick = { answered = true; stopRing() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2FB877)), modifier = Modifier.size(76.dp)) { Text("✆", fontSize = 24.sp) }
                    }
                }
            }
        }
    }

    private fun stopRing() {
        try { ringtone?.stop() } catch (e: Exception) { }
        @Suppress("DEPRECATION") try { (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).cancel() } catch (e: Exception) { }
    }
    override fun onDestroy() { stopRing(); super.onDestroy() }
}
