package com.vera.guard.net

import com.vera.guard.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

class ApiException(val code: Int, message: String) : Exception(message)

data class SessionHandle(val token: String, val key: String, val url: String, val briefUrl: String)

/** Small HttpURLConnection client for our backend. All calls are suspend + IO-bound with short timeouts. */
@Singleton
class ApiClient @Inject constructor(private val settings: SettingsRepository) {
    private fun base() = settings.serverUrl()

    suspend fun call(method: String, path: String, body: JSONObject? = null, key: String? = null, timeoutMs: Int = 8000, raw: ByteArray? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val conn = (URL(base() + path).openConnection() as HttpURLConnection)
            try {
                conn.requestMethod = method
                conn.connectTimeout = timeoutMs; conn.readTimeout = timeoutMs
                conn.setRequestProperty("Accept", "application/json")
                if (key != null) conn.setRequestProperty("x-key", key)
                val payload = raw ?: body?.toString()?.toByteArray()
                if (payload != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", if (raw != null) "application/octet-stream" else "application/json")
                    conn.outputStream.use { it.write(payload) }
                }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.use { s -> ByteArrayOutputStream().also { o -> s.copyTo(o) }.toString("UTF-8") } ?: ""
                if (code !in 200..299) throw ApiException(code, text.take(200))
                if (text.isBlank()) JSONObject() else if (text.trimStart().startsWith("[")) JSONObject().put("items", JSONArray(text)) else JSONObject(text)
            } finally { conn.disconnect() }
        }

    suspend fun createSession(body: JSONObject): SessionHandle {
        val r = call("POST", "/sessions", body)
        return SessionHandle(r.getString("token"), r.getString("key"), r.getString("url"), r.getString("briefUrl"))
    }
    /** Returns true when a responder has acknowledged. */
    suspend fun pushLocation(s: SessionHandle, lat: Double, lng: Double, acc: Float?, battery: Int?): Boolean {
        val r = call("POST", "/sessions/${s.token}/location", JSONObject().put("lat", lat).put("lng", lng).put("accuracy", acc?.toDouble() ?: JSONObject.NULL).put("battery", battery ?: JSONObject.NULL), s.key)
        return r.optBoolean("acked", false)
    }
    suspend fun updateSession(s: SessionHandle, body: JSONObject) { call("POST", "/sessions/${s.token}/event", body, s.key) }
    suspend fun endSession(s: SessionHandle, cancelled: Boolean) { call("POST", "/sessions/${s.token}/end", JSONObject().put("cancelled", cancelled), s.key) }
    suspend fun uploadAudio(s: SessionHandle, bytes: ByteArray) { call("POST", "/sessions/${s.token}/audio", key = s.key, raw = bytes, timeoutMs = 30000) }

    suspend fun nearby(lat: Double, lng: Double, timeoutMs: Int = 6000): JSONObject = call("GET", "/nearby?lat=$lat&lng=$lng&radius=5000", timeoutMs = timeoutMs)
    suspend fun geocode(q: String): JSONArray = call("GET", "/geocode?q=" + java.net.URLEncoder.encode(q, "UTF-8"), timeoutMs = 10000).getJSONArray("items")
    suspend fun route(from: Pair<Double, Double>, to: Pair<Double, Double>, mode: String): JSONObject =
        call("GET", "/route?from=${from.first},${from.second}&to=${to.first},${to.second}&mode=$mode", timeoutMs = 12000)

    suspend fun createJourney(body: JSONObject): SessionHandle {
        val r = call("POST", "/journeys", body)
        return SessionHandle(r.getString("token"), r.getString("key"), r.getString("url"), "")
    }
    suspend fun journeyLocation(j: SessionHandle, lat: Double, lng: Double, battery: Int?, stopped: Boolean, accuracy: Float?): JSONObject =
        call("POST", "/journeys/${j.token}/location", JSONObject().put("lat", lat).put("lng", lng).put("battery", battery ?: JSONObject.NULL).put("stopped", stopped).put("accuracy", accuracy?.toDouble() ?: JSONObject.NULL), j.key)
    suspend fun journeyCheckin(j: SessionHandle, ok: Boolean) { call("POST", "/journeys/${j.token}/checkin", JSONObject().put("status", if (ok) "ok" else "help"), j.key) }
    suspend fun journeyExtend(j: SessionHandle, minutes: Int) { call("POST", "/journeys/${j.token}/extend", JSONObject().put("minutes", minutes), j.key) }
    suspend fun journeyEnd(j: SessionHandle, arrived: Boolean) { call("POST", "/journeys/${j.token}/end", JSONObject().put("arrived", arrived), j.key) }

    suspend fun chat(messages: List<Pair<String, String>>, lang: String, profile: Map<String, Any>?): String {
        val arr = JSONArray(messages.takeLast(8).map { JSONObject().put("role", it.first).put("content", it.second) })
        val body = JSONObject().put("messages", arr).put("lang", lang)
        if (profile != null) body.put("profile", JSONObject(profile.mapValues { it.value.toString() }))
        return call("POST", "/chat", body, timeoutMs = 25000).getString("reply")
    }
    suspend fun deleteOwner(ownerId: String) { call("DELETE", "/owner/$ownerId") }
}
