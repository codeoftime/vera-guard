package com.vera.guard.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import com.vera.guard.journey.Geo
import com.vera.guard.journey.JourneyRequest
import com.vera.guard.journey.LatLng
import com.vera.guard.service.Actions
import com.vera.guard.service.ServiceStarter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

private data class Hit(val name: String, val lat: Double, val lng: Double)

@Composable
fun JourneyScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val js by vm.journeys.state.collectAsStateWithLifecycle()
    val s by vm.settings.settings.collectAsStateWithLifecycle()
    val contacts by vm.profiles.contacts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var query by remember { mutableStateOf("") }
    val hits = remember { mutableStateListOf<Hit>() }
    var dest by remember { mutableStateOf<Hit?>(null) }
    var mode by remember { mutableStateOf("cab") }
    var buffer by remember { mutableStateOf(s.bufferMinutes.toString()) }
    var vehicle by remember { mutableStateOf("") }
    var driver by remember { mutableStateOf("") }
    val chosen = remember { mutableStateListOf<String>() }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<Int?>(null) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tab_journey), style = MaterialTheme.typography.headlineSmall)
        if (s.demoMode) Text(stringResource(R.string.demo_banner), color = VeraAmber, fontWeight = FontWeight.Bold)

        if (js.active) {
            val color = when (js.status) { "on_route", "arrived" -> VeraGreen; "alert" -> VeraRed; else -> VeraAmber }
            SectionCard {
                Text(js.destName, style = MaterialTheme.typography.titleLarge)
                Text(statusLabel(js.status), color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.eta_at, DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(js.etaMs))))
                js.message?.let { Text(it, color = VeraAmber) }
                js.trackUrl?.let { link ->
                    Text(link, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    OutlinedButton(onClick = { ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link), null)) }) { Text(stringResource(R.string.share_link)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ServiceStarter.start(ctx, Actions.JOURNEY_EXTEND) }) { Text(stringResource(R.string.extend_eta)) }
                    Button(onClick = { ServiceStarter.start(ctx, Actions.JOURNEY_END) }) { Text(stringResource(R.string.end_journey)) }
                }
            }
            if (s.demoMode) SectionCard(stringResource(R.string.demo_tools)) {
                Button(onClick = { ServiceStarter.start(ctx, Actions.SIM_DEVIATION) }) { Text(stringResource(R.string.sim_deviation)) }
                Button(onClick = { ServiceStarter.start(ctx, Actions.SIM_DEAD) }) { Text(stringResource(R.string.sim_dead)) }
                Text(stringResource(R.string.sim_dead_hint), style = MaterialTheme.typography.bodySmall)
            }
            return@Column
        }
        if (js.status == "arrived") Text(stringResource(R.string.journey_arrived), color = VeraGreen, fontWeight = FontWeight.Bold)
        js.error?.let { Text(stringResource(R.string.journey_error), color = VeraRed) }

        SectionCard(stringResource(R.string.destination)) {
            Field(stringResource(R.string.search_place), query, { query = it })
            Button(enabled = query.length >= 2, onClick = {
                scope.launch {
                    val r = try { withContext(Dispatchers.IO) { vm.api.geocode(query) } } catch (e: Exception) { null }
                    hits.clear()
                    if (r == null) msg = R.string.search_failed else for (i in 0 until r.length()) { val o = r.getJSONObject(i); hits += Hit(o.optString("name"), o.optDouble("lat"), o.optDouble("lng")) }
                }
            }) { Text(stringResource(R.string.search)) }
            hits.forEach { h -> OutlinedButton(onClick = { dest = h; hits.clear() }, modifier = Modifier.fillMaxWidth()) { Text(h.name, maxLines = 2) } }
            dest?.let { Text("📍 " + it.name, fontWeight = FontWeight.SemiBold) }
        }
        SectionCard(stringResource(R.string.mode)) {
            ChipRow(listOf("walking" to stringResource(R.string.mode_walking), "cab" to stringResource(R.string.mode_cab), "bike" to stringResource(R.string.mode_bike), "car" to stringResource(R.string.mode_car)), mode) { mode = it }
            Field(stringResource(R.string.buffer_minutes), buffer, { buffer = it.filter { c -> c.isDigit() }.take(3) }, keyboard = androidx.compose.ui.text.input.KeyboardType.Number)
        }
        SectionCard(stringResource(R.string.followers)) {
            if (contacts.isEmpty()) Text(stringResource(R.string.no_contacts), style = MaterialTheme.typography.bodySmall)
            contacts.forEach { c ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = c.id in chosen, onCheckedChange = { if (it) chosen += c.id else chosen.remove(c.id) })
                    Text("${c.name} · ${c.phone}")
                }
            }
        }
        if (mode == "cab") SectionCard(stringResource(R.string.cab_details)) {
            Field(stringResource(R.string.vehicle_number), vehicle, { vehicle = it.take(20) })
            Field(stringResource(R.string.driver_name), driver, { driver = it.take(60) })
        }
        msg?.let { Text(stringResource(it), color = VeraRed) }
        Button(
            enabled = dest != null && !busy, modifier = Modifier.fillMaxWidth(),
            onClick = {
                val d = dest ?: return@Button
                val followers = contacts.filter { it.id in chosen }
                if (followers.isEmpty() && !s.demoMode) { msg = R.string.pick_follower; return@Button }
                busy = true; msg = null
                scope.launch {
                    val fix = vm.loc.current(7000)
                    if (fix == null) { msg = R.string.no_location; busy = false; return@launch }
                    val from = LatLng(fix.latitude, fix.longitude); val to = LatLng(d.lat, d.lng)
                    var route = listOf(from, to)
                    var durationS = Geo.haversine(from, to) / (if (mode == "walking") 1.4 else 7.0)
                    try {
                        val r = withContext(Dispatchers.IO) { vm.api.route(from.lat to from.lng, to.lat to to.lng, mode) }
                        val a = r.getJSONArray("route")
                        route = (0 until a.length()).map { i -> a.getJSONArray(i).let { LatLng(it.getDouble(0), it.getDouble(1)) } }
                        durationS = r.optDouble("durationS", durationS)
                    } catch (e: Exception) { /* routing unavailable: straight line + estimated speed */ }
                    val bufMs = (buffer.toLongOrNull() ?: 0L) * 60_000L
                    vm.journeys.pending = JourneyRequest(d.name.take(120), to, mode, route, System.currentTimeMillis() + (durationS * 1000).toLong() + bufMs, bufMs, followers, vehicle, driver)
                    vm.journeys.update { it.copy(error = null) }
                    ServiceStarter.start(ctx, Actions.JOURNEY_START)
                    busy = false
                }
            },
        ) { Text(stringResource(if (busy) R.string.loading else R.string.start_journey)) }
    }
}

@Composable
private fun statusLabel(s: String) = stringResource(when (s) {
    "on_route" -> R.string.st_on_route; "deviated" -> R.string.st_deviated; "stopped" -> R.string.st_stopped
    "delayed" -> R.string.st_delayed; "arrived" -> R.string.st_arrived; else -> R.string.st_alert
})
