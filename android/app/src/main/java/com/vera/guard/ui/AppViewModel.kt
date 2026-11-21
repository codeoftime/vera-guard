package com.vera.guard.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vera.guard.data.AppDatabase
import com.vera.guard.data.ChatMessage
import com.vera.guard.data.ChatRepository
import com.vera.guard.data.EventDao
import com.vera.guard.data.EventLogEntity
import com.vera.guard.data.NearbyDao
import com.vera.guard.data.ProfileRepository
import com.vera.guard.data.SecureStore
import com.vera.guard.data.SettingsRepository
import com.vera.guard.escalation.AlertCoordinator
import com.vera.guard.journey.JourneyCoordinator
import com.vera.guard.net.ApiClient
import com.vera.guard.service.LocationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

data class NearbyState(
    val loading: Boolean = false, val data: JSONObject? = null, val fetchedAt: Long = 0, val offline: Boolean = false,
    val error: Boolean = false, val lat: Double? = null, val lng: Double? = null,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    @ApplicationContext val ctx: Context,
    val profiles: ProfileRepository,
    val settings: SettingsRepository,
    val coordinator: AlertCoordinator,
    val journeys: JourneyCoordinator,
    val api: ApiClient,
    val store: SecureStore,
    private val chatRepo: ChatRepository,
    private val eventDao: EventDao,
    private val nearbyDao: NearbyDao,
    private val db: AppDatabase,
    val loc: LocationHelper,
    val sensors: com.vera.guard.service.SensorHub,
    val siren: com.vera.guard.service.SirenController,
) : ViewModel() {
    private val _events = MutableStateFlow<List<EventLogEntity>>(emptyList())
    val events: StateFlow<List<EventLogEntity>> = _events.asStateFlow()
    fun refreshEvents() { viewModelScope.launch { _events.value = try { eventDao.recent() } catch (e: Exception) { emptyList() } } }

    // ---- nearby ----
    private val _nearby = MutableStateFlow(NearbyState())
    val nearby: StateFlow<NearbyState> = _nearby.asStateFlow()
    fun loadNearby() {
        viewModelScope.launch {
            _nearby.value = _nearby.value.copy(loading = true, error = false)
            val fix = loc.current(7000)
            if (fix != null) {
                val r = try { withContext(Dispatchers.IO) { api.nearby(fix.latitude, fix.longitude, 9000) } } catch (e: Exception) { null }
                if (r != null && r.optBoolean("ok", false)) {
                    nearbyDao.put(com.vera.guard.data.NearbyCacheEntity("last", r.toString(), System.currentTimeMillis()))
                    _nearby.value = NearbyState(false, r, System.currentTimeMillis(), false, false, fix.latitude, fix.longitude)
                    return@launch
                }
            }
            val cached = try { nearbyDao.last() } catch (e: Exception) { null }
            _nearby.value = if (cached != null) NearbyState(false, JSONObject(cached.json), cached.fetchedAt, true, false, fix?.latitude, fix?.longitude)
            else NearbyState(false, null, 0, false, true, fix?.latitude, fix?.longitude)
        }
    }

    // ---- chat ----
    private val _chat = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chat: StateFlow<List<ChatMessage>> = _chat.asStateFlow()
    private val _chatBusy = MutableStateFlow(false)
    val chatBusy: StateFlow<Boolean> = _chatBusy.asStateFlow()
    fun sendChat(text: String, personalise: Boolean) {
        val t = text.trim().take(600)
        if (t.isEmpty() || _chatBusy.value) return
        _chat.value = _chat.value + ChatMessage(true, t)
        _chatBusy.value = true
        viewModelScope.launch {
            val reply = chatRepo.ask(_chat.value, personalise)
            _chat.value = _chat.value + reply
            _chatBusy.value = false
        }
    }

    // ---- data ownership ----
    fun exportData(): String = JSONObject().apply {
        put("profile", profiles.profile.value.toJson()); put("consent", profiles.consent.value.toJson())
        put("contacts", JSONArray(profiles.contacts.value.map { it.toJson() })); put("settings", settings.current.toJson())
        put("events", JSONArray(_events.value.map { JSONObject().put("ts", it.ts).put("type", it.type).put("severity", it.severity ?: JSONObject.NULL).put("score", it.score ?: JSONObject.NULL).put("trackUrl", it.trackUrl ?: JSONObject.NULL) }))
    }.toString(2)

    fun deleteAll(onDone: () -> Unit) {
        viewModelScope.launch {
            val owner = store.ownerId()
            try { withContext(Dispatchers.IO) { api.deleteOwner(owner) } } catch (e: Exception) { /* server may be unreachable; local wipe still happens */ }
            withContext(Dispatchers.IO) { try { db.clearAllTables() } catch (e: Exception) { } }
            profiles.wipe()
            store.setPin("cancel", null); store.setPin("duress", null)
            store.putString("ownerId", null)
            _events.value = emptyList(); _chat.value = emptyList()
            onDone()
        }
    }
}
