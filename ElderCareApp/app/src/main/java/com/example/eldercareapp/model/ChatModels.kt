package com.example.eldercareapp.model

data class ChatPolicyRequest(
    val message: String,
    val conversation_id: String? = "",
    val user_id: String? = "demo-user-001",
    val input_type: String? = "text",
    val tts_language: String? = "zh-CN",
)

data class SourceItem(
    val document_name: String? = null,
    val score: Double? = null,
    val content: String? = null,
)

data class ChatPolicyResponse(
    val answer: String,
    val conversation_id: String? = "",
    val original_text: String? = "",
    val search_query: String? = "",
    val display_text: String? = "",
    val tts: TtsInfo? = null,
    val sources: List<SourceItem> = emptyList(),
    val usage: Map<String, Any> = emptyMap(),
)

data class TtsInfo(
    val language: String? = "zh-CN",
    val voice: String? = "longxiaochun_v3",
    val text: String? = "",
    val audio_url: String? = null,
    val cached: Boolean = false,
)

data class TtsSynthesizeRequest(
    val text: String,
    val language: String = "zh-CN",
    val voice: String? = null,
)

data class TtsSynthesizeResponse(
    val text: String = "",
    val language: String? = "zh-CN",
    val voice: String? = "longxiaochun_v3",
    val audio_url: String? = null,
    val cached: Boolean = false,
)

data class VoiceTranscribeResponse(
    val original_text: String,
    val language: String? = "auto",
    val usage: Map<String, Any> = emptyMap(),
    val request_id: String? = "",
)
