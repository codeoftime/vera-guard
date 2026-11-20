package com.vera.guard.escalation

import android.content.Context
import com.vera.guard.data.Contact
import com.vera.guard.data.EventDao
import com.vera.guard.data.EventLogEntity
import com.vera.guard.data.NearbyCacheEntity
import com.vera.guard.data.NearbyDao
import com.vera.guard.data.ProfileRepository
import com.vera.guard.data.SettingsRepository
import com.vera.guard.data.Validators
import com.vera.guard.detect.FallMetrics
import com.vera.guard.net.ApiClient
import com.vera.guard.net.SessionHandle
import com.vera.guard.service.Battery
import com.vera.guard.service.CallHelper
import com.vera.guard.service.LocationHelper
import com.vera.guard.service.SmsSender
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class EscalationRequest(
    val type: EventType,
    val metrics: FallMetrics? = null,
    val severity: String? = null,
    val score: Int? = null,
    val silent: Boolean = false,
    val journeyToken: String? = null,
    val extraEvent: JSONObject? = null,
)

data class EscalationResult(val session: SessionHandle?, val facilities: List<Facility>, val smsSent: Int, val callPlaced: Boolean)

/**
 * Shared escalation pipeline (spec 1.5) used by falls, SOS and journey alerts.
 * 1 location -> 2 nearby (non-blocking, short timeout) -> 3 backend session -> 4 SMS -> 5 call primary -> 6 ambulance -> 7 tracking.
 * Each step is isolated: a failure is recorded and the pipeline continues.
 */
