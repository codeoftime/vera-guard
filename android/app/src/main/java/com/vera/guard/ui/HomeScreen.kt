package com.vera.guard.ui

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import com.vera.guard.escalation.AlertPhase
import com.vera.guard.escalation.EventType
import com.vera.guard.escalation.PinResult
import com.vera.guard.service.Actions
import com.vera.guard.service.Perm
import com.vera.guard.service.ServiceStarter
import java.text.DateFormat
import java.util.Date

private class Step(val title: Int, val why: Int, val granted: (Context) -> Boolean, val request: () -> Unit)

@Composable
fun HomeScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val st by vm.coordinator.state.collectAsStateWithLifecycle()
    val s by vm.settings.settings.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }
    var sliderScore by remember { mutableFloatStateOf(70f) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) { tick++; vm.refreshEvents() } }
        lifecycle.addObserver(obs); onDispose { lifecycle.removeObserver(obs) }
    }
    val multi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++; try { ServiceStarter.start(ctx, Actions.START) } catch (e: Exception) { } }
    val single = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++; try { ServiceStarter.start(ctx, Actions.START) } catch (e: Exception) { } }
    val act = ctx as Activity
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
    val active = states[0] && states[1] && states[3]
    val critical = listOf(0, 1, 3)

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (active) "✅" else "⚠️", fontSize = 28.sp)
                Column(Modifier.padding(start = 10.dp)) {
                    Text(stringResource(if (active) R.string.protection_active else R.string.protection_incomplete), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (active) VeraGreen else VeraAmber)
                    Text(stringResource(R.string.protection_hint), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (s.demoMode) SectionCard { Text(stringResource(R.string.demo_banner), color = VeraAmber, fontWeight = FontWeight.Bold); Text(stringResource(R.string.demo_hint), style = MaterialTheme.typography.bodySmall) }

        // Active silent SOS: stop it with the real PIN (the duress PIN fakes a cancel)
        if (st.type == EventType.SOS && st.phase != AlertPhase.NONE) {
            SectionCard(stringResource(R.string.sos_active)) {
                if (st.fakeCancelled) Text(stringResource(R.string.sos_cancelled), fontWeight = FontWeight.Bold)
                else {
                    st.trackUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    var pin by remember { mutableStateOf("") }
                    var wrong by remember { mutableStateOf(false) }
                    if (vm.coordinator.hasAnyPin()) {
                        Field(stringResource(R.string.pin_label), pin, { v -> pin = v.filter { it.isDigit() }.take(8); wrong = false }, error = if (wrong) stringResource(R.string.pin_wrong) else null, keyboard = androidx.compose.ui.text.input.KeyboardType.NumberPassword)
                        Button(onClick = { if (vm.coordinator.submitPin(pin) == PinResult.WRONG) wrong = true }) { Text(stringResource(R.string.stop_sos)) }
                    } else Button(onClick = { vm.coordinator.cancelByUser("tap") }) { Text(stringResource(R.string.stop_sos)) }
                }
            }
        }

        Button(
            onClick = { try { ServiceStarter.start(ctx, if (s.demoMode) Actions.SIM_SOS else Actions.SOS) } catch (e: Exception) { } },
            modifier = Modifier.fillMaxWidth().height(110.dp), colors = ButtonDefaults.buttonColors(containerColor = VeraRed),
        ) { Text(stringResource(R.string.sos_button), fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color.White) }
        Text(stringResource(R.string.sos_hint), style = MaterialTheme.typography.bodySmall)

        SectionCard(stringResource(R.string.setup_checklist)) {
            steps.forEachIndexed { i, step ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text((if (states[i]) "✅ " else "⬜ ") + stringResource(step.title) + if (i in critical) "" else "", fontWeight = FontWeight.SemiBold)
                        Text(stringResource(step.why), style = MaterialTheme.typography.bodySmall)
                    }
                    if (!states[i]) OutlinedButton(onClick = { step.request() }) { Text(stringResource(R.string.grant)) }
                }
            }
        }

        SectionCard(stringResource(R.string.tools)) {
            var delaySec by remember { mutableIntStateOf(5) }
            Text(stringResource(R.string.fake_call), fontWeight = FontWeight.SemiBold)
            ChipRow(listOf(0 to "0s", 5 to "5s", 15 to "15s", 30 to "30s"), delaySec) { delaySec = it }
            Button(onClick = {
                val name = s.fakeCallName
                val i = Intent(ctx, FakeCallActivity::class.java).putExtra("name", name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ try { ctx.startActivity(i) } catch (e: Exception) { } }, delaySec * 1000L)
            }) { Text(stringResource(R.string.start_fake_call)) }
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
            if (events.isEmpty()) Text(stringResource(R.string.no_events), style = MaterialTheme.typography.bodySmall)
            events.take(8).forEach { e ->
                Text("${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(e.ts))} · ${e.type.uppercase()}" + (if (e.severity != null) " · ${e.severity} ${e.score}/100" else ""), style = MaterialTheme.typography.bodyMedium)
                e.trackUrl?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
            }
        }
        Text(stringResource(R.string.disclaimer_emergency), style = MaterialTheme.typography.bodySmall)
    }
}

