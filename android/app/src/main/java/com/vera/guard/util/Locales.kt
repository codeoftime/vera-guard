package com.vera.guard.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object Locales {
    fun forCode(code: String): Locale = when (code) { "hi" -> Locale("hi", "IN"); "kn" -> Locale("kn", "IN"); else -> Locale("en", "IN") }

    /** Context whose resources use the user's chosen app language (activities, services, notifications). */
    fun wrap(base: Context, code: String): Context {
        val loc = forCode(code)
        Locale.setDefault(loc)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(loc)
        return base.createConfigurationContext(cfg)
    }

    fun code(store: com.vera.guard.data.SecureStore): String =
        try { org.json.JSONObject(store.getString("settings") ?: "{}").optString("language", "en") } catch (e: Exception) { "en" }
}