@Singleton
class EscalationManager @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val api: ApiClient,
    private val loc: LocationHelper,
    private val sms: SmsSender,
    private val calls: CallHelper,
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    private val events: EventDao,
    private val nearbyDao: NearbyDao,
    private val coordinator: AlertCoordinator,
    private val store: com.vera.guard.data.SecureStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var trackJob: Job? = null
    private var smsJob: Job? = null
    private val _session = MutableStateFlow<SessionHandle?>(null)
    val session: StateFlow<SessionHandle?> = _session.asStateFlow()

    private fun step(msg: String) = coordinator.update { it.copy(steps = it.steps + msg) }

    /** Contacts that may be alerted. In demo mode ONLY the demo contact is used. */
    fun recipients(): List<Contact> {
        val s = settings.current
        if (s.demoMode) {
            val p = Validators.normalizePhone(s.demoPhone)
            return if (Validators.phone(p)) listOf(Contact("demo", "Demo contact", p, "Demo", settings.current.language, true)) else emptyList()
        }
        return profiles.contacts.value
    }

    suspend fun run(req: EscalationRequest): EscalationResult {
        val s = settings.current
        val demo = s.demoMode
        step("start:${req.type.api}")
        // 1. location
        val fix = loc.current(8000)
        step(if (fix != null) "location:ok" else "location:none")

        // 2. nearby facilities without blocking the flow
        val nearbyDeferred = scope.async {
            if (fix == null) null else withTimeoutOrNull(5000) {
                try { api.nearby(fix.latitude, fix.longitude, 5000) } catch (e: Exception) { null }
            }
        }
        val nearbyJson = withTimeoutOrNull(5500) { nearbyDeferred.await() }
        val facilities = parseFacilities(nearbyJson)
        if (nearbyJson != null && nearbyJson.optBoolean("ok", false)) nearbyDao.put(NearbyCacheEntity("last", nearbyJson.toString(), System.currentTimeMillis()))
        step("nearby:${facilities.size}")

        // 3. backend session
        val shared = profiles.sharedFields()
        val contacts = recipients()
        val battery = Battery.percent(ctx)
        val eventJson = JSONObject().apply {
            put("kind", req.type.api); put("ts", System.currentTimeMillis())
            req.severity?.let { put("severity", it) }; req.score?.let { put("score", it) }
            req.metrics?.let { m ->
                put("peakG", m.peakG.toDouble()); put("freeFallMs", m.freeFallMs); put("orientationDeg", m.orientationDeg.toDouble())
                put("stillnessSec", m.stillnessMs / 1000.0); put("impactMs", m.impactMs); put("jerkGps", m.jerkGps.toDouble()); put("gyroPeak", m.gyroPeak.toDouble())
            }
            req.extraEvent?.let { ex -> ex.keys().forEach { k -> put(k, ex.get(k)) } }
        }
        val body = JSONObject().apply {
            put("kind", req.type.api); put("ownerId", store.ownerId()); put("demo", demo)
            if (fix != null) { put("lat", fix.latitude); put("lng", fix.longitude); put("accuracy", fix.accuracy.toDouble()) }
            battery?.let { put("battery", it) }
            put("patient", JSONObject(shared.mapValues { (_, v) -> if (v is List<*>) JSONArray(v) else v }))
            put("contacts", JSONArray(contacts.map { JSONObject().put("name", it.name).put("phone", it.phone).put("relationship", it.relationship) }))
            put("event", eventJson)
            put("facilities", nearbyJson ?: JSONObject.NULL)
            req.journeyToken?.let { put("journeyToken", it) }
        }
        val handle = try { api.createSession(body) } catch (e: Exception) { null }
        _session.value = handle
        step(if (handle != null) "session:ok" else "session:failed")
        coordinator.update { it.copy(trackUrl = handle?.url, briefUrl = handle?.briefUrl) }

        // 4. SMS every contact in their own language
        var smsCount = 0
        val timeText = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        val name = shared["name"]?.toString() ?: profiles.profile.value.fullName.ifBlank { "VERA user" }
        for (c in contacts) {
            val text = if (handle != null) SmsBuilder.alert(c.lang, name, timeText, req.type, req.severity, req.score, handle.url, handle.briefUrl, battery, facilities, demo)
            else SmsBuilder.shortAlert(c.lang, req.type, req.severity, req.score, fix?.latitude, fix?.longitude, shared["bloodGroup"]?.toString(), facilities.firstOrNull()?.name)
            if (sms.send(c.phone, text)) smsCount++
        }
        step("sms:$smsCount/${contacts.size}")

        // 5. call
        var called = false
        val first = contacts.firstOrNull()
        if (first != null) { called = calls.place(first.phone); step(if (called) "call:${first.name}" else "call:failed") }
        if (req.silent && contacts.size > 1) {
            scope.launch {
                delay(30_000)
                if (calls.isIdle()) { calls.place(contacts[1].phone); step("call2:${contacts[1].name}") }
            }
        }

        // 6. ambulance: never in demo mode; never for silent triggers (a call screen would reveal the phone). The alert screen performs the dial.
        val amb = s.emergencyNumber.ifBlank { "112" }
        if (!demo && !req.silent) {
            coordinator.update { it.copy(ambulanceNumber = amb, ambulanceNeedsManualCall = false) }
            step("ambulance:$amb")
        }

        events.insert(EventLogEntity(ts = System.currentTimeMillis(), type = req.type.api, severity = req.severity, score = req.score, details = eventJson.toString(), trackUrl = handle?.url))

        // 7. tracking + (silent) SMS fallback
        if (handle != null) startTracking(handle, req)
        if (req.silent) startSmsFallback(contacts, handle, name, req)
        return EscalationResult(handle, facilities, smsCount, called)
    }

    /** Hospitals first (nearest first), then clinics, then pharmacies. */
    private fun parseFacilities(j: JSONObject?): List<Facility> {
        if (j == null) return emptyList()
        val out = ArrayList<Facility>()
        for (key in listOf("hospitals", "clinics", "pharmacies")) {
            val a = j.optJSONArray(key) ?: continue
            val group = ArrayList<Facility>()
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                group += Facility(o.optString("name"), o.optInt("distanceM"), o.optString("phone").ifBlank { null }, o.optDouble("lat"), o.optDouble("lng"))
            }
            out += group.sortedBy { it.distanceM }
        }
        return out
    }

    /** Push location every 12 s for at least 10 minutes, until a responder acknowledges or the user cancels. */
    private fun startTracking(h: SessionHandle, req: EscalationRequest) {
        trackJob?.cancel()
        trackJob = scope.launch {
            val start = System.currentTimeMillis()
            while (isActive && System.currentTimeMillis() - start < MAX_TRACK_MS) {
                delay(TRACK_INTERVAL_MS)
                val f = loc.current(6000) ?: continue
                val acked = try { api.pushLocation(h, f.latitude, f.longitude, f.accuracy, Battery.percent(ctx)) } catch (e: Exception) { false }
                if (acked) coordinator.update { it.copy(acked = true) }
                if (acked && System.currentTimeMillis() - start >= MIN_TRACK_MS) break
            }
        }
    }

    private fun startSmsFallback(contacts: List<Contact>, h: SessionHandle?, name: String, req: EscalationRequest) {
        smsJob?.cancel()
        smsJob = scope.launch {
            while (isActive) {
                delay(120_000)
                val f = loc.current(5000)
                for (c in contacts) {
                    val text = SmsBuilder.shortAlert(c.lang, req.type, null, null, f?.latitude, f?.longitude, null, null) + (h?.let { " " + it.url } ?: "")
                    sms.send(c.phone, text)
                }
            }
        }
    }

    /** User cancelled (or confirmed safe): stop tracking and mark the session. */
    fun stop(cancelled: Boolean) {
        trackJob?.cancel(); smsJob?.cancel()
        val h = _session.value
        if (h != null) scope.launch { try { api.endSession(h, cancelled) } catch (e: Exception) { } }
        _session.value = null
    }

    fun isTracking() = trackJob?.isActive == true

    companion object {
        const val TRACK_INTERVAL_MS = 12_000L
        const val MIN_TRACK_MS = 10 * 60_000L
        const val MAX_TRACK_MS = 6 * 3600_000L
    }
}
