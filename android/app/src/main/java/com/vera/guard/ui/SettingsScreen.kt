package com.vera.guard.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R

@Composable
fun SettingsSection(vm: AppViewModel) {
    val ctx = LocalContext.current
    val s by vm.settings.settings.collectAsStateWithLifecycle()
    var server by remember(s.serverUrl) { mutableStateOf(s.serverUrl) }
    var emergency by remember(s.emergencyNumber) { mutableStateOf(s.emergencyNumber) }
    var demoPhone by remember(s.demoPhone) { mutableStateOf(s.demoPhone) }
    var fakeName by remember(s.fakeCallName) { mutableStateOf(s.fakeCallName) }
    fun upd(f: (com.vera.guard.data.AppSettings) -> com.vera.guard.data.AppSettings) = vm.settings.update(f)
    fun num(t: String, lo: Int, hi: Int, d: Int) = (t.toIntOrNull() ?: d).coerceIn(lo, hi)

    SectionCard(stringResource(R.string.set_general)) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.labelMedium)
        ChipRow(listOf("en" to "English", "hi" to "हिन्दी", "kn" to "ಕನ್ನಡ"), s.language) { code ->
            upd { it.copy(language = code) }
            (ctx as? Activity)?.recreate()
        }
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.labelMedium)
        ChipRow(listOf("system" to stringResource(R.string.theme_system), "light" to stringResource(R.string.theme_light), "dark" to stringResource(R.string.theme_dark)), s.theme) { v -> upd { it.copy(theme = v) } }
        Field(stringResource(R.string.server_url), server, { server = it.take(200) }, keyboard = KeyboardType.Uri)
        Button(onClick = { upd { it.copy(serverUrl = server.trim().trimEnd('/')) } }) { Text(stringResource(R.string.save)) }
        Field(stringResource(R.string.emergency_number), emergency, { emergency = it.filter { c -> c.isDigit() }.take(5) })
        Button(enabled = emergency.length in 3..5, onClick = { upd { it.copy(emergencyNumber = emergency) } }) { Text(stringResource(R.string.save)) }
        Text(stringResource(R.string.emergency_hint), style = MaterialTheme.typography.bodySmall)
    }

    SectionCard(stringResource(R.string.set_detection)) {
        SwitchRow(stringResource(R.string.set_fall), s.fallEnabled, { v -> upd { it.copy(fallEnabled = v) } })
        Text(stringResource(R.string.set_sensitivity) + ": %.1f".format(s.sensitivity), style = MaterialTheme.typography.labelMedium)
        Slider(value = s.sensitivity, onValueChange = { v -> upd { it.copy(sensitivity = (Math.round(v * 10) / 10f)) } }, valueRange = 0.8f..1.2f, steps = 3)
        Text(stringResource(R.string.sensitivity_hint), style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Field(stringResource(R.string.cd_low), s.countdownLow.toString(), { t -> upd { it.copy(countdownLow = num(t, 5, 60, 30)) } }, keyboard = KeyboardType.Number, modifier = androidx.compose.ui.Modifier.weight(1f))
            Field(stringResource(R.string.cd_medium), s.countdownMedium.toString(), { t -> upd { it.copy(countdownMedium = num(t, 5, 60, 20)) } }, keyboard = KeyboardType.Number, modifier = androidx.compose.ui.Modifier.weight(1f))
            Field(stringResource(R.string.cd_high), s.countdownHigh.toString(), { t -> upd { it.copy(countdownHigh = num(t, 5, 60, 10)) } }, keyboard = KeyboardType.Number, modifier = androidx.compose.ui.Modifier.weight(1f))
        }
        Field(stringResource(R.string.checkin_seconds), s.checkInSeconds.toString(), { t -> upd { it.copy(checkInSeconds = num(t, 10, 120, 30)) } }, keyboard = KeyboardType.Number)
        SwitchRow(stringResource(R.string.checkin_silent), s.checkInSilent, { v -> upd { it.copy(checkInSilent = v) } }, hint = stringResource(R.string.checkin_silent_hint))
    }

    SectionCard(stringResource(R.string.set_silent)) {
        SwitchRow(stringResource(R.string.set_shake), s.shakeEnabled, { v -> upd { it.copy(shakeEnabled = v) } }, hint = stringResource(R.string.set_shake_hint))
        SwitchRow(stringResource(R.string.set_widget), s.widgetEnabled, { v -> upd { it.copy(widgetEnabled = v) } })
        SwitchRow(stringResource(R.string.set_tile), s.tileEnabled, { v -> upd { it.copy(tileEnabled = v) } })
        SwitchRow(stringResource(R.string.set_notif_sos), s.notificationSosEnabled, { v -> upd { it.copy(notificationSosEnabled = v) } })
        SwitchRow(stringResource(R.string.set_keyword), s.keywordEnabled, { v -> upd { it.copy(keywordEnabled = v) } }, hint = stringResource(R.string.set_keyword_hint))
        SwitchRow(stringResource(R.string.set_audio), s.audioRecording, { v -> upd { it.copy(audioRecording = v) } }, hint = stringResource(R.string.set_audio_hint))
        SwitchRow(stringResource(R.string.quick_exit_setting), s.quickExit, { v -> upd { it.copy(quickExit = v) } })
        Field(stringResource(R.string.fake_call_name), fakeName, { fakeName = it.take(30) })
        OutlinedButton(onClick = { upd { it.copy(fakeCallName = fakeName.trim().ifEmpty { "Mom" }) } }) { Text(stringResource(R.string.save)) }
    }

    SectionCard(stringResource(R.string.set_journey)) {
        Field(stringResource(R.string.watchdog_seconds), s.watchdogSeconds.toString(), { t -> upd { it.copy(watchdogSeconds = num(t, 60, 1800, 300)) } }, keyboard = KeyboardType.Number)
        Text(stringResource(R.string.watchdog_hint), style = MaterialTheme.typography.bodySmall)
        Field(stringResource(R.string.buffer_minutes), s.bufferMinutes.toString(), { t -> upd { it.copy(bufferMinutes = num(t, 0, 120, 10)) } }, keyboard = KeyboardType.Number)
    }

    SectionCard(stringResource(R.string.set_demo)) {
        SwitchRow(stringResource(R.string.demo_mode), s.demoMode, { v -> upd { it.copy(demoMode = v) } }, hint = stringResource(R.string.demo_hint))
        Field(stringResource(R.string.demo_phone), demoPhone, { demoPhone = it.take(20) }, keyboard = KeyboardType.Phone)
        OutlinedButton(onClick = { upd { it.copy(demoPhone = com.vera.guard.data.Validators.normalizePhone(demoPhone)) } }) { Text(stringResource(R.string.save)) }
    }

    SectionCard(stringResource(R.string.set_reliability)) {
        Text(stringResource(R.string.battery_hint), style = MaterialTheme.typography.bodySmall)
        val pm = ctx.getSystemService(PowerManager::class.java)
        val ignoring = pm?.isIgnoringBatteryOptimizations(ctx.packageName) == true
        Text(stringResource(if (ignoring) R.string.battery_ok else R.string.battery_not_ok), color = if (ignoring) VeraGreen else VeraAmber)
        OutlinedButton(onClick = {
            try { ctx.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + ctx.packageName))) }
            catch (e: Exception) { try { ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (e2: Exception) { } }
        }) { Text(stringResource(R.string.battery_open)) }
        OutlinedButton(onClick = { try { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + ctx.packageName))) } catch (e: Exception) { } }) { Text(stringResource(R.string.app_settings)) }
        Text(stringResource(R.string.battery_oem), style = MaterialTheme.typography.bodySmall)
    }
}
