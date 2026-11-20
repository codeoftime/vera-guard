package com.vera.guard.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.vera.guard.R
import com.vera.guard.data.ProfileRepository
import com.vera.guard.data.SecureStore
import com.vera.guard.data.SettingsRepository
import com.vera.guard.detect.DetectorResult
import com.vera.guard.detect.DetectorState
import com.vera.guard.detect.FallDetector
import com.vera.guard.detect.FallMetrics
import com.vera.guard.detect.Sample
import com.vera.guard.detect.Severity
import com.vera.guard.detect.SeverityScorer
import com.vera.guard.detect.ShakeDetector
import com.vera.guard.escalation.AlertCoordinator
import com.vera.guard.escalation.AlertPhase
import com.vera.guard.escalation.AlertUiState
import com.vera.guard.escalation.EscalationManager
import com.vera.guard.escalation.EscalationRequest
import com.vera.guard.escalation.EventType
import com.vera.guard.escalation.SmsBuilder
import com.vera.guard.journey.JourneyConfig
import com.vera.guard.journey.JourneyCoordinator
import com.vera.guard.journey.JourneyEvent
import com.vera.guard.journey.JourneyMonitor
import com.vera.guard.journey.JourneyRequest
import com.vera.guard.journey.JourneyUiState
import com.vera.guard.journey.LatLng
import com.vera.guard.net.ApiClient
import com.vera.guard.net.SessionHandle
import com.vera.guard.ui.AlertActivity
import com.vera.guard.util.Locales
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * The always-on foreground service: sensors, fall detection, shake SOS, countdown alert, silent SOS, journey monitoring.
 * START_STICKY so the system restarts it; a wake lock is held only while an event is being processed.
 */
@AndroidEntryPoint
class GuardService : Service(), SensorEventListener {
    @Inject lateinit var coordinator: AlertCoordinator
    @Inject lateinit var escalation: EscalationManager
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var profiles: ProfileRepository
    @Inject lateinit var store: SecureStore
    @Inject lateinit var api: ApiClient
    @Inject lateinit var journeys: JourneyCoordinator
    @Inject lateinit var loc: LocationHelper
    @Inject lateinit var sms: SmsSender

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var sensors: SensorManager
    private var detector = FallDetector()
    private val shake = ShakeDetector()
    private var gx = 0f; private var gy = 0f; private var gz = 0f
    private var feedback: AlertFeedback? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var countdownJob: Job? = null
    private var journeyJob: Job? = null
    private var recordJob: Job? = null
    private var recognizer: SpeechRecognizer? = null
    private var keywordOn = false
    private var lastEscalationAt = 0L
    private var sosActive = false
    private var currentSensitivity = 1f
    private var journeyHandle: SessionHandle? = null
    private var journeyMonitor: JourneyMonitor? = null
    private var journeyReq: JourneyRequest? = null
    private var simDead = false
    private var checkInJob: Job? = null

