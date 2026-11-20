package com.vera.guard.data

import com.vera.guard.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(private val store: SecureStore) {
    private val _profile = MutableStateFlow(Profile.fromJson(store.getString("profile")))
    private val _contacts = MutableStateFlow(Contact.listFromJson(store.getString("contacts")))
    private val _consent = MutableStateFlow(Consent.fromJson(store.getString("consent")))
    val profile: StateFlow<Profile> = _profile.asStateFlow()
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()
    val consent: StateFlow<Consent> = _consent.asStateFlow()

    fun saveProfile(p: Profile) { store.putString("profile", p.toJson().toString()); _profile.value = p }
    fun saveConsent(c: Consent) { store.putString("consent", c.toJson().toString()); _consent.value = c }

    /** Max 5 contacts; exactly one primary (the first if none chosen). */
    fun saveContact(c: Contact): Boolean {
        val cur = _contacts.value.toMutableList()
        val idx = cur.indexOfFirst { it.id == c.id }
        if (idx < 0 && cur.size >= 5) return false
        if (idx >= 0) cur[idx] = c else cur += c
        persist(normalizePrimary(cur, if (c.primary) c.id else null))
        return true
    }
    fun deleteContact(id: String) = persist(normalizePrimary(_contacts.value.filter { it.id != id }, null))
    fun setPrimary(id: String) = persist(normalizePrimary(_contacts.value, id))
    private fun normalizePrimary(l: List<Contact>, primaryId: String?): List<Contact> {
        if (l.isEmpty()) return l
        val chosen = primaryId ?: l.firstOrNull { it.primary }?.id ?: l.first().id
        return l.map { it.copy(primary = it.id == chosen) }.sortedByDescending { it.primary }
    }
    private fun persist(l: List<Contact>) { store.putString("contacts", Contact.listToJson(l)); _contacts.value = l }
    fun newId() = UUID.randomUUID().toString()

    /** Only the fields the user enabled for sharing (used for alerts, SMS, Responder Brief and chat personalisation). */
    fun sharedFields(p: Profile = _profile.value, c: Consent = _consent.value): Map<String, Any> {
        val m = LinkedHashMap<String, Any>()
        if (c.name && p.fullName.isNotBlank()) m["name"] = p.fullName
        if (c.age) p.age()?.let { m["age"] = it }
        if (c.gender && p.gender.isNotBlank()) m["gender"] = p.gender
        if (c.bloodGroup && p.bloodGroup.isNotBlank()) m["bloodGroup"] = p.bloodGroup
        if (c.allergies && p.allergies.isNotBlank()) m["allergies"] = p.allergies.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (c.conditions && p.conditions.isNotBlank()) m["conditions"] = p.conditions.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (c.medications && p.medications.isNotBlank()) m["medications"] = p.medications.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (c.doctor && p.doctorName.isNotBlank()) m["doctor"] = "${p.doctorName} ${p.doctorPhone}".trim()
        if (c.address && p.homeAddress.isNotBlank()) m["address"] = p.homeAddress
        if (c.note && p.iceNote.isNotBlank()) m["note"] = p.iceNote
        return m
    }

    fun wipe() { saveProfile(Profile()); saveConsent(Consent()); persist(emptyList()) }
}

@Singleton
class SettingsRepository @Inject constructor(private val store: SecureStore) {
    private val _s = MutableStateFlow(AppSettings.fromJson(store.getString("settings")).let { if (it.serverUrl.isBlank()) it.copy(serverUrl = BuildConfig.BACKEND_URL) else it })
    val settings: StateFlow<AppSettings> = _s.asStateFlow()
    val current: AppSettings get() = _s.value
    fun update(f: (AppSettings) -> AppSettings) { val n = f(_s.value); store.putString("settings", n.toJson().toString()); _s.value = n }
    fun locale(): Locale = when (_s.value.language) { "hi" -> Locale("hi", "IN"); "kn" -> Locale("kn", "IN"); else -> Locale("en", "IN") }
    fun serverUrl() = _s.value.serverUrl.trim().trimEnd('/')
}
