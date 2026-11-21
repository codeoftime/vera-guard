package com.vera.guard.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import com.vera.guard.escalation.AlertPhase
import com.vera.guard.escalation.EventType
import com.vera.guard.escalation.PinResult
import com.vera.guard.service.Actions
import com.vera.guard.service.Perm
import com.vera.guard.service.ServiceStarter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

private class Step(val title: Int, val why: Int, val granted: (Context) -> Boolean, val request: () -> Unit)

private class Helpline(val number: String, val label: Int)

@Composable
fun HomeScreen(vm: AppViewModel, onNavigate: (Int) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by vm.coordinator.state.collectAsStateWithLifecycle()
    val s by vm.settings.settings.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    val sensor by vm.sensors.status.collectAsStateWithLifecycle()
    val sirenOn by vm.siren.on.collectAsStateWithLifecycle()
    val contacts by vm.profiles.contacts.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }
    var sliderScore by remember { mutableFloatStateOf(70f) }
    var showHelplines by remember { mutableStateOf(false) }
    var showFakeCall by remember { mutableStateOf(false) }
    var showChecklist by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(android.os.SystemClock.elapsedRealtime()) }
    val history = remember { mutableStateListOf<Float>() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { tick++; vm.refreshEvents() } }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(Unit) { while (true) { nowMs = android.os.SystemClock.elapsedRealtime(); delay(1000) } }
    LaunchedEffect(sensor.lastSampleMs) { if (sensor.lastSampleMs > 0) { history.add(sensor.g); while (history.size > 80) history.removeAt(0) } }
    val live = sensor.running && sensor.hasAccel && nowMs - sensor.lastSampleMs < 3000
    val multi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++; try { ServiceStarter.start(ctx, Actions.START) } catch (e: Exception) { } }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++; try { ServiceStarter.start(ctx, Actions.START) } catch (e: Exception) { } }
    fun openSettings(intent: Intent) { try { ctx.startActivity(intent) } catch (e: Exception) { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))) } }

    val steps = listOf(
        Step(R.string.perm_notifications, R.string.why_notifications, { c -> Build.VERSION.SDK_INT < 33 || Perm.has(c, Manifest.permission.POST_NOTIFICATIONS) }) { if (Build.VERSION.SDK_INT >= 33) single.launch(Manifest.permission.POST_NOTIFICATIONS) },
        Step(R.string.perm_location, R.string.why_location, { c -> Perm.has(c, Manifest.permission.ACCESS_FINE_LOCATION) }) { multi.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) },
        Step(R.string.perm_background_location, R.string.why_background_location, { c -> Build.VERSION.SDK_INT < 29 || Perm.has(c, Manifest.permission.ACCESS_BACKGROUND_LOCATION) }) { if (Build.VERSION.SDK_INT >= 29) single.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) },
        Step(R.string.perm_sms, R.string.why_sms, { c -> Perm.has(c, Manifest.permission.SEND_SMS) }) { single.launch(Manifest.permission.SEND_SMS) },
        Step(R.string.perm_phone, R.string.why_phone, { c -> Perm.has(c, Manifest.permission.CALL_PHONE) }) { multi.launch(arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE)) },
        Step(R.string.perm_microphone, R.string.why_microphone, { c -> Perm.has(c, Manifest.permission.RECORD_AUDIO) }) { single.launch(Manifest.permission.RECORD_AUDIO) },
        Step(R.string.perm_battery, R.string.why_battery, { c -> (c.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(c.packageName) }) {
            openSettings(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${ctx.packageName}"))) },
        Step(R.string.perm_fullscreen, R.string.why_fullscreen, { c -> Build.VERSION.SDK_INT < 34 || (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent() }) {
            if (Build.VERSION.SDK_INT >= 34) openSettings(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}"))) },
    )
    val states = remember(tick) { steps.map { it.granted(ctx) } }
    // Critical = notifications, location, SMS. The rest improve reliability.
    val hasRecipient = if (s.demoMode) s.demoPhone.isNotBlank() else contacts.isNotEmpty()
    val active = states[0] && states[1] && states[3] && states[4] && hasRecipient
    val ready = states.count { it }
    val critical = listOf(0, 1, 3, 4)

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ---- hero
        Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(BrandBrush).padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), color = Color.White, style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.hero_tagline), color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                }
                StatusPill(stringResource(if (active) R.string.pill_protected else R.string.pill_setup), if (active) VeraGreen else VeraAmber, pulsing = active)
            }
        }
        if (!hasRecipient) SectionCard {
            Text(stringResource(R.string.no_contact_title), color = VeraRed, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.no_contact_body), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { onNavigate(4) }) { Text(stringResource(R.string.no_contact_btn)) }
        }
        if (s.demoMode) SectionCard { Text(stringResource(R.string.demo_banner), color = VeraAmber, fontWeight = FontWeight.Bold); Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall) }

        // Active silent SOS: stop it with the real PIN (the duress PIN fakes a cancel)
        if (st.type == EventType.SOS && st.phase != AlertPhase.NONE) {
            SectionCard(stringResource(R.string.sos_active)) {
                if (st.fakeCancelled) Text(stringResource(R.string.sos_cancelled), fontWeight = FontWeight.Bold)
                else {
                    st.trackUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    st.steps.forEach { code -> stepText(code)?.let { t -> Text(t, style = MaterialTheme.typography.bodyMedium) } }
                    var pin by remember { mutableStateOf("") }
                    var wrong by remember { mutableStateOf(false) }
                    if (vm.coordinator.hasAnyPin()) {
                        Field(stringResource(R.string.pin_label), pin, { v -> pin = v.filter { it.isDigit() }.take(8); wrong = false }, error = if (wrong) stringResource(R.string.pin_wrong) else null, keyboard = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                        Button(onClick = { if (vm.coordinator.submitPin(pin) == PinResult.WRONG) wrong = true }) { Text(stringResource(R.string.stop_sos)) }
                    } else Button(onClick = { vm.coordinator.cancelByUser("tap") }) { Text(stringResource(R.string.stop_sos)) }
                }
            }
        }

        // ---- SOS
        SosButton(stringResource(R.string.sos_button), stringResource(R.string.sos_hold), onActivated = {
            try { ServiceStarter.start(ctx, if (s.demoMode) Actions.SIM_SOS else Actions.SOS) } catch (e: Exception) { }
        })
        Text(stringResource(R.string.sos_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

        // ---- live motion sensors
        SectionCard(stringResource(R.string.sensor_title)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusPill(stringResource(if (live) R.string.sensor_live else R.string.sensor_off), if (live) VeraGreen else VeraAmber, pulsing = live)
                if (!live) OutlinedButton(onClick = { try { ServiceStarter.start(ctx, Actions.START) } catch (e: Exception) { } }) { Text(stringResource(R.string.sensor_start)) }
            }
            if (!sensor.hasAccel) Text(stringResource(R.string.sensor_none), color = VeraRed, fontWeight = FontWeight.SemiBold)
            else {
                SensorMonitor(
                    sensor, live, history, stringResource(R.string.sensor_force), stringResource(R.string.sensor_rotation), stringResource(R.string.sensor_state),
                    stringResource(when (sensor.detector) {
                        "FREE_FALL" -> R.string.det_freefall; "IMPACT" -> R.string.det_impact; "STILLNESS_CHECK" -> R.string.det_still
                        "CONFIRMING" -> R.string.det_confirm; "ALERTING" -> R.string.det_alert; else -> R.string.det_idle
                    }),
                )
                Text(stringResource(R.string.sensor_shake), style = MaterialTheme.typography.labelMedium)
                androidx.compose.material3.LinearProgressIndicator(progress = { sensor.shake }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = if (sensor.shake >= 1f) VeraRed else VeraTeal)
                if (nowMs - sensor.lastTriggerMs < 8000 && sensor.lastTriggerMs > 0) Text(stringResource(R.string.shake_sent), color = VeraRed, fontWeight = FontWeight.Bold)
                if (!sensor.hasGyro) Text(stringResource(R.string.sensor_nogyro), style = MaterialTheme.typography.bodySmall, color = VeraAmber)
                Text(stringResource(R.string.sensor_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---- women's safety toolkit
        SectionCard(stringResource(R.string.ws_title)) {
            Text(stringResource(R.string.ws_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusPill(stringResource(if (s.shakeEnabled) R.string.ws_shake_on else R.string.ws_shake_off), if (s.shakeEnabled) VeraTeal else VeraAmber)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(Icons.Filled.Call, stringResource(R.string.fake_call), VeraIndigo, Modifier.weight(1f)) { showFakeCall = true }
                ActionTile(Icons.Filled.Notifications, stringResource(if (sirenOn) R.string.ws_siren_stop else R.string.ws_siren), VeraRed, Modifier.weight(1f), active = sirenOn) { vm.siren.toggle() }
                ActionTile(Icons.Filled.Share, stringResource(R.string.ws_share), VeraTeal, Modifier.weight(1f)) {
                    scope.launch {
                        val fix = vm.loc.current(7000)
                        if (fix == null) Toast.makeText(ctx, ctx.getString(R.string.no_location), Toast.LENGTH_LONG).show()
                        else {
                            val link = "https://maps.google.com/?q=${fix.latitude},${fix.longitude}"
                            ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, ctx.getString(R.string.ws_share_msg, link)), null))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionTile(NavigationIcon, stringResource(R.string.ws_safe_walk), VeraViolet, Modifier.weight(1f)) { onNavigate(2) }
                ActionTile(Icons.Filled.LocationOn, stringResource(R.string.ws_nearby), VeraAmber, Modifier.weight(1f)) { onNavigate(1) }
                ActionTile(Icons.Filled.Phone, stringResource(R.string.ws_helplines), VeraGreen, Modifier.weight(1f)) { showHelplines = true }
            }
        }

        // ---- setup checklist
        SectionCard(stringResource(R.string.setup_checklist)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.checklist_progress, ready, steps.size), color = if (ready == steps.size) VeraGreen else VeraAmber, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showChecklist = !showChecklist }) { Text(stringResource(if (showChecklist) R.string.hide else R.string.show)) }
            }
            if (showChecklist || ready < steps.size) steps.forEachIndexed { i, step ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text((if (states[i]) "✅ " else "⬜ ") + stringResource(step.title) + if (i in critical) " *" else "", fontWeight = FontWeight.SemiBold)
                        Text(stringResource(step.why), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!states[i]) OutlinedButton(onClick = { step.request() }) { Text(stringResource(R.string.grant)) }
                }
            }
        }

        if (s.demoMode) {
            SectionCard(stringResource(R.string.demo_tools)) {
                Text(stringResource(R.string.sim_severity, sliderScore.toInt()))
                Slider(value = sliderScore, onValueChange = { sliderScore = it }, valueRange = 0f..100f)
                Button(onClick = { try { ServiceStarter.start(ctx, Actions.SIM_FALL) { putExtra(Actions.EXTRA_SCORE, sliderScore.toInt()) } } catch (e: Exception) { } }) { Text(stringResource(R.string.sim_fall)) }
                Button(onClick = { try { ServiceStarter.start(ctx, Actions.SIM_SOS) } catch (e: Exception) { } }) { Text(stringResource(R.string.sim_sos)) }
                Text(stringResource(R.string.sim_journey_hint), style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard(stringResource(R.string.event_log)) {
            if (events.isEmpty()) Text(stringResource(R.string.no_events), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            events.take(8).forEach { e ->
                Text("${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(e.ts))} · ${e.type.uppercase()}" + (if (e.severity != null) " · ${e.severity} ${e.score}/100" else ""), style = MaterialTheme.typography.bodyMedium)
                e.trackUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
        Text(stringResource(R.string.disclaimer_emergency), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showFakeCall) {
        var delaySec by remember { mutableIntStateOf(5) }
        AlertDialog(
            onDismissRequest = { showFakeCall = false },
            title = { Text(stringResource(R.string.fake_call)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.fc_ring_in), style = MaterialTheme.typography.labelMedium)
                    ChipRow(listOf(0 to "0 s", 5 to "5 s", 15 to "15 s", 30 to "30 s"), delaySec) { delaySec = it }
                    Text(stringResource(R.string.fc_hint, s.fakeCallName), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFakeCall = false
                    val i = Intent(ctx, FakeCallActivity::class.java).putExtra("name", s.fakeCallName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ try { ctx.startActivity(i) } catch (e: Exception) { } }, delaySec * 1000L)
                }) { Text(stringResource(R.string.start_fake_call)) }
            },
            dismissButton = { TextButton(onClick = { showFakeCall = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (showHelplines) {
        val lines = listOf(
            Helpline("112", R.string.hl_112), Helpline("181", R.string.hl_181), Helpline("1091", R.string.hl_1091), Helpline("100", R.string.hl_100),
            Helpline("108", R.string.hl_108), Helpline("1098", R.string.hl_1098), Helpline("1930", R.string.hl_1930),
        )
        AlertDialog(
            onDismissRequest = { showHelplines = false },
            title = { Text(stringResource(R.string.ws_helplines)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lines.forEach { h ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(h.number, style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(h.label), style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = { try { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + h.number))) } catch (e: Exception) { } }) { Text(stringResource(R.string.call)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showHelplines = false }) { Text(stringResource(R.string.close)) } },
        )
    }
}

@Composable
private fun stepText(code: String): String? {
    val arg = code.substringAfter(':', "")
    return when (code.substringBefore(':')) {
        "location" -> stringResource(if (arg == "ok") R.string.step_loc_ok else R.string.step_loc_none)
        "contacts" -> stringResource(R.string.step_contacts_none)
        "sms" -> stringResource(R.string.step_sms, arg)
        "sms2" -> stringResource(R.string.step_sms2, arg)
        "call" -> stringResource(if (arg == "failed") R.string.step_call_failed else R.string.step_call_ok, arg)
        "session" -> stringResource(if (arg == "ok") R.string.step_session_ok else R.string.step_session_failed)
        else -> null
    }
}
