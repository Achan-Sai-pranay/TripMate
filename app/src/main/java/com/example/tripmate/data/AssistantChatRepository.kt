package com.example.tripmate.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tripmate.model.ChatMessage
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.chatDataStore by preferencesDataStore(name = "assistant_chat_store")

class AssistantChatRepository(private val context: Context) {

    private val chatKey = stringPreferencesKey("chat_messages_history")

    suspend fun loadMessages(): List<ChatMessage> {
        val raw = context.chatDataStore.data.first()[chatKey] ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                ChatMessage(
                    text = obj.getString("text"),
                    isFromUser = obj.getBoolean("isFromUser")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun saveMessages(messages: List<ChatMessage>) {
        context.chatDataStore.edit { prefs ->
            val array = JSONArray()
            // Keep the last 50 messages to avoid unbounded store growth
            messages.takeLast(50).forEach { msg ->
                array.put(
                    JSONObject().apply {
                        put("text", msg.text)
                        put("isFromUser", msg.isFromUser)
                    }
                )
            }
            prefs[chatKey] = array.toString()
        }
    }

    suspend fun clearMessages() {
        context.chatDataStore.edit { prefs ->
            prefs.remove(chatKey)
        }
    }
}
