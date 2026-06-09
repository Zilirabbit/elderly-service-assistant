package com.example.eldercareapp.data.local

import android.content.Context
import com.example.eldercareapp.model.ChatHistoryItem
import com.example.eldercareapp.model.ChatHistoryMessage
import com.example.eldercareapp.model.QaAnswerUiModel
import com.example.eldercareapp.model.QaScenarioOption
import org.json.JSONArray
import org.json.JSONObject

class ChatHistoryStorage(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE
    )

    fun load(): List<ChatHistoryItem> {
        val raw = preferences.getString(HistoryKey, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                array.optJSONObject(index)?.toHistoryItem()
            }.filterNotNull()
                .sortedByDescending { it.updatedAtMillis }
                .take(MaxHistoryItems)
        }.getOrDefault(emptyList())
    }

    fun save(items: List<ChatHistoryItem>) {
        val normalized = items
            .sortedByDescending { it.updatedAtMillis }
            .take(MaxHistoryItems)
        val array = JSONArray()
        normalized.forEach { item -> array.put(item.toJson()) }
        preferences.edit().putString(HistoryKey, array.toString()).apply()
    }

    private fun ChatHistoryItem.toJson(): JSONObject {
        return JSONObject()
            .put("id", id)
            .put("question", question)
            .put("answerPreview", answerPreview)
            .put("messages", messages.toMessagesJson())
            .put("conversationId", conversationId)
            .put("inputType", inputType)
            .put("displayLanguage", displayLanguage)
            .put("speechLanguage", speechLanguage)
            .put("createdAtMillis", createdAtMillis)
            .put("updatedAtMillis", updatedAtMillis)
    }

    private fun JSONObject.toHistoryItem(): ChatHistoryItem {
        return ChatHistoryItem(
            id = optString("id"),
            question = optString("question"),
            answerPreview = optString("answerPreview"),
            messages = optJSONArray("messages").toHistoryMessages(),
            conversationId = optString("conversationId"),
            inputType = optString("inputType", "text"),
            displayLanguage = optString("displayLanguage", "zh-CN"),
            speechLanguage = optString("speechLanguage", "zh-CN"),
            createdAtMillis = optLong("createdAtMillis", 0L),
            updatedAtMillis = optLong("updatedAtMillis", 0L),
        )
    }

    private fun List<ChatHistoryMessage>.toMessagesJson(): JSONArray {
        val array = JSONArray()
        forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("role", message.role)
                    .put("text", message.text)
                    .put("answerUiModel", message.answerUiModel?.toJson() ?: JSONObject.NULL)
                    .put("ttsText", message.ttsText)
                    .put("ttsAudioUrl", message.ttsAudioUrl ?: JSONObject.NULL)
            )
        }
        return array
    }

    private fun JSONArray?.toHistoryMessages(): List<ChatHistoryMessage> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            optJSONObject(index)?.let { item ->
                ChatHistoryMessage(
                    id = item.optLong("id", 0L),
                    role = item.optString("role", "user"),
                    text = item.optString("text"),
                    answerUiModel = item.optJSONObject("answerUiModel")?.toQaAnswerUiModel(),
                    ttsText = item.optString("ttsText"),
                    ttsAudioUrl = item.optNullableString("ttsAudioUrl"),
                )
            }
        }.filterNotNull()
    }

    private fun QaAnswerUiModel.toJson(): JSONObject {
        return JSONObject()
            .put("title", title)
            .put("subtitle", subtitle)
            .put("conclusion", conclusion)
            .put("scenarioOptions", scenarioOptions.toScenarioOptionsJson())
            .put("steps", steps.toStringArrayJson())
            .put("requiredMaterials", requiredMaterials.toStringArrayJson())
            .put("optionalMaterials", optionalMaterials.toStringArrayJson())
            .put("warnings", warnings.toStringArrayJson())
            .put("sourceTitle", sourceTitle ?: JSONObject.NULL)
            .put("originalAnswer", originalAnswer ?: JSONObject.NULL)
            .put("confidence", confidence ?: JSONObject.NULL)
            .put("needHumanReminder", needHumanReminder)
    }

    private fun JSONObject.toQaAnswerUiModel(): QaAnswerUiModel {
        return QaAnswerUiModel(
            title = optString("title"),
            subtitle = optString("subtitle"),
            conclusion = optString("conclusion"),
            scenarioOptions = optJSONArray("scenarioOptions").toScenarioOptions(),
            steps = optJSONArray("steps").toStringList(),
            requiredMaterials = optJSONArray("requiredMaterials").toStringList(),
            optionalMaterials = optJSONArray("optionalMaterials").toStringList(),
            warnings = optJSONArray("warnings").toStringList(),
            sourceTitle = optNullableString("sourceTitle"),
            originalAnswer = optNullableString("originalAnswer"),
            confidence = optNullableString("confidence"),
            needHumanReminder = optBoolean("needHumanReminder", true),
        )
    }

    private fun List<QaScenarioOption>.toScenarioOptionsJson(): JSONArray {
        val array = JSONArray()
        forEach { option ->
            array.put(
                JSONObject()
                    .put("label", option.label)
                    .put("standardQuestion", option.standardQuestion)
            )
        }
        return array
    }

    private fun JSONArray?.toScenarioOptions(): List<QaScenarioOption> {
        if (this == null) return emptyList()
        return List(length()) { index ->
            optJSONObject(index)?.let { item ->
                QaScenarioOption(
                    label = item.optString("label"),
                    standardQuestion = item.optString("standardQuestion"),
                )
            }
        }.filterNotNull()
    }

    private fun List<String>.toStringArrayJson(): JSONArray {
        val array = JSONArray()
        forEach { value -> array.put(value) }
        return array
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return List(length()) { index -> optString(index) }
            .filter { it.isNotBlank() }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        if (!has(name) || isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() }
    }

    companion object {
        const val MaxHistoryItems = 30
        private const val PreferencesName = "eldercare_chat_history"
        private const val HistoryKey = "chat_history_items_v1"
    }
}
