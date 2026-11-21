package com.vera.guard.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.Period

data class SafePlace(val label: String, val lat: Double, val lng: Double)

data class Profile(
    val fullName: String = "",
    val photoUri: String = "",
    val dob: String = "",            // yyyy-MM-dd
    val gender: String = "",
    val heightCm: Float? = null,
    val weightKg: Float? = null,
    val imperial: Boolean = false,
    val bloodGroup: String = "",
    val allergies: String = "",
    val conditions: String = "",
    val medications: String = "",
    val doctorName: String = "",
    val doctorPhone: String = "",
    val homeAddress: String = "",
    val safePlaces: List<SafePlace> = emptyList(),
    val iceNote: String = "",
) {
    fun age(today: LocalDate = LocalDate.now()): Int? = try {
        if (dob.isBlank()) null else Period.between(LocalDate.parse(dob), today).years.takeIf { it in 0..130 }
    } catch (e: Exception) { null }

    fun toJson() = JSONObject().apply {
        put("fullName", fullName); put("photoUri", photoUri); put("dob", dob); put("gender", gender)
        put("heightCm", heightCm?.toDouble() ?: JSONObject.NULL); put("weightKg", weightKg?.toDouble() ?: JSONObject.NULL)
        put("imperial", imperial); put("bloodGroup", bloodGroup); put("allergies", allergies); put("conditions", conditions)
        put("medications", medications); put("doctorName", doctorName); put("doctorPhone", doctorPhone)
        put("homeAddress", homeAddress); put("iceNote", iceNote)
        put("safePlaces", JSONArray(safePlaces.map { JSONObject().put("label", it.label).put("lat", it.lat).put("lng", it.lng) }))
    }

    companion object {
        fun fromJson(s: String?): Profile {
            if (s.isNullOrBlank()) return Profile()
            return try {
                val o = JSONObject(s)
                val sp = o.optJSONArray("safePlaces") ?: JSONArray()
                Profile(
                    o.optString("fullName"), o.optString("photoUri"), o.optString("dob"), o.optString("gender"),
                    if (o.isNull("heightCm")) null else o.optDouble("heightCm").toFloat(),
                    if (o.isNull("weightKg")) null else o.optDouble("weightKg").toFloat(),
                    o.optBoolean("imperial"), o.optString("bloodGroup"), o.optString("allergies"), o.optString("conditions"),
                    o.optString("medications"), o.optString("doctorName"), o.optString("doctorPhone"), o.optString("homeAddress"),
                    (0 until sp.length()).map { val x = sp.getJSONObject(it); SafePlace(x.optString("label"), x.optDouble("lat"), x.optDouble("lng")) },
                    o.optString("iceNote"),
                )
            } catch (e: Exception) { Profile() }
        }
    }
}

