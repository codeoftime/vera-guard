package com.vera.guard.ui

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import com.vera.guard.data.SecureStore
import com.vera.guard.data.SettingsRepository
import com.vera.guard.escalation.AlertCoordinator
import com.vera.guard.escalation.AlertPhase
import com.vera.guard.escalation.AlertUiState
import com.vera.guard.escalation.EventType
import com.vera.guard.escalation.PinResult
import com.vera.guard.service.CallHelper
import com.vera.guard.service.Perm
import com.vera.guard.util.Locales
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Full-screen alert over the lock screen: fall countdown, journey check-in, and the post-escalation summary. */
@AndroidEntryPoint
class AlertActivity : ComponentActivity() {
    @Inject lateinit var coordinator: AlertCoordinator
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var calls: CallHelper
    private var recognizer: SpeechRecognizer? = null

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(Locales.wrap(newBase, Locales.code(SecureStore(newBase))))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true) }
        try { (getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null) } catch (e: Exception) { }
        setContent {
            VeraTheme(settings.current.theme) {
                val st by coordinator.state.collectAsStateWithLifecycle()
                LaunchedEffect(st.phase) {
                    if (st.phase == AlertPhase.NONE) finish()
                    if (st.phase == AlertPhase.COUNTDOWN) startVoice() else stopVoice()
                }
                AlertScreen(st, coordinator, settings.current.emergencyNumber, calls, settings.current.demoMode)
            }
        }
    }

    /** Best-effort spoken cancel: "I'm okay" / "मैं ठीक हूँ" / "ನಾನು ಚೆನ್ನಾಗಿದ್ದೇನೆ". */
    private fun startVoice() {
        if (recognizer != null || !Perm.has(this, Manifest.permission.RECORD_AUDIO) || !SpeechRecognizer.isRecognitionAvailable(this)) return
        try {
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { r ->
                r.setRecognitionListener(object : RecognitionListener {
                    private fun check(b: Bundle?) {
                        val l = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                        if (l.any { t -> val x = t.lowercase(); x.contains("okay") || x.contains("i'm ok") || x.contains("i am ok") || x.contains("ठीक") || x.contains("ಚೆನ್ನಾಗಿ") }) coordinator.cancelByUser("voice")
                    }
                    override fun onResults(b: Bundle?) { check(b); restart() }
                    override fun onPartialResults(b: Bundle?) { check(b) }
                    override fun onError(error: Int) { restart() }
                    override fun onReadyForSpeech(p: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(v: Float) = Unit
                    override fun onBufferReceived(b: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(t: Int, p: Bundle?) = Unit
                })
                listen(r)
            }
        } catch (e: Exception) { recognizer = null }
    }
    private fun listen(r: SpeechRecognizer) {
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, settings.locale().toLanguageTag()).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
    }
    private fun restart() { val r = recognizer ?: return; window.decorView.postDelayed({ try { if (recognizer != null) listen(r) } catch (e: Exception) { } }, 600) }
    private fun stopVoice() { try { recognizer?.destroy() } catch (e: Exception) { }; recognizer = null }

    override fun onDestroy() { stopVoice(); super.onDestroy() }
}