    private fun lctx(): Context = Locales.wrap(this, settings.current.language)
    private fun str(id: Int, vararg a: Any) = lctx().getString(id, *a)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val c = lctx()
        Channels.create(this, c.getString(R.string.channel_guard), c.getString(R.string.channel_alert), c.getString(R.string.channel_quiet))
        goForeground(guardNotification())
        sensors = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        registerSensors()
        scope.launch { settings.settings.collect { applySettings() } }
    }

    private fun registerSensors() {
        sensors.unregisterListener(this)
        sensors.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        sensors.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let { sensors.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
    }

    private fun applySettings() {
        val s = settings.current
        if (s.sensitivity != currentSensitivity) { currentSensitivity = s.sensitivity; detector = FallDetector(currentSensitivity) }
        if (s.keywordEnabled && !keywordOn && Perm.has(this, Manifest.permission.RECORD_AUDIO)) startKeyword()
        if (!s.keywordEnabled && keywordOn) stopKeyword()
        if (!sosActive) notify(ID_MAIN, guardNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Required: every startForegroundService() call must reach startForeground().
        if (!::sensors.isInitialized) return START_STICKY
        goForeground(if (sosActive) discreetNotification() else guardNotification())
        when (intent?.action) {
            Actions.SOS -> if (settings.current.let { it.widgetEnabled || it.tileEnabled || it.notificationSosEnabled || it.shakeEnabled }) triggerSos(simulate = false)
            Actions.SIM_SOS -> triggerSos(simulate = true)
            Actions.CANCEL_SOS -> cancelSos()
            Actions.SIM_FALL -> simulateFall(intent.getIntExtra(Actions.EXTRA_SCORE, 60))
            Actions.JOURNEY_START -> startJourney()
            Actions.JOURNEY_END -> endJourney(arrived = false)
            Actions.JOURNEY_EXTEND -> extendJourney(15)
            Actions.SIM_DEVIATION -> onJourneyEvent(JourneyEvent.DEVIATED)
            Actions.SIM_DEAD -> simulateDead()
            Actions.SETTINGS -> applySettings()
            Actions.STOP -> { stopSelf() }
        }
        return START_STICKY
    }

    // ------------------------------------------------------------------ sensors
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(e: SensorEvent) {
        if (e.sensor.type == Sensor.TYPE_GYROSCOPE) { gx = e.values[0]; gy = e.values[1]; gz = e.values[2]; return }
        if (e.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val s = Sample(e.timestamp / 1_000_000L, e.values[0], e.values[1], e.values[2], gx, gy, gz)
        val cfg = settings.current
        if (cfg.shakeEnabled && !sosActive && shake.onSample(s)) { triggerSos(simulate = false); return }
        if (!cfg.fallEnabled) return
        if (detector.state == DetectorState.ALERTING) return
        when (detector.onSample(s)) {
            DetectorResult.DETECTED -> detector.currentMetrics()?.let { onFallDetected(it) }
            DetectorResult.CANCELLED_BY_MOVEMENT -> cancelFallAlert("walking")
            DetectorResult.NONE -> Unit
        }
    }

    // ------------------------------------------------------------------ fall alert
    private fun onFallDetected(m: FallMetrics) {
        if (coordinator.state.value.phase != AlertPhase.NONE || System.currentTimeMillis() - lastEscalationAt < 30_000) { detector.reset(); return }
        val sev = SeverityScorer.score(m)
        beginCountdown(m, sev.level, sev.score)
    }

    private fun simulateFall(score: Int) {
        val sc = score.coerceIn(0, 100)
        val level = SeverityScorer.levelOf(sc)
        // synthetic metrics tuned so the score lands near the slider value (demo only)
        val k = sc / 100f
        val m = FallMetrics(2f + 7f * k, (40 + 160 * k).toLong(), 300f * k, 10f * k, (100 + 500 * k).toLong(), (3000 + 17000 * k).toLong(), 90f * k, true)
        beginCountdown(m, level, sc)
    }

    private fun beginCountdown(m: FallMetrics, level: Severity, score: Int) {
        val cfg = settings.current
        val total = when (level) { Severity.LOW -> cfg.countdownLow; Severity.MEDIUM -> cfg.countdownMedium; Severity.HIGH -> cfg.countdownHigh }.coerceIn(5, 120)
        val type = if (m.hadFreeFall) EventType.FALL else EventType.CRASH
        detector.markAlerting()
        acquireWakeLock(5 * 60_000L)
        coordinator.set(AlertUiState(AlertPhase.COUNTDOWN, type, level.name, score, m, total, total))
        coordinator.onCancel = { src -> cancelFallAlert(src) }
        coordinator.onDuress = { /* duress on a fall alert behaves as a silent immediate escalation */ countdownJob?.cancel(); escalateFall(type, m, level.name, score, silent = true, fake = true) }
        showAlertNotification(str(R.string.alert_notif_title), str(R.string.alert_notif_text))
        launchAlertActivity()
        feedback = AlertFeedback(this).also { it.start(str(R.string.tts_fall), settings.locale()) }
        countdownJob?.cancel()
        countdownJob = scope.launch {
            var left = total
            while (left > 0 && isActive) {
                delay(1000); left--
                coordinator.update { it.copy(remaining = left) }
            }
            if (isActive) escalateFall(type, detector.currentMetrics() ?: m, level.name, score, silent = false, fake = false)
        }
    }

    private fun escalateFall(type: EventType, m: FallMetrics, level: String, score: Int, silent: Boolean, fake: Boolean) {
        lastEscalationAt = System.currentTimeMillis()
        if (silent) feedback?.stop()
        coordinator.update { it.copy(phase = AlertPhase.ESCALATING, remaining = 0, fakeCancelled = fake, silent = silent) }
        scope.launch {
            val r = withContext(Dispatchers.Default) { escalation.run(EscalationRequest(type, detector.currentMetrics() ?: m, level, score, silent)) }
            coordinator.update { it.copy(phase = AlertPhase.ESCALATED, trackUrl = r.session?.url, briefUrl = r.session?.briefUrl) }
            delay(120_000)
            feedback?.stop()   // alarm runs for 2 min after escalation unless cancelled earlier
            delay(180_000)
            detector.reset(); releaseWakeLock()
        }
    }

    private fun cancelFallAlert(source: String) {
        countdownJob?.cancel(); feedback?.stop(); feedback = null
        val wasEscalated = coordinator.state.value.phase == AlertPhase.ESCALATED || coordinator.state.value.phase == AlertPhase.ESCALATING
        if (wasEscalated) escalation.stop(cancelled = true)
        coordinator.update { it.copy(phase = AlertPhase.CANCELLED) }
        detector.reset(); releaseWakeLock()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ID_ALERT)
        scope.launch { delay(2500); coordinator.clear() }
    }

    // ------------------------------------------------------------------ silent SOS
    private fun triggerSos(simulate: Boolean) {
        if (sosActive || System.currentTimeMillis() - lastEscalationAt < 15_000) return
        sosActive = true; lastEscalationAt = System.currentTimeMillis()
        acquireWakeLock(10 * 60_000L)
        notify(ID_MAIN, discreetNotification())
        coordinator.set(AlertUiState(AlertPhase.ESCALATING, EventType.SOS, silent = true))
        coordinator.onCancel = { cancelSos() }
        coordinator.onDuress = { coordinator.update { it.copy(fakeCancelled = true) } }
        scope.launch {
            val r = withContext(Dispatchers.Default) { escalation.run(EscalationRequest(EventType.SOS, silent = true, extraEvent = if (simulate) JSONObject().put("simulated", true) else null)) }
            coordinator.update { it.copy(phase = AlertPhase.ESCALATED, trackUrl = r.session?.url, briefUrl = r.session?.briefUrl) }
            if (settings.current.audioRecording) startRecording(r.session)
        }
    }

    private fun cancelSos() {
        sosActive = false
        recordJob?.cancel()
        escalation.stop(cancelled = true)
        coordinator.clear(); releaseWakeLock()
        notify(ID_MAIN, guardNotification())
    }

    /** Consent-gated audio evidence: 30 s chunks encrypted on-device (AES-256) before any upload. Best effort: Android may block background mic use. */
    @SuppressLint("MissingPermission")
    private fun startRecording(h: SessionHandle?) {
        if (!Perm.has(this, Manifest.permission.RECORD_AUDIO)) return
        recordJob?.cancel()
        recordJob = scope.launch(Dispatchers.IO) {
            var n = 0
            while (isActive && sosActive) {
                val plain = File(cacheDir, "ev_${n}.m4a")
                try {
                    @Suppress("DEPRECATION")
                    val rec = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this@GuardService) else MediaRecorder()
                    rec.setAudioSource(MediaRecorder.AudioSource.MIC); rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC); rec.setOutputFile(plain.absolutePath)
                    rec.prepare(); rec.start(); delay(30_000); rec.stop(); rec.release()
                    val dir = File(filesDir, "evidence").apply { mkdirs() }
                    val enc = File(dir, "ev_${System.currentTimeMillis()}.enc")
                    val mk = MasterKey.Builder(this@GuardService).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
                    val ef = EncryptedFile.Builder(this@GuardService, enc, mk, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
                    ef.openFileOutput().use { out -> plain.inputStream().use { it.copyTo(out) } }
                    plain.delete()
                    if (h != null) try { api.uploadAudio(h, enc.readBytes()) } catch (e: Exception) { }
                } catch (e: Exception) { plain.delete(); return@launch }  // mic blocked in background: stop quietly
                n++
            }
        }
    }

    // ------------------------------------------------------------------ keyword ("help me" / बचाओ / ಸಹಾಯ), battery heavy
    private fun startKeyword() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        keywordOn = true
        scope.launch { listenOnce() }
    }
    private fun stopKeyword() { keywordOn = false; try { recognizer?.destroy() } catch (e: Exception) { }; recognizer = null }
    private suspend fun listenOnce() {
        if (!keywordOn) return
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { r ->
                r.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(b: Bundle?) { check(b); again() }
                    override fun onPartialResults(b: Bundle?) { check(b) }
                    override fun onError(error: Int) { again() }
                    override fun onReadyForSpeech(p: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(v: Float) = Unit
                    override fun onBufferReceived(b: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(t: Int, p: Bundle?) = Unit
                    private fun check(b: Bundle?) {
                        val list = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return
                        if (list.any { t -> KEYWORDS.any { k -> t.lowercase().contains(k) } }) triggerSos(simulate = false)
                    }
                    private fun again() { scope.launch { delay(800); listenOnce() } }
                })
                r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, settings.locale().toLanguageTag()).putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
            }
        } catch (e: Exception) { keywordOn = false }
    }

    // ------------------------------------------------------------------ journey
    private fun startJourney() {
        val req = journeys.pending ?: return
        journeys.pending = null
        journeyReq = req; simDead = false
        journeys.update { JourneyUiState(active = true, destName = req.destName, mode = req.mode, etaMs = req.etaMs, route = req.route, message = null) }
        val prof = profiles.profile.value
        journeyMonitor = JourneyMonitor(req.route, req.dest, req.etaMs, req.bufferMs, prof.safePlaces.map { LatLng(it.lat, it.lng) })
        acquireWakeLock(60_000L); // short: only while setting up
        scope.launch {
            val cfg = settings.current
            val recipients = if (cfg.demoMode) escalation.recipients() else req.followers
            val fix = withContext(Dispatchers.Default) { loc.current(6000) }
            val body = JSONObject().apply {
                put("name", profiles.sharedFields()["name"]?.toString() ?: prof.fullName.ifBlank { "VERA user" })
                put("mode", req.mode); put("ownerId", store.ownerId()); put("demo", cfg.demoMode); put("notify", false)
                put("dest", JSONObject().put("name", req.destName).put("lat", req.dest.lat).put("lng", req.dest.lng))
                put("etaTs", req.etaMs); put("bufferS", req.bufferMs / 1000)
                put("route", JSONArray(req.route.map { JSONArray().put(it.lat).put(it.lng) }))
                put("followers", JSONArray(recipients.map { JSONObject().put("name", it.name).put("phone", it.phone).put("lang", it.lang) }))
                put("watchdogSeconds", if (cfg.demoMode) 20 else cfg.watchdogSeconds)
                put("lang", cfg.language)
                if (fix != null) { put("lat", fix.latitude); put("lng", fix.longitude) }
                Battery.percent(this@GuardService)?.let { put("battery", it) }
                if (req.cabVehicle.isNotBlank() || req.cabDriver.isNotBlank()) put("cab", JSONObject().put("vehicle", req.cabVehicle).put("driver", req.cabDriver))
                put("patient", JSONObject(profiles.sharedFields().mapValues { (_, v) -> if (v is List<*>) JSONArray(v) else v }))
            }
            val h = try { withContext(Dispatchers.IO) { api.createJourney(body) } } catch (e: Exception) {
                journeys.update { it.copy(active = false, error = e.message ?: "error") }; stopJourneyForeground(); return@launch
            }
            journeyHandle = h
            journeys.update { it.copy(trackUrl = h.url) }
            val name = body.getString("name")
            val eta = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(req.etaMs))
            for (c in recipients) sms.send(c.phone, (if (cfg.demoMode) "[DEMO] " else "") + SmsBuilder.journeyStart(c.lang, name, req.destName, eta, h.url))
            releaseWakeLock()
            runJourneyLoop(h, req)
        }
    }

    private fun runJourneyLoop(h: SessionHandle, req: JourneyRequest) {
        journeyJob?.cancel()
        journeyJob = scope.launch {
            val ticker = launch { while (isActive) { delay(15_000); if (!simDead) journeyMonitor?.onTick(System.currentTimeMillis())?.forEach { onJourneyEvent(it) } } }
            try {
                loc.updates(JourneyConfig.LOCATION_INTERVAL_MS).collect { l ->
                    if (simDead) return@collect
                    val p = LatLng(l.latitude, l.longitude)
                    val bat = Battery.percent(this@GuardService)
                    val mon = journeyMonitor ?: return@collect
                    val evs = mon.onLocation(p, System.currentTimeMillis(), bat)
                    journeys.update { it.copy(current = p) }
                    val r = try { withContext(Dispatchers.IO) { api.journeyLocation(h, p.lat, p.lng, bat, mon.stopped, l.accuracy) } } catch (e: Exception) { null }
                    r?.optString("status")?.takeIf { it.isNotBlank() }?.let { st -> journeys.update { it.copy(status = st) } }
                    evs.forEach { onJourneyEvent(it) }
                }
            } finally { ticker.cancel() }
        }
    }

    private fun onJourneyEvent(e: JourneyEvent) {
        val h = journeyHandle ?: return
        when (e) {
            JourneyEvent.ARRIVED -> {
                journeys.update { it.copy(status = "arrived", message = str(R.string.journey_arrived)) }
                scope.launch {
                    val cfg = settings.current
                    val name = profiles.profile.value.fullName.ifBlank { "VERA user" }
                    for (c in (if (cfg.demoMode) escalation.recipients() else journeyReq?.followers ?: emptyList())) sms.send(c.phone, SmsBuilder.arrived(c.lang, name))
                    try { withContext(Dispatchers.IO) { api.journeyEnd(h, true) } } catch (x: Exception) { }
                    endJourneyLocal()
                }
            }
            JourneyEvent.LOW_BATTERY -> journeys.update { it.copy(message = str(R.string.journey_low_battery)) }
            JourneyEvent.DEVIATED, JourneyEvent.STOPPED, JourneyEvent.DELAYED -> {
                val st = when (e) { JourneyEvent.DEVIATED -> "deviated"; JourneyEvent.STOPPED -> "stopped"; else -> "delayed" }
                journeys.update { it.copy(status = st) }
                promptCheckIn(st)
            }
        }
    }

    /** Discreet "Are you okay?" prompt. Unanswered or "I need help" -> shared pipeline as a Journey alert. */
    private fun promptCheckIn(reason: String) {
        if (coordinator.state.value.phase != AlertPhase.NONE) return
        val cfg = settings.current
        val total = cfg.checkInSeconds.coerceIn(10, 120)
        coordinator.set(AlertUiState(AlertPhase.CHECKIN, EventType.JOURNEY_ALERT, remaining = total, total = total, silent = cfg.checkInSilent))
        coordinator.onCancel = { answerCheckIn(ok = true) }
        coordinator.onNeedHelp = { checkInEscalate(reason, fake = false) }
        coordinator.onDuress = { checkInEscalate(reason, fake = true) }
        acquireWakeLock(3 * 60_000L)
        feedback = AlertFeedback(this).also { if (cfg.checkInSilent) it.vibrate(true) else it.start(str(R.string.tts_checkin), settings.locale()) }
        showAlertNotification(str(R.string.checkin_title), str(R.string.checkin_text))
        launchAlertActivity()
        checkInJob?.cancel()
        checkInJob = scope.launch {
            var left = total
            while (left > 0 && isActive) { delay(1000); left--; coordinator.update { it.copy(remaining = left) } }
            if (isActive) checkInEscalate(reason, fake = false)
        }
    }

    private fun answerCheckIn(ok: Boolean) {
        checkInJob?.cancel(); feedback?.stop(); feedback = null
        val h = journeyHandle
        journeyMonitor?.acknowledge(System.currentTimeMillis(), journeys.state.value.current)
        journeys.update { it.copy(status = "on_route") }
        if (h != null) scope.launch { try { withContext(Dispatchers.IO) { api.journeyCheckin(h, ok) } } catch (e: Exception) { } }
        coordinator.clear(); releaseWakeLock()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(ID_ALERT)
    }

    private fun checkInEscalate(reason: String, fake: Boolean) {
        checkInJob?.cancel(); feedback?.stop(); feedback = null
        val h = journeyHandle
        journeys.update { it.copy(status = "alert") }
        coordinator.update { it.copy(phase = AlertPhase.ESCALATING, fakeCancelled = fake, silent = true) }
        lastEscalationAt = System.currentTimeMillis()
        scope.launch {
            if (h != null) try { withContext(Dispatchers.IO) { api.journeyCheckin(h, false) } } catch (e: Exception) { }
            val extra = JSONObject().put("reason", reason).put("journeyDest", journeyReq?.destName ?: "")
            val r = withContext(Dispatchers.Default) { escalation.run(EscalationRequest(EventType.JOURNEY_ALERT, silent = true, journeyToken = h?.token, extraEvent = extra)) }
            coordinator.update { it.copy(phase = AlertPhase.ESCALATED, trackUrl = r.session?.url, briefUrl = r.session?.briefUrl) }
        }
    }

    private fun extendJourney(minutes: Int) {
        val h = journeyHandle ?: return
        journeyMonitor?.extend(minutes * 60_000L)
        journeys.update { it.copy(etaMs = it.etaMs + minutes * 60_000L, status = if (it.status == "delayed") "on_route" else it.status) }
        scope.launch { try { withContext(Dispatchers.IO) { api.journeyExtend(h, minutes) } } catch (e: Exception) { } }
    }

    fun endJourney(arrived: Boolean) {
        val h = journeyHandle
        if (h != null) scope.launch { try { withContext(Dispatchers.IO) { api.journeyEnd(h, arrived) } } catch (e: Exception) { }; endJourneyLocal() } else endJourneyLocal()
    }

    private fun endJourneyLocal() {
        journeyJob?.cancel(); checkInJob?.cancel()
        journeyHandle = null; journeyMonitor = null; journeyReq = null; simDead = false
        val arrived = journeys.state.value.status == "arrived"
        journeys.update { it.copy(active = false, simulatingDead = false, status = if (arrived) "arrived" else "on_route") }
        stopJourneyForeground()
    }

    /** Demo: the phone "dies". We stop all reporting; the server's watchdog must alert followers by itself. */
    private fun simulateDead() {
        if (journeyHandle == null) return
        simDead = true
        journeys.update { it.copy(simulatingDead = true, message = str(R.string.sim_dead_msg)) }
    }

    private fun stopJourneyForeground() { /* the guard service keeps running for fall protection */ }

    // ------------------------------------------------------------------ notifications, wake lock
    private fun launchAlertActivity() {
        try {
            startActivity(Intent(this, AlertActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        } catch (e: Exception) { /* background start blocked: the full-screen intent notification still launches it */ }
    }

    private fun showAlertNotification(title: String, text: String) {
        val pi = PendingIntent.getActivity(this, 11, Intent(this, AlertActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(this, Channels.ALERT)
            .setSmallIcon(R.drawable.ic_notif).setContentTitle(title).setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX).setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pi, true).setContentIntent(pi).setOngoing(true).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).build()
        notify(ID_ALERT, n)
    }

    private fun guardNotification(): Notification {
        val c = lctx()
        val open = PendingIntent.getActivity(this, 1, Intent(this, com.vera.guard.ui.MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val b = NotificationCompat.Builder(this, Channels.GUARD)
            .setSmallIcon(R.drawable.ic_notif).setContentTitle(c.getString(R.string.notif_title)).setContentText(c.getString(R.string.notif_text))
            .setPriority(NotificationCompat.PRIORITY_LOW).setOngoing(true).setContentIntent(open).setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        if (settings.current.notificationSosEnabled) b.addAction(0, c.getString(R.string.notif_sos_action), ServiceStarter.pending(this, Actions.SOS, 2))
        return b.build()
    }

    /** During a silent SOS the notification looks like a generic system service. */
    private fun discreetNotification(): Notification =
        NotificationCompat.Builder(this, Channels.QUIET).setSmallIcon(R.drawable.ic_notif).setContentTitle(str(R.string.notif_discreet_title))
            .setContentText(str(R.string.notif_discreet_text)).setPriority(NotificationCompat.PRIORITY_MIN).setOngoing(true).setVisibility(NotificationCompat.VISIBILITY_SECRET).build()

    private fun notify(id: Int, n: Notification) { try { (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, n) } catch (e: Exception) { } }

    private fun goForeground(n: Notification) {
        try {
            when {
                Build.VERSION.SDK_INT >= 34 -> {
                    var t = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    if (loc.hasPermission()) t = t or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                    startForeground(ID_MAIN, n, t)
                }
                Build.VERSION.SDK_INT >= 29 && loc.hasPermission() -> startForeground(ID_MAIN, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
                else -> startForeground(ID_MAIN, n)
            }
        } catch (e: Exception) {
            try { if (Build.VERSION.SDK_INT >= 34) startForeground(ID_MAIN, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE) else startForeground(ID_MAIN, n) } catch (e2: Exception) { stopSelf() }
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun acquireWakeLock(ms: Long) {
        releaseWakeLock()
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "vera:event").also { it.acquire(ms) }
    }
    private fun releaseWakeLock() { try { if (wakeLock?.isHeld == true) wakeLock?.release() } catch (e: Exception) { }; wakeLock = null }

    override fun onDestroy() {
        sensors.unregisterListener(this)
        feedback?.stop(); stopKeyword(); releaseWakeLock(); scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ID_MAIN = 1001
        const val ID_ALERT = 1002
        private val KEYWORDS = listOf("help me", "बचाओ", "ಸಹಾಯ")
    }
}