data class Contact(val id: String, val name: String, val phone: String, val relationship: String = "", val lang: String = "en", val primary: Boolean = false) {
    fun toJson() = JSONObject().put("id", id).put("name", name).put("phone", phone).put("relationship", relationship).put("lang", lang).put("primary", primary)
    companion object {
        fun fromJson(o: JSONObject) = Contact(o.optString("id"), o.optString("name"), o.optString("phone"), o.optString("relationship"), o.optString("lang", "en"), o.optBoolean("primary"))
        fun listFromJson(s: String?): List<Contact> = try {
            val a = JSONArray(s ?: "[]"); (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        } catch (e: Exception) { emptyList() }
        fun listToJson(l: List<Contact>) = JSONArray(l.map { it.toJson() }).toString()
    }
}

/** Per-field sharing consent (spec 6.1). Blood group on by default, medications off by default. */
data class Consent(
    val name: Boolean = true, val age: Boolean = true, val gender: Boolean = true, val bloodGroup: Boolean = true,
    val allergies: Boolean = true, val conditions: Boolean = true, val medications: Boolean = false,
    val doctor: Boolean = false, val address: Boolean = false, val note: Boolean = true,
) {
    fun toJson() = JSONObject().put("name", name).put("age", age).put("gender", gender).put("bloodGroup", bloodGroup).put("allergies", allergies)
        .put("conditions", conditions).put("medications", medications).put("doctor", doctor).put("address", address).put("note", note)
    companion object {
        fun fromJson(s: String?): Consent = try {
            val o = JSONObject(s ?: "{}"); val d = Consent()
            Consent(o.optBoolean("name", d.name), o.optBoolean("age", d.age), o.optBoolean("gender", d.gender), o.optBoolean("bloodGroup", d.bloodGroup),
                o.optBoolean("allergies", d.allergies), o.optBoolean("conditions", d.conditions), o.optBoolean("medications", d.medications),
                o.optBoolean("doctor", d.doctor), o.optBoolean("address", d.address), o.optBoolean("note", d.note))
        } catch (e: Exception) { Consent() }
    }
}

data class AppSettings(
    val language: String = "en",
    val theme: String = "dark",           // system | light | dark
    val serverUrl: String = "",
    val emergencyNumber: String = "112",
    val shakeEnabled: Boolean = true,
    val widgetEnabled: Boolean = true,
    val tileEnabled: Boolean = true,
    val notificationSosEnabled: Boolean = true,
    val keywordEnabled: Boolean = false,
    val audioRecording: Boolean = false,
    val photoCapture: Boolean = false,
    val fallEnabled: Boolean = true,
    val sensitivity: Float = 1f,          // 0.8 (less sensitive) .. 1.2 (more sensitive)
    val countdownLow: Int = 30, val countdownMedium: Int = 20, val countdownHigh: Int = 10,
    val checkInSeconds: Int = 30, val checkInSilent: Boolean = false,
    val watchdogSeconds: Int = 300, val bufferMinutes: Int = 10,
    val demoMode: Boolean = false, val demoPhone: String = "",
    val quickExit: Boolean = false,
    val fakeCallName: String = "Mom",
    val appLock: Boolean = false,
) {
    fun toJson() = JSONObject().apply {
        put("language", language); put("theme", theme); put("serverUrl", serverUrl); put("emergencyNumber", emergencyNumber)
        put("shakeEnabled", shakeEnabled); put("widgetEnabled", widgetEnabled); put("tileEnabled", tileEnabled); put("notificationSosEnabled", notificationSosEnabled)
        put("keywordEnabled", keywordEnabled); put("audioRecording", audioRecording); put("photoCapture", photoCapture); put("fallEnabled", fallEnabled)
        put("sensitivity", sensitivity.toDouble()); put("countdownLow", countdownLow); put("countdownMedium", countdownMedium); put("countdownHigh", countdownHigh)
        put("checkInSeconds", checkInSeconds); put("checkInSilent", checkInSilent); put("watchdogSeconds", watchdogSeconds); put("bufferMinutes", bufferMinutes)
        put("demoMode", demoMode); put("demoPhone", demoPhone); put("quickExit", quickExit); put("fakeCallName", fakeCallName); put("appLock", appLock)
    }
    companion object {
        fun fromJson(s: String?): AppSettings = try {
            val o = JSONObject(s ?: "{}"); val d = AppSettings()
            AppSettings(
                o.optString("language", d.language), o.optString("theme", d.theme), o.optString("serverUrl", d.serverUrl), o.optString("emergencyNumber", d.emergencyNumber),
                o.optBoolean("shakeEnabled", d.shakeEnabled), o.optBoolean("widgetEnabled", d.widgetEnabled), o.optBoolean("tileEnabled", d.tileEnabled),
                o.optBoolean("notificationSosEnabled", d.notificationSosEnabled), o.optBoolean("keywordEnabled", d.keywordEnabled), o.optBoolean("audioRecording", d.audioRecording),
                o.optBoolean("photoCapture", d.photoCapture), o.optBoolean("fallEnabled", d.fallEnabled), o.optDouble("sensitivity", d.sensitivity.toDouble()).toFloat(),
                o.optInt("countdownLow", d.countdownLow), o.optInt("countdownMedium", d.countdownMedium), o.optInt("countdownHigh", d.countdownHigh),
                o.optInt("checkInSeconds", d.checkInSeconds), o.optBoolean("checkInSilent", d.checkInSilent), o.optInt("watchdogSeconds", d.watchdogSeconds),
                o.optInt("bufferMinutes", d.bufferMinutes), o.optBoolean("demoMode", d.demoMode), o.optString("demoPhone", d.demoPhone),
                o.optBoolean("quickExit", d.quickExit), o.optString("fakeCallName", d.fakeCallName), o.optBoolean("appLock", d.appLock),
            )
        } catch (e: Exception) { AppSettings() }
    }
}

object Validators {
    private val phone = Regex("^\\+?[0-9]{7,15}$")
    fun phone(s: String) = phone.matches(s.replace(" ", "").replace("-", ""))
    fun normalizePhone(s: String) = s.replace(" ", "").replace("-", "")
    fun name(s: String) = s.trim().length in 2..60
    val bloodGroups = listOf("", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    fun height(cm: Float?) = cm == null || cm in 50f..250f
    fun weight(kg: Float?) = kg == null || kg in 2f..400f
    fun dob(s: String) = s.isBlank() || try { val d = LocalDate.parse(s); !d.isAfter(LocalDate.now()) && d.year > 1900 } catch (e: Exception) { false }
    fun cmToFtIn(cm: Float): Pair<Int, Int> { val totalIn = Math.round(cm / 2.54f); return totalIn / 12 to totalIn % 12 }
    fun ftInToCm(ft: Int, inch: Int) = (ft * 12 + inch) * 2.54f
    fun kgToLb(kg: Float) = kg * 2.2046226f
    fun lbToKg(lb: Float) = lb / 2.2046226f
}