@Composable
fun AlertScreen(st: AlertUiState, coordinator: AlertCoordinator, emergencyNumber: String, calls: CallHelper, demo: Boolean) {
    val ctx = LocalContext.current
    val bg = if (st.silent && st.phase == AlertPhase.CHECKIN) Color(0xFF101418) else Color(0xFF3A0D10)
    Column(
        Modifier.fillMaxSize().background(bg).padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (st.fakeCancelled) { // duress PIN: looks cancelled while everything continues silently
            Text(stringResource(R.string.sos_cancelled), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            return@Column
        }
        if (demo) Text(stringResource(R.string.demo_banner), color = VeraAmber, fontWeight = FontWeight.Bold)
        when (st.phase) {
            AlertPhase.COUNTDOWN -> {
                Text(stringResource(if (st.type == EventType.CRASH) R.string.crash_detected else R.string.fall_detected), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(stringResource(R.string.are_you_okay), color = Color.White, fontSize = 22.sp)
                Text("${st.remaining}", color = Color.White, fontSize = 110.sp, fontWeight = FontWeight.Black)
                Text(stringResource(R.string.alerting_in, st.remaining), color = Color(0xFFFCA5A5))
                Metrics(st)
                Spacer(Modifier.height(20.dp))
                OkButton { coordinator.cancelByUser("tap") }
                PinBox(coordinator)
            }
            AlertPhase.CHECKIN -> {
                Text(stringResource(R.string.checkin_title), color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("${st.remaining}", color = Color.White, fontSize = 90.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                OkButton { coordinator.cancelByUser("tap") }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { coordinator.needHelp() }, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text(stringResource(R.string.i_need_help), color = Color.White, fontSize = 18.sp) }
                PinBox(coordinator)
            }
            AlertPhase.ESCALATING -> {
                Text(stringResource(R.string.alerting_contacts), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(st.steps.lastOrNull().orEmpty(), color = Color(0xFFFCA5A5))
            }
            AlertPhase.ESCALATED -> {
                Text(stringResource(R.string.help_alerted), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                if (st.acked) Text(stringResource(R.string.responder_on_way), color = VeraGreen, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Metrics(st)
                st.trackUrl?.let { Text(it, color = Color(0xFF8EC5FF), fontSize = 13.sp) }
                Spacer(Modifier.height(16.dp))
                val number = st.ambulanceNumber ?: emergencyNumber
                if (!demo && st.type != EventType.SOS) {
                    // Android usually refuses automatic emergency dialing from apps; keep a big manual CALL button.
                    LaunchedEffect(Unit) { dialAmbulance(ctx, number, calls, tryDirect = true) }
                    Button(onClick = { dialAmbulance(ctx, number, calls, tryDirect = true) }, modifier = Modifier.fillMaxWidth().height(72.dp), colors = ButtonDefaults.buttonColors(containerColor = VeraRed)) {
                        Text(stringResource(R.string.call_number, number), fontSize = 24.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                OkButton { coordinator.cancelByUser("tap") }
                PinBox(coordinator)
            }
            AlertPhase.CANCELLED -> Text(stringResource(R.string.cancelled_ok), color = VeraGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            AlertPhase.NONE -> Unit
        }
    }
}

private fun dialAmbulance(ctx: Context, number: String, calls: CallHelper, tryDirect: Boolean) {
    try {
        if (tryDirect && Perm.has(ctx, Manifest.permission.CALL_PHONE)) {
            ctx.startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else calls.dial(number)
    } catch (e: Exception) { try { calls.dial(number) } catch (e2: Exception) { } }
}

@Composable private fun OkButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().height(96.dp), colors = ButtonDefaults.buttonColors(containerColor = VeraGreen)) {
        Text(stringResource(R.string.im_ok), fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable private fun Metrics(st: AlertUiState) {
    val m = st.metrics
    if (st.severity != null && st.score != null) {
        Text("${st.severity} · ${st.score}/100", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
    if (m != null) {
        Text(
            stringResource(R.string.metrics_line, m.peakG, m.freeFallMs.toInt(), m.orientationDeg.toInt(), (m.stillnessMs / 1000f)),
            color = Color(0xFFFCA5A5), fontSize = 13.sp, textAlign = TextAlign.Center,
        )
    }
}

@Composable private fun PinBox(coordinator: AlertCoordinator) {
    if (!coordinator.hasAnyPin()) return
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    Spacer(Modifier.height(14.dp))
    OutlinedTextField(
        value = pin, onValueChange = { v -> pin = v.filter { it.isDigit() }.take(8); wrong = false },
        label = { Text(stringResource(R.string.pin_label)) }, singleLine = true, isError = wrong,
        supportingText = { if (wrong) Text(stringResource(R.string.pin_wrong)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(),
    )
    // Explicit confirm (no auto-submit while typing), so a short PIN can never fire as a prefix of a longer one.
    OutlinedButton(enabled = pin.length >= 4, onClick = { if (coordinator.submitPin(pin) == PinResult.WRONG) { wrong = true; pin = "" } }) {
        Text(stringResource(R.string.confirm), color = Color.White)
    }
}
