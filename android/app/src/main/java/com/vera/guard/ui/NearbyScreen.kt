package com.vera.guard.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vera.guard.R
import org.json.JSONArray
import org.json.JSONObject
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private data class Place(val name: String, val type: String, val lat: Double, val lng: Double, val distanceM: Int, val phone: String?, val address: String?, val emergency: Boolean, val openNow: Boolean?)

private fun parse(j: JSONObject?): List<Place> {
    if (j == null) return emptyList()
    val out = ArrayList<Place>()
    fun add(a: JSONArray?, type: String) {
        if (a == null) return
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            out += Place(o.optString("name"), type, o.optDouble("lat"), o.optDouble("lng"), o.optInt("distanceM"), o.optString("phone").ifBlank { null }, o.optString("address").ifBlank { null }, o.optBoolean("emergency"), if (o.isNull("openNow")) null else o.optBoolean("openNow"))
        }
    }
    add(j.optJSONArray("hospitals"), "hospital"); add(j.optJSONArray("clinics"), "clinic"); add(j.optJSONArray("pharmacies"), "pharmacy")
    return out
}

/** "Nearby help" tab: map + list + filters, usable any time; shows the cached result offline with its age. */
@Composable
fun NearbyScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val st by vm.nearby.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf("all") }
    LaunchedEffect(Unit) { vm.loadNearby() }
    val all = remember(st.data) { parse(st.data) }
    val shown = all.filter { filter == "all" || it.type == filter }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.tab_nearby), style = MaterialTheme.typography.headlineSmall)
        ChipRow(listOf("all" to stringResource(R.string.filter_all), "hospital" to stringResource(R.string.hospitals), "clinic" to stringResource(R.string.clinics), "pharmacy" to stringResource(R.string.pharmacies)), filter) { filter = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.loadNearby() }, enabled = !st.loading) { Text(stringResource(if (st.loading) R.string.loading else R.string.refresh)) }
        }
        if (st.offline) Text(stringResource(R.string.offline_cached, fmtAge(System.currentTimeMillis() - st.fetchedAt)), color = VeraAmber)
        if (st.error) Text(stringResource(R.string.nearby_error), color = VeraRed)

        AndroidView(
            modifier = Modifier.fillMaxWidth().height(260.dp),
            factory = { c -> MapView(c).apply { setTileSource(TileSourceFactory.MAPNIK); setMultiTouchControls(true); controller.setZoom(14.0) } },
            update = { map ->
                map.overlays.clear()
                val c = st.lat?.let { la -> st.lng?.let { lo -> GeoPoint(la, lo) } }
                if (c != null) { map.controller.setCenter(c); map.overlays.add(Marker(map).apply { position = c; title = ctx.getString(R.string.you_are_here); setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }) }
                shown.take(40).forEach { p -> map.overlays.add(Marker(map).apply { position = GeoPoint(p.lat, p.lng); title = p.name; snippet = SmsLikeDistance.format(p.distanceM) }) }
                map.invalidate()
            },
        )

        if (shown.isEmpty() && !st.loading) Text(stringResource(R.string.nothing_found), style = MaterialTheme.typography.bodySmall)
        shown.forEach { p ->
            SectionCard {
                Text(p.name, style = MaterialTheme.typography.titleMedium)
                Text(listOfNotNull(stringResource(when (p.type) { "hospital" -> R.string.hospitals; "clinic" -> R.string.clinics; else -> R.string.pharmacies }), SmsLikeDistance.format(p.distanceM), if (p.emergency) "24x7/ER" else null, p.openNow?.let { if (it) stringResource(R.string.open_now) else stringResource(R.string.closed) }).joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                p.address?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (p.phone != null) OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + p.phone.filter { it.isDigit() || it == '+' }))) }) { Text(stringResource(R.string.call)) }
                    OutlinedButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}(${Uri.encode(p.name)})"))) }) { Text(stringResource(R.string.directions)) }
                }
            }
        }
    }
}

private object SmsLikeDistance { fun format(m: Int) = com.vera.guard.escalation.SmsBuilder.formatDistance(m) }
