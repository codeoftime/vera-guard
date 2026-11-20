package com.vera.guard.data

import com.vera.guard.net.ApiClient
import javax.inject.Inject
import javax.inject.Singleton

data class ChatMessage(val fromUser: Boolean, val text: String, val offline: Boolean = false)

/** Calls the backend /chat proxy (the LLM key never ships in the app); falls back to bundled first-aid cards. */
@Singleton
class ChatRepository @Inject constructor(private val api: ApiClient, private val profiles: ProfileRepository, private val settings: SettingsRepository) {
    /** Personalisation (age, blood group, allergies, conditions) is sent only for fields the user consented to share. */
    suspend fun ask(history: List<ChatMessage>, personalise: Boolean): ChatMessage {
        val lang = settings.current.language
        val last = history.lastOrNull { it.fromUser }?.text.orEmpty()
        return try {
            val msgs = history.takeLast(8).map { (if (it.fromUser) "user" else "assistant") to it.text }
            val shared = profiles.sharedFields()
            val prof = if (personalise) shared.filterKeys { it in setOf("age", "bloodGroup", "allergies", "conditions") }.takeIf { it.isNotEmpty() } else null
            ChatMessage(false, api.chat(msgs, lang, prof))
        } catch (e: Exception) {
            ChatMessage(false, FirstAid.offlineReply(last, lang), offline = true)
        }
    }
}
